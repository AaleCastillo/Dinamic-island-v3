package com.example.island

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val d = resources.displayMetrics.density
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding((24 * d).toInt(), 0, (24 * d).toInt(), 0)
        }
        col.addView(TextView(this).apply { text = "Isla"; textSize = 36f; gravity = Gravity.CENTER })
        status = TextView(this).apply {
            textSize = 16f; gravity = Gravity.CENTER
            setPadding(0, (16 * d).toInt(), 0, (24 * d).toInt())
        }
        col.addView(status)
        col.addView(Button(this).apply {
            text = "1 · Permiso de superposición"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            }
        })
        col.addView(Button(this).apply {
            text = "2 · Acceso a notificaciones"
            setOnClickListener { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
        })
        setContentView(col)
    }

    override fun onResume() {
        super.onResume()
        val overlay = Settings.canDrawOverlays(this)
        val notif = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = "Superposición: ${if (overlay) "✓" else "✗"}\nNotificaciones: ${if (notif) "✓" else "✗"}"
    }
}
