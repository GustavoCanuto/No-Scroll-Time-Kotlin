package com.gcanuto.particular.no_scroll_time

import android.app.Activity
import android.util.Log
import android.view.MotionEvent

class MyActivity : Activity() {
    private var previousY: Float = 0f
    private var totalScroll: Int = 0

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> {
                val deltaY = event.y - previousY // Quantidade de movimento no eixo Y
                previousY = event.y  // Atualiza a posição anterior
                val scrollAmount = Math.abs(deltaY) // Valor absoluto de deltaY (quantidade de rolagem)
                totalScroll += scrollAmount.toInt()

                Log.d("ScrollMonitor", "Rolagem detectada: $scrollAmount, Total rolado: $totalScroll")
            }
        }
        return true
    }
}
