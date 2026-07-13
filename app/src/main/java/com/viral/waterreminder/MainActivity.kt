package com.viral.waterreminder

import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

private const val PREFS = "water_reminder_prefs"
private const val KEY_NAME = "user_name"

fun savedName(context: Context): String =
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getString(KEY_NAME, "")?.trim().orEmpty()

fun saveName(context: Context, name: String) {
    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().putString(KEY_NAME, name.trim()).apply()
}

class MainActivity : Activity() {
    private lateinit var title: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
        if (savedName(this).isBlank()) showNameDialog(firstLaunch = true)
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 64, 48, 64)
        }

        title = TextView(this).apply {
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(24, 50, 74))
        }
        updateTitle()

        val allowOverlay = Button(this).apply { text = "1. ALLOW FLOATING CHARACTER" }
        val startHourly = Button(this).apply { text = "2. START HOURLY REMINDERS" }
        val testNow = Button(this).apply { text = "TEST FLOATING REMINDER NOW" }
        val changeName = Button(this).apply { text = "CHANGE NAME" }

        root.addView(title)
        root.addView(allowOverlay)
        root.addView(startHourly)
        root.addView(testNow)
        root.addView(changeName)
        setContentView(root)

        allowOverlay.setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                Toast.makeText(this, "Floating permission is already enabled", Toast.LENGTH_SHORT).show()
            }
        }

        startHourly.setOnClickListener {
            ReminderScheduler.scheduleHourly(this)
            Toast.makeText(this, "Hourly water reminders started", Toast.LENGTH_SHORT).show()
        }

        testNow.setOnClickListener {
            if (Settings.canDrawOverlays(this)) Overlay.show(this)
            else Toast.makeText(this, "Allow floating permission first", Toast.LENGTH_SHORT).show()
        }

        changeName.setOnClickListener { showNameDialog(firstLaunch = false) }
    }

    private fun updateTitle() {
        val name = savedName(this)
        title.text = if (name.isBlank()) "Anime Water Reminder 💧" else "Hello, $name! 💧"
    }

    private fun showNameDialog(firstLaunch: Boolean) {
        val input = EditText(this).apply {
            hint = "Enter your name"
            setText(savedName(this@MainActivity))
            setSingleLine(true)
            setPadding(36, 24, 36, 24)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (firstLaunch) "Welcome! 💧" else "Change your name")
            .setMessage("What's your name? We'll use it in your water reminders.")
            .setView(input)
            .setPositiveButton("SAVE") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveName(this, name)
                    updateTitle()
                } else if (firstLaunch) {
                    Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
                    showNameDialog(true)
                }
            }
            .apply { if (!firstLaunch) setNegativeButton("CANCEL", null) }
            .create()

        dialog.setCanceledOnTouchOutside(!firstLaunch)
        dialog.setCancelable(!firstLaunch)
        dialog.show()
    }
}

object ReminderScheduler {
    private const val REQUEST_HOURLY = 7001
    private const val REQUEST_SNOOZE = 7002
    private const val ONE_HOUR_MS = 60L * 60L * 1000L

    fun scheduleHourly(context: Context) = schedule(context, ONE_HOUR_MS, REQUEST_HOURLY)
    fun scheduleSnooze(context: Context) = schedule(context, 10L * 60L * 1000L, REQUEST_SNOOZE)

    private fun schedule(context: Context, delayMs: Long, requestCode: Int) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context, requestCode, Intent(context, ReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val trigger = System.currentTimeMillis() + delayMs
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            } else {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
            }
        } catch (_: SecurityException) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pending)
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (Settings.canDrawOverlays(context)) Overlay.show(context)
        ReminderScheduler.scheduleHourly(context)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) ReminderScheduler.scheduleHourly(context)
    }
}

object Overlay {
    private var currentView: View? = null

    fun show(context: Context) {
        if (currentView != null || !Settings.canDrawOverlays(context)) return

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val name = savedName(context).ifBlank { "there" }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(18, 14, 18, 14)
            setBackgroundColor(Color.argb(248, 255, 255, 255))
        }

        val image = ImageView(context).apply {
            setImageResource(R.drawable.anime_avatar)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        val right = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(12, 0, 0, 0)
        }

        val message = TextView(context).apply {
            text = "Hey $name! 💧\nTime to drink some water!"
            textSize = 17f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(24, 50, 74))
        }

        val buttons = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val done = Button(context).apply {
            text = "DONE ✓"
            textSize = 12f
            minWidth = 0
            minimumWidth = 0
        }
        val snooze = Button(context).apply {
            text = "SNOOZE\n10 MIN"
            textSize = 11f
            minWidth = 0
            minimumWidth = 0
        }

        buttons.addView(done, LinearLayout.LayoutParams(0, 110, 1f))
        buttons.addView(snooze, LinearLayout.LayoutParams(0, 110, 1.15f))
        right.addView(message)
        right.addView(buttons, LinearLayout.LayoutParams(360, 120))

        card.addView(image, LinearLayout.LayoutParams(180, 220))
        card.addView(right)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 18
            y = 70
        }

        fun remove() {
            currentView?.let {
                try { wm.removeView(it) } catch (_: Exception) {}
            }
            currentView = null
        }

        done.setOnClickListener { remove() }
        snooze.setOnClickListener {
            remove()
            ReminderScheduler.scheduleSnooze(context)
        }

        currentView = card
        try { wm.addView(card, params) } catch (_: Exception) { currentView = null }
    }
}
