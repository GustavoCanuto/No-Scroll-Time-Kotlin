package com.gcanuto.particular.no_scroll_time


import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class ControleRolagemPostHandler(private val accessibilityService: AccessibilityService) {

    private val scrollLimit = 500 // Limite de rolagem
    private val scrollLimitThreads = 40200
    private var totalScroll = 0
    private var totalScrollY = 0
    private var limiteThreds = false

    // Tempo de reset
    private val resetIntervalMillis: Long = 5 * 60 * 1000 // 5 minutos
    private var lastScrollResetTime: Long = 0

    /**
     * Reseta o contador de rolagem, se necessário, com base no tempo decorrido.
     */
    private fun resetScrollCountIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastScrollResetTime >= resetIntervalMillis) {
            totalScroll = 0
            totalScrollY = 0
            lastScrollResetTime = currentTime
            limiteThreds = false
            Log.d("ControleRolagem", "Contador de rolagem resetado após 5 minutos.")
        }
    }

    /**
     * Verifica se o limite de rolagem está ativado.
     */
    private fun isScrollLimitEnabled(): Boolean {
        val sharedPref = accessibilityService.getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("scroll_limit_enabled", false)
    }

    // Armazena o tempo do último bloqueio
    private fun setLastBlockTime(time: Long) {
        val sharedPref = accessibilityService.getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putLong("last_block_time", time)
            apply()
        }
    }

    // Recupera o tempo do último bloqueio
    private fun getLastBlockTime(): Long {
        val sharedPref = accessibilityService.getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getLong("last_block_time", 0)
    }
    /**
     * Verifica se o aplicativo atual é alvo do monitoramento.
     */
    private fun isTargetApp(packageName: String?): Boolean {
        return packageName?.contains("com.twitter.android") == true || // Twitter (X)
                packageName?.contains("com.instagram.android") == true || // Instagram
                packageName?.contains("com.linkedin.android") == true || // LinkedIn
                packageName?.contains("com.threads") == true ||
                packageName?.contains("com.instagram.barcelona") == true ||
                packageName?.contains("com.facebook.katana") == true // Facebook
    }

    /**
     * Retorna o limite de rolagem personalizado com base no pacote do aplicativo.
     */
    private fun getScrollLimitForApp(packageName: String?): Int {
        return when {
            packageName?.contains("com.instagram.android") == true -> 400 // Instagram
            packageName?.contains("com.twitter.android") == true -> 250 // Twitter (X)
            packageName?.contains("com.instagram.barcelona") == true -> Integer.MAX_VALUE // Threads
            packageName?.contains("com.threads") == true -> Integer.MAX_VALUE // Threads
            else -> 500 // Limite padrão para outros apps
        }
    }

    /**
     * verifica evento após rolagem e realiza bloqueio caso necessário.
     */
    fun handleScrollClickEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString()

        Log.d("BlockAccessService", "Estado da janela mudou. Pacote: $packageName")
        resetScrollCountIfNeeded()
        
        // Verifica se o aplicativo é um dos que queremos monitorar
        if (isScrollLimitEnabled() && isTargetApp(packageName)) {
            // Verifica se o bloqueio de 10 minutos está ativo
            if (totalScroll > scrollLimit || totalScrollY > scrollLimitThreads || limiteThreds) {

                blockAccess()
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
    /**
     * Lida com eventos de rolagem e realiza bloqueio caso necessário.
     */
    fun handleScrollEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?) {
        rootNode ?: return
        val packageName = event.packageName?.toString()

        resetScrollCountIfNeeded()

        if (isScrollLimitEnabled() && isTargetApp(packageName)) {
            val fromIndex = event.fromIndex
            val toIndex = event.toIndex
            val scrollY = event.scrollY

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
            else if (scrollY > 0) {
                totalScrollY = scrollY // Usa scrollY como fallback
                Log.d("ScrollMonitor", "Rolagem detectada com scrollY: $scrollY")
            } else {
                Log.d("ScrollMonitor", "Índices inválidos e scrollY não disponível")
                totalScroll += 1;
            }

            Log.d("ControleRolagem", "Total rolagem: $totalScroll, Total Y: $totalScrollY")

            if (totalScroll > scrollLimit || totalScrollY > scrollLimitThreads || limiteThreds) {
                Log.d("ScrollMonitor", "Limite de rolagem alcançado. Fechando o app.")
//                Log.d("ScrollMonitor", "totalScroll > scrollLimit  ${totalScroll > scrollLimit}")
//                Log.d("ScrollMonitor", "totalScrollY > scrollLimitThreads ${totalScrollY > scrollLimitThreads} ")
//                Log.d("ScrollMonitor", " limiteThreds ${limiteThreds}")

                // Navegar para a tela inicial
                if(scrollY <= 0){
                    limiteThreds = true;
                    Handler().postDelayed({ blockAccess() }, 2000) // Atraso de 1 segundo
                }else {
                    blockAccess()
                }

            }
            }
        }

    private fun blockAccess() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        accessibilityService.startActivity(intent)
    }


    private fun isPostInstagram(node: AccessibilityNodeInfo?): Boolean {
        if(node != null) {

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
        return false
    }

 }
