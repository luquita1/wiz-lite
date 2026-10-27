package com.example.wizlite

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import kotlin.concurrent.thread

/** days: bitmask, bit0 = lunes ... bit6 = domingo */
data class Schedule(
    val id: Int, val hour: Int, val minute: Int, val days: Int,
    val on: Boolean, val dimming: Int, val enabled: Boolean
)

object ScheduleStore {
    private fun prefs(c: Context) = c.getSharedPreferences("wiz", Context.MODE_PRIVATE)

    fun load(c: Context): List<Schedule> {
        val arr = JSONArray(prefs(c).getString("schedules", "[]"))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            Schedule(o.getInt("id"), o.getInt("h"), o.getInt("m"), o.getInt("d"),
                o.getBoolean("on"), o.getInt("dim"), o.getBoolean("en"))
        }
    }

    fun save(c: Context, list: List<Schedule>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().put("id", it.id).put("h", it.hour).put("m", it.minute)
                .put("d", it.days).put("on", it.on).put("dim", it.dimming).put("en", it.enabled))
        }
        prefs(c).edit().putString("schedules", arr.toString()).apply()
    }
}

object Scheduler {
    const val ACTION = "com.example.wizlite.SCHEDULE"

    private fun nextTrigger(s: Schedule): Long? {
        val now = System.currentTimeMillis()
        for (offset in 0..7) {
            val c = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, offset)
                set(Calendar.HOUR_OF_DAY, s.hour); set(Calendar.MINUTE, s.minute)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }
            val dow = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7 // lunes = 0
            if ((s.days and (1 shl dow)) != 0 && c.timeInMillis > now) return c.timeInMillis
        }
        return null
    }

    private fun pi(ctx: Context, id: Int): PendingIntent {
        val i = Intent(ctx, ScheduleReceiver::class.java).setAction(ACTION).putExtra("id", id)
        return PendingIntent.getBroadcast(ctx, id, i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    fun schedule(ctx: Context, s: Schedule) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        am.cancel(pi(ctx, s.id))
        if (!s.enabled) return
        val t = nextTrigger(s) ?: return
        if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms())
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi(ctx, s.id))
        else
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pi(ctx, s.id))
    }

    fun cancel(ctx: Context, id: Int) {
        ctx.getSystemService(AlarmManager::class.java).cancel(pi(ctx, id))
    }

    fun scheduleAll(ctx: Context) = ScheduleStore.load(ctx).forEach { schedule(ctx, it) }
}

class ScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getIntExtra("id", -1)
        val s = ScheduleStore.load(ctx).find { it.id == id } ?: return
        Scheduler.schedule(ctx, s) // programa la siguiente repetición
        val ip = ctx.getSharedPreferences("wiz", Context.MODE_PRIVATE).getString("ip", "") ?: ""
        if (ip.isBlank()) return

        val pending = goAsync()
        thread {
            try {
                val p = JSONObject().put("state", s.on)
                if (s.on) p.put("dimming", s.dimming)
                var ok = false
                repeat(2) {
                    if (!ok) { ok = WizClient.setPilot(ip, p); if (!ok) Thread.sleep(1500) }
                }
            } finally { pending.finish() }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Scheduler.scheduleAll(ctx)
    }
}
