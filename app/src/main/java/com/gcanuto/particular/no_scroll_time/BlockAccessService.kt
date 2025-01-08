package com.gcanuto.particular.no_scroll_time

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.recyclerview.widget.RecyclerView


class BlockAccessService : AccessibilityService() {

    private val handler = Handler()
    private val delayMillis: Long = 3000
    private val youtubeHandler = YoutubeAccessHandler()
    private val instagramHandler = InstagramAccessHandler(this)

    // Para limitar scroll
    private val scrollLimit = 500 // Limite de rolagem
    private var totalScroll = 0
    private var totalScrollY = 0
    private var scrollLimitThreads = 50200;
    private var limiteThreds = false;
    var previousY: Float = 0f  // Posição anterior de Y


    // Tempo de bloqueio
    private val resetIntervalMillis: Long = 5 * 60 * 1000 // 5 minutos

    // Armazena o tempo de último reset de rolagem
    private var lastScrollResetTime: Long = 0

    private val periodicTask = object : Runnable {
        override fun run() {

            try {
                val rootNode = rootInActiveWindow
                if (rootNode != null && isBlockingEnabled()) {
                    val currentPackageName = rootNode.packageName?.toString()

                    if (currentPackageName?.contains("com.google.android.youtube") == true) {
                        youtubeHandler.logNodeHierarchyYoutube(rootNode)
                        youtubeHandler.handleViewClickYoube(rootNode)
                        youtubeHandler.handleWindowChangeYoutube(rootNode)
                    } else if (currentPackageName?.contains("com.instagram.android") == true) {
                        instagramHandler.logNodeHierarchyInstagram(rootNode)
                        instagramHandler.handleViewClickInstagram(rootNode)
                        instagramHandler.handleWindowChangeInstagram(rootNode)
                    }
                } else {
                    Log.e("BlockAccessService", "Root node is null")
                }
            } catch (e: Exception) {
                Log.e("BlockAccessService", "Erro ao processar a tarefa periódica", e)
            }
            handler.postDelayed(this, delayMillis)
        }
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_SCROLLED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d("BlockAccessService", "Serviço de Acessibilidade conectado")

        handler.post(periodicTask)
    }

    private fun isBlockingEnabled(): Boolean {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("block_enabled", false)
    }

    private fun isScrollLimitEnabled(): Boolean {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("scroll_limit_enabled", false)
    }

    // Adicione a lógica para detectar se o pacote é uma das redes sociais
    private fun isTargetApp(packageName: String?): Boolean {
        return packageName?.contains("com.twitter.android") == true || // Twitter (X)
                packageName?.contains("com.instagram.android") == true || // Instagram
                packageName?.contains("com.linkedin.android") == true || // LinkedIn
                packageName?.contains("com.threads") == true ||
                packageName?.contains("com.instagram.barcelona") == true ||
                packageName?.contains("com.facebook.katana") == true // Facebook
    }

    // Armazena o tempo do último bloqueio
    private fun setLastBlockTime(time: Long) {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putLong("last_block_time", time)
            apply()
        }
    }

    /**
     * Retorna o limite de rolagem personalizado com base no pacote do aplicativo.
     */
    private fun getScrollLimitForApp(packageName: String?): Int {
        return when {
            packageName?.contains("com.instagram.android") == true -> 300 // Instagram
            packageName?.contains("com.twitter.android") == true -> 250 // Twitter (X)
            packageName?.contains("com.instagram.barcelona") == true -> Integer.MAX_VALUE // Threads
            packageName?.contains("com.threads") == true -> Integer.MAX_VALUE // Threads
            else -> 500 // Limite padrão para outros apps
        }
    }

    // Recupera o tempo do último bloqueio
    private fun getLastBlockTime(): Long {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getLong("last_block_time", 0)
    }

    // Resetar o contador de rolagem a cada 5 minutos
    private fun resetScrollCountIfNeeded() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastScrollResetTime >= resetIntervalMillis) {
            totalScroll = 0
            totalScrollY = 0
            lastScrollResetTime = currentTime
            limiteThreds = false
            Log.d("BlockAccessService", "Contador de rolagem resetado após 5 minutos.")
        }
    }


    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            val packageName = event.packageName?.toString()
          //  Log.d("BlockAccessService", "Pacote: $packageName")
            //verifica se é um dos pacotes

            val rootNode = rootInActiveWindow

            // Verifica se o evento é de mudança de estado da janela
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                Log.d("BlockAccessService", "Estado da janela mudou. Pacote: $packageName")
                resetScrollCountIfNeeded() // Reseta o contador de rolagem se necessário
                // Verifica se o aplicativo é um dos que queremos monitorar
                if (isTargetApp(packageName)) {
                    // Verifica se o bloqueio de 10 minutos está ativo
                    if (totalScroll > scrollLimit || totalScrollY > scrollLimitThreads || limiteThreds) {
                        this.performGlobalAction(GLOBAL_ACTION_BACK)
                        this.performGlobalAction(GLOBAL_ACTION_BACK)
//                        // Navegar para a tela inicial
//                        val intent = Intent(Intent.ACTION_MAIN)
//                        intent.addCategory(Intent.CATEGORY_HOME)
//                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                        startActivity(intent)

                        // Salva o tempo do bloqueio
                        setLastBlockTime(System.currentTimeMillis())

                        Log.d(
                            "BlockAccessService",
                            "App identificado e bloqueado, indo para a tela inicial."
                        )
                    } else {
                        Log.d("BlockAccessService", "O app está liberado")
                    }
                }
            }

            // Verifica se o evento é de rolagem, apenas para contagem do scroll
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                //Log.d("ScrollMonitor", "Rolagem detectada ")
                resetScrollCountIfNeeded() // Reseta o contador de rolagem se necessário

                if (isScrollLimitEnabled() && isTargetApp(packageName)) {
                    val fromIndex = event.fromIndex
                    val toIndex = event.toIndex
                    val scrollY = Math.abs(event.scrollY) // Fallback caso índices sejam inválidos

                    val scrollLimit = getScrollLimitForApp(packageName)

                    if (fromIndex >= 0 && toIndex >= 0) {
                        if(packageName?.contains("com.instagram.android") == true
                            && !isPostInstagram(rootNode)
                            ){
                            totalScroll += 0 // Calcula a rolagem com índices
                        }else {
                            totalScroll += (toIndex - fromIndex) // Calcula a rolagem com índices
                        }
                        Log.d("ScrollMonitor", "Movimento vertical detectado Total: $totalScroll")
                    }
                     else if (scrollY <= 0) {
                        totalScrollY = scrollY // Usa scrollY como fallback
                        Log.d("ScrollMonitor", "Rolagem detectada com scrollY: $scrollY")
                    } else {
                        Log.d("ScrollMonitor", "Índices inválidos e scrollY não disponível")
                        totalScroll = 0
                    }

                   // Log.d("ScrollMonitor", "Rolagem detectada, Total rolado: $totalScroll")

                    if (totalScroll > scrollLimit || totalScrollY > scrollLimitThreads || limiteThreds) {
                        Log.d("ScrollMonitor", "Limite de rolagem alcançado. Fechando o app.")
                        Log.d("ScrollMonitor", "totalScroll > scrollLimit  ${totalScroll > scrollLimit}")
                        Log.d("ScrollMonitor", "totalScrollY > scrollLimitThreads ${totalScrollY > scrollLimitThreads} ")
                        Log.d("ScrollMonitor", " limiteThreds ${limiteThreds}")
                        // Navegar para a tela inicial
                        if(scrollY != -1){
                            this.performGlobalAction(GLOBAL_ACTION_BACK)
                            this.performGlobalAction(GLOBAL_ACTION_BACK)
                            limiteThreds = true;
                            Handler().postDelayed({
//                                val intent = Intent(Intent.ACTION_MAIN)
//                                intent.addCategory(Intent.CATEGORY_HOME)
//                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                                startActivity(intent)
                            }, 2000) // Atraso de 1 segundo
                        }else {
                            this.performGlobalAction(GLOBAL_ACTION_BACK)
                            this.performGlobalAction(GLOBAL_ACTION_BACK)
//                            val intent = Intent(Intent.ACTION_MAIN)
//                            intent.addCategory(Intent.CATEGORY_HOME)
//                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
//                            startActivity(intent)
                        }

                    }
                }
            }

            // Verificação dos pacotes bloqueados para o YouTube e Instagram

            if (isBlockingEnabled()) {
                if (packageName?.contains("com.google.android.youtube") == true) {
                    youtubeHandler.onAccessibilityYoutube(event, rootNode)
                } else if (packageName?.contains("com.instagram.android") == true) {
                    instagramHandler.onAccessibilityInstagram(event, rootNode)
                }
            }

        }
    }


    private fun isPostInstagram(node: AccessibilityNodeInfo): Boolean {
        val className = node.className ?: "Unknown"
        val text = node.text ?: "No text"
        val contentDescription = node.contentDescription ?: "No content description"

        // Verificando se estamos na tela de Reels
        if ((className.contains("ViewGroup", ignoreCase = true) &&
                    contentDescription.contains("Send post", ignoreCase = true))
        ) {
            Log.d(
                "BlockReelsAccessService",
                "Elemento 'Send post'  encontrado: Class: $className, Text: $text, ContentDescription: $contentDescription"
            )
            return true
        }

        // Verificação recursiva nos filhos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (isPostInstagram(child)) {
                return true
            }
        }

        return false
    }

    override fun onInterrupt() {
        Log.d("BlockAccessService", "Serviço de Acessibilidade interrompido")
    }
}
