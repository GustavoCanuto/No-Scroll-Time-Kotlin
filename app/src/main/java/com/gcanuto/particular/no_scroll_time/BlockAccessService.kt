package com.gcanuto.particular.no_scroll_time

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BlockAccessService : AccessibilityService() {

    private val handler = Handler() // Criar o Handler
    private val delayMillis: Long = 3000 // 3 segundos
    private val youtubeHandler = YoutubeAccessHandler()
    private val instagramHandler = InstagramAccessHandler(this)

    private val periodicTask = object : Runnable {
        override fun run() {
            try {
                // Obter o root da tela atual
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    // Obter o nome do pacote do aplicativo ativo
                    val currentPackageName = rootNode.packageName?.toString()

                    // Verificar se o aplicativo ativo é o YouTube
                    if (currentPackageName != null && currentPackageName.contains("com.google.android.youtube")) {
                        youtubeHandler.logNodeHierarchyYoutube(rootNode) //youtube
                        // Executar as funções que você quer a cada 3
                        youtubeHandler.handleViewClickYoube(rootNode)  // Passe o evento real aqui se necessário
                        youtubeHandler.handleWindowChangeYoutube(rootNode)
                    }
                    // Verificar se o aplicativo ativo é o Instagram
                    else if (currentPackageName != null && currentPackageName.contains("com.instagram.android")) {
                        instagramHandler.logNodeHierarchyInstagram(rootNode)
                        // Executar as funções que você quer a cada 3 segundos
                        instagramHandler.handleViewClickInstagram(rootNode)  // Passe o evento real aqui se necessário
                        instagramHandler.handleWindowChangeInstagram(rootNode)
                    }

                } else {
                    Log.e("BlockAccessService", "Root node is null")
                }
            } catch (e: Exception) {
                Log.e("BlockAccessService", "Erro ao processar a tarefa periódica", e)
            }

            // Reexecuta o Runnable a cada 3 segundos
            handler.postDelayed(this, delayMillis)
        }
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_VIEW_CLICKED or
                    AccessibilityEvent.TYPE_VIEW_FOCUSED or
                    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_VIEW_HOVER_EXIT or
                    AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 100
        }
        serviceInfo = info
        Log.d("BlockAccessService", "Serviço de Acessibilidade conectado")

        // Inicia a execução periódica
        handler.post(periodicTask)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event != null) {
            val packageName = event.packageName?.toString()

            // Obter o rootNode aqui
            val rootNode = rootInActiveWindow

            if (packageName != null && packageName.contains("com.google.android.youtube")) {
                Log.d("BlockAccessService", "Evento detectado no YouTube.")
                youtubeHandler.onAccessibilityYoutube(event, rootNode)
            }
            else if (packageName != null && packageName.contains("com.instagram.android")) {
                Log.d("BlockAccessService", "Evento detectado no Instagram.")
                 instagramHandler.onAccessibilityInstagram(event, rootNode)
            } else {
                Log.d("BlockAccessService", "Evento detectado em outro aplicativo.")
            }
            }
    }

    override fun onInterrupt() {
        Log.d("BlockAccessService", "Serviço de Acessibilidade interrompido")
    }




}
