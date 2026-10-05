package com.subaruplayer.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import androidx.compose.ui.test.down
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.test.up
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.subaruplayer.app.ui.player.PlayerGestureCallbacks
import com.subaruplayer.app.ui.player.playerGestures
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Exercita a camada de toque do player com gestos reais injetados pelo Compose. */
@RunWith(AndroidJUnit4::class)
class PlayerGesturesTest {
    @get:Rule
    val rule = createComposeRule()

    private val events = mutableListOf<String>()

    @Before
    fun setUp() {
        events.clear()
        rule.setContent {
            Box(
                Modifier.size(400.dp, 300.dp).testTag("video").playerGestures(
                    PlayerGestureCallbacks(
                        onTap = { _, _, _ -> events += "tap" },
                        onHold = { events += "hold:$it" },
                        onSwipeUp = { events += "up" },
                        onSwipeDown = { events += "down" },
                        onPinch = { events += "pinch:$it" },
                    ),
                ),
            )
        }
    }

    private fun touch(block: androidx.compose.ui.test.TouchInjectionScope.() -> Unit) {
        rule.onNodeWithTag("video").performTouchInput(block)
        rule.waitForIdle()
    }

    @Test
    fun toqueCurto() {
        touch { click() }
        assertEquals(listOf("tap"), events)
    }

    @Test
    fun segurarLigaEDesliga() {
        touch { down(center) }
        rule.mainClock.advanceTimeBy(800)
        touch { up() }
        assertEquals(listOf("hold:true", "hold:false"), events)
    }

    @Test
    fun deslizarParaCima() {
        touch { swipeUp() }
        assertEquals(listOf("up"), events)
    }

    @Test
    fun deslizarParaBaixo() {
        touch { swipeDown() }
        assertEquals(listOf("down"), events)
    }

    @Test
    fun arrastoHorizontalEIgnorado() {
        touch { swipeLeft() }
        touch { swipeRight() }
        assertEquals(emptyList<String>(), events)
    }

    @Test
    fun pincaAfastandoLigaZoom() {
        touch { pinch(center - Offset(40f, 0f), center - Offset(250f, 0f), center + Offset(40f, 0f), center + Offset(250f, 0f)) }
        assertEquals(listOf("pinch:true"), events)
    }

    @Test
    fun pincaAproximandoDesligaZoom() {
        touch { pinch(center - Offset(250f, 0f), center - Offset(40f, 0f), center + Offset(250f, 0f), center + Offset(40f, 0f)) }
        assertEquals(listOf("pinch:false"), events)
    }

    @Test
    fun pincaPequenaNaoFazNada() {
        touch { pinch(center - Offset(100f, 0f), center - Offset(104f, 0f), center + Offset(100f, 0f), center + Offset(104f, 0f)) }
        assertEquals(emptyList<String>(), events)
    }
}
