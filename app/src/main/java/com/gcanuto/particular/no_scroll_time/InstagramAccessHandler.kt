package com.gcanuto.particular.no_scroll_time

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class InstagramAccessHandler(private val accessibilityService: AccessibilityService) {

    //********************************
    //Código logica Instragam
    fun onAccessibilityInstagram(event: AccessibilityEvent?, rootNode: AccessibilityNodeInfo?) {
        try {
            if (event != null) {
                val eventPackageName = event.packageName?.toString()
                val eventType = event.eventType

                // Verifica se está no aplicativo Instagram
                if (eventPackageName != null && eventPackageName.contains("com.instagram.android")) {
                    // Verifica se o evento é um clique
                    if (eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
                        Log.d("BlockReelsAccessService", "Clique detectado")
                        handleViewClickInstagram(rootNode)
                        handleWindowChangeInstagram(rootNode)
                    }

                    // Verifica eventos de mudança de estado da janela
                    if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                        Log.d(
                            "BlockReelsAccessService",
                            "Evento TYPE_WINDOW_STATE_CHANGED detectado."
                        )
                        handleViewClickInstagram(rootNode)
                        handleWindowChangeInstagram(rootNode)
                    }
                }
            }

        } catch (e: Exception) {
            Log.e("BlockReelsAccessService", "Erro ao processar o evento de acessibilidade", e)
        }
    }

    fun handleViewClickInstagram(rootNode: AccessibilityNodeInfo?) {
        rootNode ?: return
        logNodeHierarchyInstagram(rootNode)
        if (isReelsScreenInstagram(rootNode)) {
            Log.d("BlockReelsAccessService", "Tela de Reels detectada.")
            Handler().postDelayed({
                checkReelsScreenInstagram(rootNode)
            }, 200)
        }
    }

    fun handleWindowChangeInstagram(rootNode: AccessibilityNodeInfo?) {
        rootNode  ?: return
        logNodeHierarchyInstagram(rootNode)

        // Aqui você garante que a tela está sendo verificada após a mudança de janela
        if (isReelsScreenInstagram(rootNode)) {
            Log.d(
                "BlockReelsAccessService",
                "Tela de Reels detectada após a mudança de janela. Tentando clicar no botão 'Home'..."
            )

            val homeButton = findHomeButtonInstagram(rootNode)
            if (homeButton != null) {
                homeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d(
                    "BlockReelsAccessService",
                    "Botão 'Home' clicado com sucesso após mudança de janela."
                )
            } else {
                Log.e(
                    "BlockReelsAccessService",
                    "Botão 'Home' não encontrado após mudança de janela."
                )
                // Caso o botão 'Home' não seja encontrado, simula o pressionamento do botão 'Voltar'
                Log.d(
                    "BlockReelsAccessService",
                    "Tentando pressionar o botão 'Voltar'..."
                )
                performBackAction()
            }
        }
    }

    // Função para simular o pressionamento do botão de 'Voltar' do Android
    private fun performBackAction() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        Log.d("BlockReelsAccessService", "Ação de 'Voltar' realizada.")
    }

    private fun checkReelsScreenInstagram(rootNode: AccessibilityNodeInfo?) {
        if (rootNode != null) {
            logNodeHierarchyInstagram(rootNode)
            if (isReelsScreenInstagram(rootNode)) {
                Log.d(
                    "BlockReelsAccessService",
                    "Tela de Reels detectada. Tentando clicar no botão 'Home'..."
                )

                val homeButton = findHomeButtonInstagram(rootNode)

                if (homeButton != null) {
                    homeButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("BlockReelsAccessService", "Botão 'Home' clicado com sucesso.")
                } else {
                    Log.e("BlockReelsAccessService", "Botão 'Home' não encontrado.")

                }
            }
        } else {
            Log.e("BlockReelsAccessService", "Root node is null")
        }
    }

   private fun findHomeButtonInstagram(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            val className = child.className?.toString() ?: ""
            val contentDescription = child.contentDescription?.toString() ?: ""

            if (className == "android.widget.FrameLayout" && contentDescription.contains(
                    "Home",
                    ignoreCase = true
                )
            ) {
                return child
            }
            val foundButton = findHomeButtonInstagram(child)
            if (foundButton != null) return foundButton
        }
        return null
    }

   fun logNodeHierarchyInstagram(node: AccessibilityNodeInfo, depth: Int = 0) {
        val prefix = "  ".repeat(depth)
        val className = node.className ?: "Unknown"
        val text = node.text ?: "No text"
        val contentDescription = node.contentDescription ?: "No content description"
//        Log.d(
//            "BlockReelsAccessService",
//            "$prefix Class: $className, Text: $text, ContentDescription: $contentDescription"
//        )

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                logNodeHierarchyInstagram(child, depth + 1)
            }
        }
    }

    private fun isReelsScreenInstagram(node: AccessibilityNodeInfo): Boolean {
        val className = node.className ?: "Unknown"
        val text = node.text ?: "No text"
        val contentDescription = node.contentDescription ?: "No content description"

        // Verificando se estamos na tela de Reels
        if ((className.contains("ViewGroup", ignoreCase = true) &&
                    contentDescription.contains("Original audio", ignoreCase = true))
            &&
            (className.contains("FrameLayout", ignoreCase = true) &&
                    contentDescription.contains("Double tap to play or pause", ignoreCase = true))
        ) {
//            Log.d(
//                "BlockReelsAccessService",
//                "Elemento 'Reels' encontrado por Original audio e: Class: $className, Text: $text, ContentDescription: $contentDescription"
//            )
            return true
        }

        // Verificação recursiva nos filhos
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (isReelsScreenInstagram(child)) {
                return true
            }
        }

        // Adicionando verificações extras
        if (isSubscriptionsScreenInstagram(node)) {
            return true
        }

        return false
    }

    private fun isSubscriptionsScreenInstagram(node: AccessibilityNodeInfo): Boolean {
        val requiredElements = listOf(
            Pair("ImageView", "More"),
            Pair("ImageView", "Share"),
            Pair("ImageView", "Comment"),
            Pair("ImageView", "Like"),
            Pair("ViewGroup", "Reel")
        )

        // Contagem de elementos encontrados
        var elementsFoundCount = 0

        // Para cada elemento esperado, verificamos se ele está presente na hierarquia
        for (requiredElement in requiredElements) {
            val (className, contentDescription) = requiredElement
            val found = findElementInHierarchyInstagram(node, className, contentDescription)

//            Log.d(
//                "BlockReelsAccessService",
//                "Elemento (Class: $className, ContentDesc: $contentDescription) encontrado: $found"
//            )

            if (found) {
                elementsFoundCount++
            }
        }

        // Se todos os elementos foram encontrados (tamanho da lista == contagem encontrada), retornamos true
        if (elementsFoundCount == requiredElements.size) {
            Log.d("BlockReelsAccessService", "Todos os elementos encontrados.")
            return true
        }

        // Log.d("BlockReelsAccessService", "Nem todos os elementos foram encontrados.")
        return false
    }

    private fun findElementInHierarchyInstagram(
        node: AccessibilityNodeInfo,
        className: String,
        contentDescription: String
    ): Boolean {
        // Verifica se o nó atual contém os critérios fornecidos (caso insensível)
        val classMatch = node.className?.contains(className, ignoreCase = true) == true
        val contentDescriptionToCheck =
            node.contentDescription?.toString() ?: "No content description"
        val contentDescMatch =
            contentDescriptionToCheck.contains(contentDescription, ignoreCase = true)

        if (classMatch && contentDescMatch) {
            return true
        }

        // Caso contrário, verifica todos os filhos do nó
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            if (findElementInHierarchyInstagram(child, className, contentDescription)) {
                return true // Se encontrado, retorna imediatamente
            }
        }

        return false // Se não encontrado, retorna false
    }
}