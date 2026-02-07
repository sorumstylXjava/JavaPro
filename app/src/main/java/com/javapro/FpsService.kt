package com.javapro

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.TextView
import com.javapro.utils.TweakExecutor
import kotlinx.coroutines.*
import java.util.regex.Pattern

class FpsService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var fpsView: View
    private lateinit var params: WindowManager.LayoutParams
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var isRunning = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        fpsView = LayoutInflater.from(this).inflate(R.layout.overlay_fps, null)

        val layoutType = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100

        // Fitur Geser (Drag)
        fpsView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x ; initialY = params.y
                        initialTouchX = event.rawX ; initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(fpsView, params)
                        return true
                    }
                }
                return false
            }
        })

        windowManager.addView(fpsView, params)
        startHardwareFpsMonitoring()
    }

    private fun startHardwareFpsMonitoring() {
        isRunning = true
        serviceScope.launch {
            // Memicu TimeStats sesuai kode C++ mu
            TweakExecutor.execute("dumpsys SurfaceFlinger --timestats -enable")
            
            var lastTotalFrames = 0L
            var lastTime = System.currentTimeMillis()

            while (isRunning) {
                // Kita ambil angka totalFrames dari sistem
                val output = TweakExecutor.executeWithOutput("dumpsys SurfaceFlinger --timestats -dump | grep totalFrames")
                
                // Gunakan Regex yang lebih kuat untuk menangkap angka
                val currentTotalFrames = extractFrames(output)
                val currentTime = System.currentTimeMillis()

                if (lastTotalFrames != 0L && currentTotalFrames > lastTotalFrames) {
                    val frameDiff = currentTotalFrames - lastTotalFrames
                    val timeDiff = (currentTime - lastTime) / 1000.0
                    
                    if (timeDiff > 0) {
                        val rawFps = (frameDiff / timeDiff).toInt()
                        
                        // Validasi angka: Jika HP 60Hz tapi dapet 62, kita bulatkan ke 60
                        val finalFps = if (rawFps in 58..62) 60 else if (rawFps > 120) 120 else rawFps

                        withContext(Dispatchers.Main) {
                            val textView = fpsView.findViewById<TextView>(R.id.fps_text)
                            textView?.text = "$finalFps"
                            
                            // Efek warna kalau nge-drop
                            if (finalFps < 45) textView?.setTextColor(android.graphics.Color.RED)
                            else textView?.setTextColor(android.graphics.Color.WHITE)
                        }
                    }
                }
                
                lastTotalFrames = currentTotalFrames
                lastTime = currentTime
                
                // Interval 500ms (Cepat tapi hemat baterai)
                delay(500)
            }
        }
    }

    private fun extractFrames(input: String): Long {
        return try {
            // Mencari angka setelah "totalFrames ="
            val p = Pattern.compile("totalFrames\\s*=\\s*(\\d+)")
            val m = p.matcher(input)
            if (m.find()) m.group(1)?.toLong() ?: 0L else 0L
        } catch (e: Exception) { 0L }
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        serviceScope.launch {
            TweakExecutor.execute("dumpsys SurfaceFlinger --timestats -disable")
        }
        if (::fpsView.isInitialized) windowManager.removeView(fpsView)
    }
}
