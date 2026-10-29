package com.example.island

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {
    private lateinit var status: TextView
    private val sliders = LinkedHashMap<String, Pair<SeekBar, Int>>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(48), dp(24), dp(24))
        }
        col.addView(TextView(this).apply { text = "Isla"; textSize = 36f; gravity = Gravity.CENTER })
        status = TextView(this).apply {
            textSize = 16f; gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, dp(16))
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
        col.addView(TextView(this).apply {
            text = "Posición"; textSize = 22f
            setPadding(0, dp(32), 0, 0)
        })
        slider(col, "Horizontal (← →)", "x", -200, 200, 0)
        slider(col, "Vertical (↑ ↓)", "y", -20, 200, 10)
        col.addView(Button(this).apply {
            text = "Restablecer posición"
            setOnClickListener {
                val e = getSharedPreferences("isla", MODE_PRIVATE).edit()
                sliders.forEach { (k, v) -> v.first.progress = v.second; e.putInt(k, v.second) }
                e.apply()
                IslandListener.instance?.preview()
            }
        })
        setContentView(ScrollView(this).apply { addView(col) })
    }

    private fun slider(parent: LinearLayout, label: String, key: String, lo: Int, hi: Int, def: Int) {
        val sp = getSharedPreferences("isla", MODE_PRIVATE)
        val tv = TextView(this).apply { textSize = 16f; setPadding(0, dp(20), 0, 0) }
        val sb = SeekBar(this)
        sb.min = lo; sb.max = hi; sb.progress = sp.getInt(key, def)
        tv.text = "$label: ${sb.progress}"
        sb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar, v: Int, user: Boolean) {
                tv.text = "$label: $v"
                if (user) {
                    sp.edit().putInt(key, v).apply()
                    IslandListener.instance?.preview()
                }
            }
            override fun onStartTrackingTouch(s: SeekBar) {}
            override fun onStopTrackingTouch(s: SeekBar) {}
        })
        sliders[key] = Pair(sb, def)
        parent.addView(tv); parent.addView(sb)
    }

    override fun onResume() {
        super.onResume()
        val overlay = Settings.canDrawOverlays(this)
        val notif = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        status.text = "Superposición: ${if (overlay) "✓" else "✗"}\nNotificaciones: ${if (notif) "✓" else "✗"}"
    }
}
