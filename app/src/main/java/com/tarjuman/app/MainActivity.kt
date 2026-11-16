package com.tarjuman.app

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvModelStatus: TextView
    private lateinit var tvOverlayBadge: TextView
    private lateinit var tvAccessBadge: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvModelStatus = findViewById(R.id.tvModelStatus)
        tvOverlayBadge = findViewById(R.id.tvOverlayBadge)
        tvAccessBadge = findViewById(R.id.tvAccessBadge)

        val btnOverlay = findViewById<LinearLayout>(R.id.btnOverlayPermission)
        val btnAccessibility = findViewById<LinearLayout>(R.id.btnAccessibilityPermission)
        val btnToggleFloating = findViewById<TextView>(R.id.btnToggleFloating)

        TranslationHelper.downloadModelsIfNeeded { status, ready ->
            runOnUiThread {
                tvModelStatus.text = status
                tvModelStatus.setTextColor(if (ready) Color.parseColor("#38BDF8") else Color.parseColor("#E2E8F0"))
            }
        }

        btnOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            } else {
                Toast.makeText(this, "الإذن مفعّل", Toast.LENGTH_SHORT).show()
            }
        }

        btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        btnToggleFloating.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "فعّل الظهور فوق التطبيقات أولاً", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val serviceIntent = Intent(this, FloatingTranslatorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            moveTaskToBack(true)
        }
    }

    override fun onResume() {
        super.onResume()
        updatePermissionBadges()
    }

    private fun updatePermissionBadges() {
        if (Settings.canDrawOverlays(this)) {
            tvOverlayBadge.text = "مفعّل ✓"
            tvOverlayBadge.setTextColor(Color.parseColor("#34D399"))
        } else {
            tvOverlayBadge.text = "مطلوب"
            tvOverlayBadge.setTextColor(Color.parseColor("#FBBF24"))
        }

        if (ScreenReaderAccessibilityService.instance != null) {
            tvAccessBadge.text = "مفعّل ✓"
            tvAccessBadge.setTextColor(Color.parseColor("#34D399"))
        } else {
            tvAccessBadge.text = "تفعيل"
            tvAccessBadge.setTextColor(Color.parseColor("#38BDF8"))
        }
    }
}
