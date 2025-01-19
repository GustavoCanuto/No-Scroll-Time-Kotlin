package com.gcanuto.particular.no_scroll_time

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BlockAccessService : AccessibilityService() {

    private val handler = Handler()
    private val delayMillis: Long = 3000
    private val youtubeHandler = YoutubeAccessHandler()
    private val instagramHandler = InstagramAccessHandler(this)
    private val controleRolagemHandler = ControleRolagemPostHandler(this) // Inicialização
    private var isBackActionRunning = false // Variável de controle para o loop de ação
    val blockedUrls = listOf(
        "youtube.com", "instagram.com", "facebook.com", "threads.net", "twitter.com"
    )

    private val periodicTask = object : Runnable {
        override fun run() {

            try {

                val rootNode = rootInActiveWindow

                if (rootNode != null && isBlockingEnabled()) {
                    val currentPackageName = rootNode.packageName?.toString()

                    // Log.d("BlockAccessService", "Pacote acessado: $currentPackageName")
                    if (currentPackageName?.contains("com.opera.browser") == true) {
                        Log.d("BlockAccessService", "opera acessado")
                        goHome()
                    }

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

        event?.let {
            val packageName = event.packageName?.toString()
            val rootNode = rootInActiveWindow
            Log.d("BlockAccessService", "Pacote acessado: $packageName")

            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                controleRolagemHandler.handleScrollClickEvent(event)
            }

            // Controle de rolagem
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                controleRolagemHandler.handleScrollEvent(event, rootNode)
            }

            if (isWebBlockEnabled() && packageName != null) {
                if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED ||
                    event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED
                ) {

                    checkForBlockedBrowsers(packageName)



                    // Verifica se o pacote é de um navegador conhecido
                    if (packageName.contains("com.android.chrome")
                    ) {
                        logAllNodes(rootNode)

                        // Tente capturar o URL da barra de endereços
                        val currentUrl = getCurrentUrlFromAddressBar(rootNode)
                        if (currentUrl != null) {
                            Log.d("BlockAccessService", "URL capturado: $currentUrl")
                            if (blockedUrls.any { url ->
                                    currentUrl.contains(
                                        url,
                                        ignoreCase = true
                                    )
                                }) {
                                redirectToPaginaAnterior()
                            }
                        } else {
                            Log.d("BlockAccessService", "URL não encontrado na barra de endereços.")
                        }
                    } else {
                        Log.d(
                            "BlockAccessService",
                            "Pacote não é de um navegador conhecido: $packageName"
                        )
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


    private fun redirectToPaginaAnterior() {
        if (!isBackActionRunning) {
            isBackActionRunning = true // Inicia o loop de ações

            val handler = Handler(Looper.getMainLooper())
            val backActionRunnable = object : Runnable {
                override fun run() {
                    val rootNode = rootInActiveWindow
                    val currentUrl = getCurrentUrlFromAddressBar(rootNode)

                    if (currentUrl != null && blockedUrls.any { url ->
                            currentUrl.contains(
                                url,
                                ignoreCase = true
                            )
                        }) {
                        // URL ainda é bloqueado, continua pressionando "Voltar"
                        Log.d(
                            "BlockAccessService",
                            "URL ainda bloqueado: $currentUrl. Pressionando voltar."
                        )
                        performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                        handler.postDelayed(
                            this,
                            1500
                        ) // Reagenda o próximo "Voltar" após 1,5 segundos
                    } else {
                        // URL mudou, encerra o loop
                        Log.d(
                            "BlockAccessService",
                            "URL liberado ou não encontrado. Parando ações de voltar."
                        )
                        isBackActionRunning = false
                    }
                }
            }

            // Delay inicial de 1 segundo antes de começar o loop
            handler.postDelayed({
                backActionRunnable.run()
            }, 1000) // Delay inicial de 1 segundo
        } else {
            Log.d("BlockAccessService", "Ação de voltar já está em execução.")
        }
    }


    private fun logAllNodes(rootNode: AccessibilityNodeInfo?, depth: Int = 0) {
        if (rootNode == null) return
        val prefix = "-".repeat(depth)
        Log.d(
            "BlockAccessService",
            "$prefix Node: ${rootNode.viewIdResourceName}, Text: ${rootNode.text}, Class: ${rootNode.className}"
        )
        for (i in 0 until rootNode.childCount) {
            logAllNodes(rootNode.getChild(i), depth + 1)
        }
    }

    private fun getCurrentUrlFromAddressBar(rootNode: AccessibilityNodeInfo?): String? {
        val chromeNodes =
            rootNode?.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar")
        if (chromeNodes.isNullOrEmpty()) {
            Log.d("BlockAccessService", "Barra de endereços não encontrada.")
            return null
        }
        val url = chromeNodes.firstOrNull()?.text?.toString()
        Log.d("BlockAccessService", "URL capturado: $url")
        return url
    }

    // Função para verificar o pacote do navegador e bloquear outros navegadores
    private fun checkForBlockedBrowsers(packageName: String) {
        // Lista de pacotes dos navegadores bloqueados
        val blockedPackages = listOf(
            "org.mozilla.firefox",    // Firefox
            "com.microsoft.emmx",     // Edge
            "com.opera.browser",  // Opera
            "com.hsv.freeadblockerbrowser"

        )

        if (blockedPackages.any { packageName.contains(it, ignoreCase = true) }) {
            // Bloqueia o navegador (retorna para a tela inicial)
            Log.d("BlockAccessService", "Navegador bloqueado detectado: $packageName")
            goHome()
        }
    }

    // Função para ir para a tela inicial
    private fun goHome() {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun isWebBlockEnabled(): Boolean {
        val sharedPref = getSharedPreferences("no_scroll_time_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("web_block_enabled", false)
    }

    override fun onInterrupt() {
        Log.d("BlockAccessService", "Serviço de Acessibilidade interrompido")
    }
}
