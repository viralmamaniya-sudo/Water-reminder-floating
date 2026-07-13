package com.viral.waterreminder

import android.app.AlarmManager
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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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

        val permissionButton = Button(this).apply {
            text = "1. Allow floating character"
        }

        val startButton = Button(this).apply {
            text = "2. Start hourly reminders"
        }

        val testButton = Button(this).apply {
            text = "Test floating reminder now"
        }

        box.addView(title)
        box.addView(permissionButton)
        box.addView(startButton)
        box.addView(testButton)

        setContentView(box)

        permissionButton.setOnClickListener {
            if (!Settings.canDrawOverlays(this@MainActivity)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        startButton.setOnClickListener {
            ReminderScheduler.schedule(
                this@MainActivity,
                60 * 60 * 1000L
            )

            Toast.makeText(
                this@MainActivity,
                "Hourly reminders started",
                Toast.LENGTH_SHORT
            ).show()
        }

        testButton.setOnClickListener {
            if (Settings.canDrawOverlays(this@MainActivity)) {
                Overlay.show(this@MainActivity)
            } else {
                Toast.makeText(
                    this@MainActivity,
                    "Allow floating permission first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

object ReminderScheduler {

    fun schedule(context: Context, delay: Long) {
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            7,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = System.currentTimeMillis() + delay

        try {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (Settings.canDrawOverlays(context)) {
            Overlay.show(context)
        }

        ReminderScheduler.schedule(
            context,
            60 * 60 * 1000L
        )
    }
}

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.schedule(
                context,
                60 * 60 * 1000L
            )
        }
    }
}

object Overlay {

    private var currentView: View? = null

    fun show(context: Context) {
        if (currentView != null || !Settings.canDrawOverlays(context)) {
            return
        }

        val windowManager =
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(30, 30, 30, 30)
            setBackgroundColor(Color.argb(245, 255, 255, 255))
        }

        val message = TextView(context).apply {
            text = "Hey Viral! 💧\nTime to drink some water!"
            textSize = 21f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(24, 50, 74))
        }

        val image = ImageView(context).apply {
            setImageResource(R.drawable.anime_avatar)
            adjustViewBounds = true
        }

        val buttonRow = LinearLayout(context).apply {
            gravity = Gravity.CENTER
        }

        val doneButton = Button(context).apply {
            text = "Done ✓"
        }

        val snoozeButton = Button(context).apply {
            text = "Snooze 10 min"
        }

        buttonRow.addView(doneButton)
        buttonRow.addView(snoozeButton)

        card.addView(message)

        card.addView(
            image,
            LinearLayout.LayoutParams(600, 700)
        )

        card.addView(buttonRow)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.END
            x = 20
            y = 60
        }

        fun removeOverlay() {
            currentView?.let {
                try {
                    windowManager.removeView(it)
                } catch (_: Exception) {
                }
            }

            currentView = null
        }

        doneButton.setOnClickListener {
            removeOverlay()
        }

        snoozeButton.setOnClickListener {
            removeOverlay()

            ReminderScheduler.schedule(
                context,
                10 * 60 * 1000L
            )
        }

        currentView = card

        try {
            windowManager.addView(card, params)
        } catch (_: Exception) {
            currentView = null
        }
    }
}
