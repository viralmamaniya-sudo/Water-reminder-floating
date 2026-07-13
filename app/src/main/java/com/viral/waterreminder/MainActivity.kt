package com.viral.waterreminder

import android.app.*
import android.content.*
import android.graphics.Color
import android.net.Uri
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 80, 40, 80)
        }
        val title = TextView(this).apply {
            text = "Viral's Anime Water Reminder 💧"
            textSize = 24f
            gravity = Gravity.CENTER
        }
        val permission = Button(this).apply { text = "1. Allow floating character" }
        val start = Button(this).apply { text = "2. Start hourly reminders" }
        val test = Button(this).apply { text = "Test floating reminder now" }
        box.addView(title); box.addView(permission); box.addView(start); box.addView(test)
        setContentView(box)

        permission.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        }
        start.setOnClickListener {
            ReminderScheduler.schedule(this, 60 * 60 * 1000L)
            Toast.makeText(this, "Hourly reminders started", Toast.LENGTH_SHORT).show()
        }
        test.setOnClickListener {
            if (Settings.canDrawOverlays(this)) Overlay.show(this)
            else Toast.makeText(this, "Allow floating permission first", Toast.LENGTH_SHORT).show()
        }
    }
}

object ReminderScheduler {
    fun schedule(context: Context, delay: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(context, 7, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val at = System.currentTimeMillis() + delay
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarm.canScheduleExactAlarms())
                alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
            else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } catch (_: SecurityException) {
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context) {
        if (Settings.canDrawOverlays(context)) Overlay.show(context)
        ReminderScheduler.schedule(context, 60 * 60 * 1000L)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED)
            ReminderScheduler.schedule(context, 60 * 60 * 1000L)
    }
}

object Overlay {
    private var view: View? = null
    fun show(context: Context) {
        if (view != null || !Settings.canDrawOverlays(context)) return
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.argb(245, 255, 255, 255))
        }
        val msg = TextView(context).apply {
            text = "Hey Viral! 💧\nTime to drink some water!"
            textSize = 21f; gravity = Gravity.CENTER
            setTextColor(Color.rgb(24, 50, 74))
        }
        val image = ImageView(context).apply {
            setImageResource(com.viral.waterreminder.R.drawable.anime_avatar)
            adjustViewBounds = true
        }
        val buttons = LinearLayout(context).apply { gravity = Gravity.CENTER }
        val done = Button(context).apply { text = "Done ✓" }
        val snooze = Button(context).apply { text = "Snooze 10 min" }
        buttons.addView(done); buttons.addView(snooze)
        card.addView(msg)
        card.addView(image, LinearLayout.LayoutParams(600, 700))
        card.addView(buttons)

        val type = if (Build.VERSION.SDK_INT >= 26)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.BOTTOM or Gravity.END; x = 20; y = 60 }

        fun remove() {
            view?.let { try { wm.removeView(it) } catch (_: Exception) {} }
            view = null
        }
        done.setOnClickListener { remove() }
        snooze.setOnClickListener {
            remove()
            ReminderScheduler.schedule(context, 10 * 60 * 1000L)
        }
        view = card
        try { wm.addView(card, params) } catch (_: Exception) { view = null }
    }
}
