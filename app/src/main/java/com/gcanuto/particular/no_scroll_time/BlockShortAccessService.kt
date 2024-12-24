package com.gcanuto.particular.no_scroll_time

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class BlockShortAccessService : AccessibilityService() {

    private val handler = Handler() // Criar o Handler
    private val delayMillis: Long = 5000 // 10 segundos

    private val periodicTask = object : Runnable {
        override fun run() {
            try {
                // Obter o root da tela atual
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    logNodeHierarchy(rootNode)

                    // Executar as funções que você quer a cada 10 segundos
                    handleViewClick(null)  // Passe o evento real aqui se necessário
                    handleWindowChange()

                } else {
                    Log.e("BlockAccessService", "Root node is null")
                }
            } catch (e: Exception) {
                Log.e("BlockAccessService", "Erro ao processar a tarefa periódica", e)
            }

            // Reexecuta o Runnable a cada 10 segundos
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
        try {
            if (event != null) {
                val eventPackageName = event.packageName?.toString()
                val eventType = event.eventType

                // Verifica se está no aplicativo YouTube
                if (eventPackageName != null && eventPackageName.contains("com.google.android.youtube")) {
                    // Verifica se o evento é um clique
                    if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                        Log.d("BlockAccessService", "Clique detectado")
                        handleViewClick(event)
                        handleWindowChange()
                    }

                    // Verifica eventos de mudança de estado da janela
                    if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                        val textList = event.text?.joinToString(", ") ?: ""
                        Log.d(
                            "BlockAccessService",
                            "Evento TYPE_WINDOW_STATE_CHANGED detectado."
                        )
                            Log.d(
                                "BlockAccessService",
                                "Mudança de janela no YouTube detectada com texto [YouTube]"
                            )
                            handleViewClick(event)
                            handleWindowChange()

                    }
                }

            }

        } catch (e: Exception) {
            Log.e("BlockAccessService", "Erro ao processar o evento de acessibilidade", e)
        }
    }

    override fun onInterrupt() {
        Log.d("BlockAccessService", "Serviço de Acessibilidade interrompido")
    }

    private fun handleViewClick(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        logNodeHierarchy(rootNode)
        if (isShortsScreen(rootNode)) {
            Log.d("BlockAccessService", "Detectado clique em prévia de Shorts")
            Handler().postDelayed({
                checkShortsScreen()
            }, 250)
        }
    }

    private fun handleWindowChange() {
        val rootNode = rootInActiveWindow ?: return
        logNodeHierarchy(rootNode)

        // Aqui você garante que a tela está sendo verificada após a mudança de janela
        if (isShortsScreen(rootNode)) {
            Log.d("BlockAccessService", "Tela de Shorts detectada após a mudança de janela. Tentando clicar no botão 'Home'...")

            val homeButton = findHomeButton(rootNode)
            if (homeButton != null) {
                homeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("BlockAccessService", "Botão 'Home' clicado com sucesso após mudança de janela.")
            } else {
                Log.e("BlockAccessService", "Botão 'Home' não encontrado após mudança de janela.")
            }
        }
    }

    private fun checkShortsScreen() {
        val rootNode = rootInActiveWindow
        if (rootNode != null) {
            logNodeHierarchy(rootNode)
            if (isShortsScreen(rootNode)) {
                Log.d("BlockAccessService", "Tela de Shorts detectada. Tentando clicar no botão 'Home'...")

                val homeButton = findHomeButton(rootNode)
                if (homeButton != null) {
                    homeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("BlockAccessService", "Botão 'Home' clicado com sucesso.")
                } else {
                    Log.e("BlockAccessService", "Botão 'Home' não encontrado.")
                }
            }
        } else {
            Log.e("BlockAccessService", "Root node is null")
        }
    }

    private fun findHomeButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val className = child.className?.toString() ?: ""
            val contentDescription = child.contentDescription?.toString() ?: ""

            if (className == "android.widget.Button" && contentDescription.equals("Home", ignoreCase = true)) {
                return child
            }
            val foundButton = findHomeButton(child)
            if (foundButton != null) return foundButton
        }
        return null
    }

    private fun logNodeHierarchy(node: AccessibilityNodeInfo, depth: Int = 0) {
        val prefix = "  ".repeat(depth)
        val className = node.className ?: "Unknown"
        val text = node.text ?: "No text"
        val contentDescription = node.contentDescription ?: "No content description"
        Log.d("BlockAccessService", "$prefix Class: $className, Text: $text, ContentDescription: $contentDescription")

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                logNodeHierarchy(child, depth + 1)
            }
        }
    }

    private fun isShortsScreen(node: AccessibilityNodeInfo): Boolean {
        val className = node.className ?: "Unknown"
        val text = node.text ?: "No text"
        val contentDescription = node.contentDescription ?: "No content description"

        if (
            ( className.contains("ViewGroup", ignoreCase = true) &&
            contentDescription.contains("See more videos using this sound", ignoreCase = true) )
        ) {
            Log.d(
                "BlockAccessService",
                "Elemento 'Shorts' encontrado: Class: $className, Text: $text, ContentDescription: $contentDescription"
            )
            return true
        }

        // Recursivamente verificar os filhos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (isShortsScreen(child)) {
                return true // Se encontrado no filho, retornar imediatamente
            }
        }

        return isSubscriptionsScreen(node)
    }

    private fun isSubscriptionsScreen(node: AccessibilityNodeInfo): Boolean {
        val requiredElements = listOf(
            // Cada item é um par (className, contentDescription)
            Pair("Button", "Subscriptions"),
            Pair("HorizontalScrollView", ""),
            Pair("View", "Video Progress"),
            Pair("ImageView", "Go to channel"),
            Pair("Button", "Subscriptions"),
            Pair("ImageView", "Search"),
            Pair("ImageView", "Search"),
            Pair("ImageView", "More"),
            Pair("FrameLayout", ""),
            Pair("ImageButton", "Navigate up")

        )
        // Contagem de elementos encontrados
        var elementsFoundCount = 0

        // Para cada elemento esperado, verificamos se ele está presente na hierarquia
        for (requiredElement in requiredElements) {
            val (className, contentDescription) = requiredElement
            val found = findElementInHierarchy(node, className, contentDescription)

            Log.d("BlockAccessService", "Elemento (Class: $className, ContentDesc: $contentDescription) encontrado: $found")

            if (found) {
                elementsFoundCount++
            }
        }

        // Se todos os elementos foram encontrados (tamanho da lista == contagem encontrada), retornamos true
        if (elementsFoundCount == requiredElements.size) {
            Log.d("BlockAccessService", "Todos os elementos encontrados.")
            return true
        }

        Log.d("BlockAccessService", "Nem todos os elementos foram encontrados.")
        return false
    }

    private fun findElementInHierarchy(node: AccessibilityNodeInfo, className: String, contentDescription: String): Boolean {
        // Verifica se o nó atual contém os critérios fornecidos (caso insensível)
        val classMatch = node.className?.contains(className, ignoreCase = true) == true
        val contentDescriptionToCheck = node.contentDescription?.toString() ?: "No content description"
        val contentDescMatch = contentDescriptionToCheck.contains(contentDescription, ignoreCase = true)

        if (classMatch && contentDescMatch) {
            return true
        }

        // Caso contrário, verifica todos os filhos do nó
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findElementInHierarchy(child, className, contentDescription)) {
                return true // Se encontrado, retorna imediatamente
            }
        }

        return false // Se não encontrado, retorna false
    }

}