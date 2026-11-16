package com.tarjuman.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingTranslatorService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
        setupFloatingWindow()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "tarjuman_glass_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ترجمان العائم",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ترجمان نشط")
            .setContentText("الريشة العائمة جاهزة فوق تطبيقاتك")
            .setSmallIcon(R.drawable.ic_quill_ink)
            .build()

        startForeground(101, notification)
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_translator, null)

        val layoutFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // إزالة FLAG_BLUR_BEHIND نهائياً حتى لا يحجب النظام اللمس عن الشاشة الخلفية
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 36
            y = 280
        }

        windowManager.addView(floatingView, params)

        val bubbleContainer = floatingView.findViewById<FrameLayout>(R.id.floatingBubbleContainer)
        val expandedPanel = floatingView.findViewById<LinearLayout>(R.id.expandedPanelContainer)
        val ivQuill = floatingView.findViewById<ImageView>(R.id.ivQuillIcon)

        val tvDirection = floatingView.findViewById<TextView>(R.id.tvDetectedDirection)
        val btnMinimize = floatingView.findViewById<TextView>(R.id.btnMinimize)
        val btnReadScreen = floatingView.findViewById<TextView>(R.id.btnReadScreen)
        val btnTranslateNow = floatingView.findViewById<TextView>(R.id.btnTranslateNow)
        val btnPasteClipboard = floatingView.findViewById<TextView>(R.id.btnPasteClipboard)
        val btnClearText = floatingView.findViewById<TextView>(R.id.btnClearText)
        val btnCopyResult = floatingView.findViewById<TextView>(R.id.btnCopyResult)
        val etSourceText = floatingView.findViewById<EditText>(R.id.etSourceText)
        val tvTranslatedResult = floatingView.findViewById<TextView>(R.id.tvTranslatedResult)

        ObjectAnimator.ofFloat(ivQuill, "translationY", -3f, 3f).apply {
            duration = 1600
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        // سحب وتحريك الريشة + فتح لوحة الترجمة عند النقر دون إعاقة الشاشة
        bubbleContainer.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (abs(event.rawX - initialTouchX) < 12 && abs(event.rawY - initialTouchY) < 12) {
                            // إظهار اللوحة الزجاجية دون حجب اللمس عن التطبيقات الأخرى
                            bubbleContainer.visibility = View.GONE
                            expandedPanel.visibility = View.VISIBLE
                            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                            windowManager.updateViewLayout(floatingView, params)
                        }
                        return true
                    }
                }
                return false
            }
        })

        // إغلاق اللوحة والعودة للريشة وتحرير التركيز بالكامل
        btnMinimize.setOnClickListener {
            expandedPanel.visibility = View.GONE
            bubbleContainer.visibility = View.VISIBLE
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
            windowManager.updateViewLayout(floatingView, params)
        }

        // مسح الشاشة وترجمتها عند الضغط على الزر المخصص
        btnReadScreen.setOnClickListener {
            readAndTranslateScreen(etSourceText, tvTranslatedResult, tvDirection)
        }

        btnTranslateNow.setOnClickListener {
            val text = etSourceText.text.toString()
            TranslationHelper.autoTranslate(
                text,
                onResult = { translated, dir ->
                    tvDirection.text = dir
                    tvTranslatedResult.text = translated
                },
                onError = { err -> tvTranslatedResult.text = err }
            )
        }

        btnPasteClipboard.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrEmpty()) {
                etSourceText.setText(clipText)
                TranslationHelper.autoTranslate(
                    clipText,
                    onResult = { translated, dir ->
                        tvDirection.text = dir
                        tvTranslatedResult.text = translated
                    },
                    onError = { err -> tvTranslatedResult.text = err }
                )
            } else {
                Toast.makeText(this, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
            }
        }

        btnClearText.setOnClickListener {
            etSourceText.setText("")
            tvTranslatedResult.text = "..."
            tvDirection.text = "عربي ⇄ English"
        }

        btnCopyResult.setOnClickListener {
            val result = tvTranslatedResult.text.toString()
            if (result.isNotEmpty() && result != "...") {
                val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Tarjuman", result))
                Toast.makeText(this, "تم النسخ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readAndTranslateScreen(etSource: EditText, tvResult: TextView, tvDirection: TextView) {
        val service = ScreenReaderAccessibilityService.instance
        if (service == null) {
            tvResult.text = "فعّل (ترجمان) من إعدادات إمكانية الوصول لمسح الشاشة تلقائياً، أو الصق النص هنا."
            return
        }

        // إخفاء اللوحة لحظة المسح لقراءة التطبيق الخلفي بدقة ثم إظهارها فوراً
        val expandedPanel = floatingView.findViewById<LinearLayout>(R.id.expandedPanelContainer)
        expandedPanel.visibility = View.INVISIBLE

        expandedPanel.postDelayed({
            val screenText = service.extractVisibleScreenText()
            expandedPanel.visibility = View.VISIBLE

            if (screenText.isNotEmpty()) {
                etSource.setText(screenText)
                TranslationHelper.autoTranslate(
                    screenText,
                    onResult = { translated, dir ->
                        tvDirection.text = dir
                        tvResult.text = translated
                    },
                    onError = { err -> tvResult.text = err }
                )
            } else {
                tvResult.text = "لا توجد نصوص قابلة للقراءة في هذه الواجهة."
            }
        }, 120)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
