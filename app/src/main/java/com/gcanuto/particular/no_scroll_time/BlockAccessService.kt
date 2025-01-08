package com.gcanuto.particular.no_scroll_time

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class BlockAccessService : AccessibilityService() {

    private val handler = Handler()
    private val delayMillis: Long = 3000
    private val youtubeHandler = YoutubeAccessHandler()
    private val instagramHandler = InstagramAccessHandler(this)
    private val controleRolagemHandler = ControleRolagemPostHandler(this) // Inicialização

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

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            val packageName = event.packageName?.toString()
            val rootNode = rootInActiveWindow

            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                controleRolagemHandler.handleScrollClickEvent(event)
             }

            // Controle de rolagem
            if (event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED || event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                controleRolagemHandler.handleScrollEvent(event, rootNode)
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

    override fun onInterrupt() {
        Log.d("BlockAccessService", "Serviço de Acessibilidade interrompido")
    }
}
