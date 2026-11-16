package com.tarjuman.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var tvModelStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvModelStatus = findViewById(R.id.tvModelStatus)
        val btnOverlay = findViewById<MaterialButton>(R.id.btnOverlayPermission)
        val btnAccessibility = findViewById<MaterialButton>(R.id.btnAccessibilityPermission)
        val btnToggleFloating = findViewById<MaterialButton>(R.id.btnToggleFloating)

        // تحميل نماذج الترجمة (عربي <-> إنجليزي) للعمل أوفلاين
        TranslationHelper.downloadModelsIfNeeded { status ->
            runOnUiThread {
                tvModelStatus.text = status
            }
        }

        // 1. طلب إذن الظهور فوق التطبيقات الأخرى
        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "إذن الظهور فوق التطبيقات مفعل بالفعل ✅", Toast.LENGTH_SHORT).show()
            }
        }

        // 2. فتح إعدادات إمكانية الوصول لتفعيل قراءة الشاشة
        btnAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
            Toast.makeText(this, "ابحث عن (ترجمان) وقم بتفعيله", Toast.LENGTH_LONG).show()
        }

        // 3. تشغيل الفقاعة العائمة (الريشة)
        btnToggleFloating.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "يرجى تفعيل إذن الظهور فوق التطبيقات أولاً!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val serviceIntent = Intent(this, FloatingTranslatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Toast.makeText(this, "تم إطلاق أيقونة الريشة العائمة!", Toast.LENGTH_SHORT).show()
            moveTaskToBack(true) // تصغير التطبيق للعمل فوق التطبيقات الأخرى
        }
    }
}
