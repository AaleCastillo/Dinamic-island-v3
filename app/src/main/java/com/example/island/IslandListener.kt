package com.example.island

import android.animation.ValueAnimator
import android.app.Notification
import android.app.PendingIntent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class IslandListener : NotificationListenerService() {
    private val ui = Handler(Looper.getMainLooper())
    private lateinit var wm: WindowManager
    private var root: FrameLayout? = null
    private var pill: LinearLayout? = null
    private var icon: ImageView? = null
    private var titleV: TextView? = null
    private var textV: TextView? = null
    private val bg = GradientDrawable().apply { setColor(Color.BLACK) }
    private var radius = 0f
    private var anim: ValueAnimator? = null
    private var expanded = false
    private var lastKey: String? = null
    private var lastTitle = ""
    private var tapIntent: PendingIntent? = null
    private val collapse = Runnable { setExpanded(false) }
    private val remove = Runnable { detach() }

    private val categories = setOf(
        Notification.CATEGORY_CALL, Notification.CATEGORY_TRANSPORT, Notification.CATEGORY_ALARM,
        Notification.CATEGORY_PROGRESS, Notification.CATEGORY_NAVIGATION, Notification.CATEGORY_STOPWATCH
    )

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onListenerConnected() {
        activeNotifications?.firstOrNull { relevant(it) }?.let { ui.post { present(it) } }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (relevant(sbn)) ui.post { present(sbn) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        if (sbn.key == lastKey) ui.post { dismiss() }
    }

    override fun onDestroy() {
        detach()
        super.onDestroy()
    }

    private fun relevant(s: StatusBarNotification) =
        s.packageName != packageName && s.isOngoing && s.notification.category in categories

    private fun present(sbn: StatusBarNotification) {
        if (!Settings.canDrawOverlays(this)) return
        ui.removeCallbacks(remove)
        val n = sbn.notification
        val title = (n.extras.getCharSequence(Notification.EXTRA_TITLE) ?: "").toString()
        val text = (n.extras.getCharSequence(Notification.EXTRA_TEXT) ?: "").toString()
        tapIntent = n.contentIntent
        if (root == null) attach()
        val accent = getColor(android.R.color.system_accent1_200)
        icon?.setImageDrawable(n.smallIcon?.loadDrawable(this)?.mutate()?.apply { setTint(accent) })
        titleV?.text = title
        textV?.text = text
        val changed = sbn.key != lastKey || title != lastTitle
        lastKey = sbn.key
        lastTitle = title
        if (changed) peek()
    }

    private fun peek() {
        setExpanded(true)
        ui.removeCallbacks(collapse)
        ui.postDelayed(collapse, 4000)
    }

    private fun attach() {
        val r = FrameLayout(this)
        val p = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), 0, dp(14), 0)
            background = bg
        }
        val ic = ImageView(this)
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val t = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
        }
        val s = TextView(this).apply {
            setTextColor(0xB3FFFFFF.toInt()); textSize = 12f
            maxLines = 1; ellipsize = TextUtils.TruncateAt.END
            visibility = View.GONE
        }
        col.addView(t); col.addView(s)
        p.addView(ic, LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(10) })
        p.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        p.setOnClickListener {
            if (expanded) {
                try { tapIntent?.send() } catch (_: Exception) {}
                setExpanded(false)
            } else peek()
        }
        r.addView(p, FrameLayout.LayoutParams(dp(60), dp(14)))
        radius = dp(7).toFloat()
        bg.cornerRadius = radius
        val lp = WindowManager.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL; y = dp(10) }
        wm.addView(r, lp)
        root = r; pill = p; icon = ic; titleV = t; textV = s
    }

    private fun setExpanded(e: Boolean) {
        expanded = e
        textV?.visibility = if (e) View.VISIBLE else View.GONE
        if (e) animateTo(dp(320), dp(76), dp(32).toFloat())
        else animateTo(dp(150), dp(36), dp(18).toFloat())
    }

    private fun animateTo(w: Int, h: Int, r: Float) {
        val p = pill ?: return
        anim?.cancel()
        val w0 = p.layoutParams.width
        val h0 = p.layoutParams.height
        val r0 = radius
        anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 420
            interpolator = OvershootInterpolator(1.1f)
            addUpdateListener {
                val t = it.animatedValue as Float
                p.layoutParams.width = (w0 + (w - w0) * t).toInt()
                p.layoutParams.height = (h0 + (h - h0) * t).toInt()
                radius = r0 + (r - r0) * t
                bg.cornerRadius = radius
                p.requestLayout()
            }
            start()
        }
    }

    private fun dismiss() {
        ui.removeCallbacks(collapse)
        lastKey = null
        lastTitle = ""
        animateTo(dp(40), dp(12), dp(6).toFloat())
        ui.postDelayed(remove, 450)
    }

    private fun detach() {
        anim?.cancel()
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null; pill = null; icon = null; titleV = null; textV = null
        expanded = false
    }
}
