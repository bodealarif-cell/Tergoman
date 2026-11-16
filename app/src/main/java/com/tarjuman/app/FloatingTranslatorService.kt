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
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat

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
        val channelId = "tarjuman_overlay_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ترجمان - المترجم العائم",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("ترجمان نشط")
            .setContentText("أيقونة الريشة جاهزة للترجمة فوق التطبيقات")
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

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        // جعل النافذة لا تمنع الكتابة في التطبيقات الأخرى عند تصغيرها
        params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE

        windowManager.addView(floatingView, params)

        val bubbleContainer = floatingView.findViewById<FrameLayout>(R.id.floatingBubbleContainer)
        val expandedPanel = floatingView.findViewById<LinearLayout>(R.id.expandedPanelContainer)
        val ivQuill = floatingView.findViewById<ImageView>(R.id.ivQuillIcon)

        val btnMinimize = floatingView.findViewById<TextView>(R.id.btnMinimize)
        val btnReadScreen = floatingView.findViewById<Button>(R.id.btnReadScreen)
        val btnTranslateNow = floatingView.findViewById<Button>(R.id.btnTranslateNow)
        val btnPasteClipboard = floatingView.findViewById<Button>(R.id.btnPasteClipboard)
        val btnCopyResult = floatingView.findViewById<Button>(R.id.btnCopyResult)
        val etSourceText = floatingView.findViewById<EditText>(R.id.etSourceText)
        val tvTranslatedResult = floatingView.findViewById<TextView>(R.id.tvTranslatedResult)

        // حركة نبض خفيفة لأيقونة الريشة
        val pulseAnimator = ObjectAnimator.ofFloat(ivQuill, "translationY", -4f, 4f).apply {
            duration = 1400
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }

        // تحريك أيقونة الريشة بالسحب + النقر لفتح الترجمة
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
                        val diffX = Math.abs(event.rawX - initialTouchX)
                        val diffY = Math.abs(event.rawY - initialTouchY)
                        if (diffX < 10 && diffY < 10) {
                            // فتح لوحة الترجمة وتفعيل لوحة المفاتيح عند الحاجة
                            bubbleContainer.visibility = View.GONE
                            expandedPanel.visibility = View.VISIBLE
                            params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                            windowManager.updateViewLayout(floatingView, params)

                            // قراءة نص الشاشة تلقائياً بمجرد الضغط على الريشة!
                            readAndTranslateScreen(etSourceText, tvTranslatedResult)
                        }
                        return true
                    }
                }
                return false
            }
        })

        // تصغير اللوحة والعودة لأيقونة الريشة
        btnMinimize.setOnClickListener {
            expandedPanel.visibility = View.GONE
            bubbleContainer.visibility = View.VISIBLE
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            windowManager.updateViewLayout(floatingView, params)
        }

        // زر ترجمة الشاشة الحالية فوق أي تطبيق
        btnReadScreen.setOnClickListener {
            readAndTranslateScreen(etSourceText, tvTranslatedResult)
        }

        // ترجمة النص المكتوب يدوياً
        btnTranslateNow.setOnClickListener {
            val text = etSourceText.text.toString()
            tvTranslatedResult.text = "جاري الترجمة..."
            TranslationHelper.autoTranslate(
                text,
                onResult = { translated -> tvTranslatedResult.text = translated },
                onError = { err -> tvTranslatedResult.text = err }
            )
        }

        // لصق من الحافظة وترجمته فوراً
        btnPasteClipboard.setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
            if (!clipText.isNullOrEmpty()) {
                etSourceText.setText(clipText)
                TranslationHelper.autoTranslate(
                    clipText,
                    onResult = { translated -> tvTranslatedResult.text = translated },
                    onError = { err -> tvTranslatedResult.text = err }
                )
            } else {
                Toast.makeText(this, "الحافظة فارغة", Toast.LENGTH_SHORT).show()
            }
        }

        // نسخ الترجمة
        btnCopyResult.setOnClickListener {
            val result = tvTranslatedResult.text.toString()
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("TarjumanTranslation", result))
            Toast.makeText(this, "تم نسخ الترجمة!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readAndTranslateScreen(etSource: EditText, tvResult: TextView) {
        val accessibilityService = ScreenReaderAccessibilityService.instance
        if (accessibilityService == null) {
            tvResult.text = "⚠️ يرجى تفعيل خدمة (ترجمان) من إعدادات إمكانية الوصول لقراءة نصوص التطبيقات تلقائياً، أو الصق النص يدوياً."
            return
        }

        val screenText = accessibilityService.extractVisibleScreenText()
        if (screenText.isNotEmpty()) {
            etSource.setText(screenText)
            tvResult.text = "جاري الترجمة الفورية..."
            TranslationHelper.autoTranslate(
                screenText,
                onResult = { translated -> tvResult.text = translated },
                onError = { err -> tvResult.text = err }
            )
        } else {
            tvResult.text = "لم يتم العثور على نصوص قابلة للقراءة في هذه الشاشة."
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}
