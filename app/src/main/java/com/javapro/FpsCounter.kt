package com.javapro

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.Choreographer
import kotlin.math.roundToInt

@Composable
fun DraggableFpsCounter() {
    var fps by remember { mutableStateOf(0) }
    var offset by remember { mutableStateOf(IntOffset(20, 100)) } 

    LaunchedEffect(Unit) {
        val callback = object : Choreographer.FrameCallback {
            var lastTime = 0L
            override fun doFrame(frameTimeNanos: Long) {
                if (lastTime != 0L) {
                    val diff = frameTimeNanos - lastTime
                    fps = (1_000_000_000L / diff).toInt()
                }
                lastTime = frameTimeNanos
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
    }

    Text(
        text = "FPS: $fps",
        color = Color.Black,
        fontSize = 12.sp,
        modifier = Modifier
            .offset { offset }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset = IntOffset(
                        (offset.x + dragAmount.x).roundToInt(),
                        (offset.y + dragAmount.y).roundToInt()
                    )
                }
            }
            .background(Color.White.copy(alpha = 0.5f)) 
            .padding(4.dp)
    )
}
