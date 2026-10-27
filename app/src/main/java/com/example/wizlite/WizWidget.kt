package com.example.wizlite

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import org.json.JSONObject
import kotlin.concurrent.thread

class WizWidget : AppWidgetProvider() {

    override fun onUpdate(ctx: Context, mgr: AppWidgetManager, ids: IntArray) {
        ids.forEach { mgr.updateAppWidget(it, buildViews(ctx)) }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        super.onReceive(ctx, intent)
        val action = intent.action ?: return
        if (action !in listOf(TOGGLE, LOW, HIGH)) return
        val ip = ctx.getSharedPreferences("wiz", Context.MODE_PRIVATE).getString("ip", "") ?: ""
        if (ip.isBlank()) return

        val pending = goAsync()
        thread {
            try {
                when (action) {
                    TOGGLE -> {
                        val on = WizClient.getState(ip) ?: false
                        WizClient.setPilot(ip, JSONObject().put("state", !on))
                    }
                    LOW -> WizClient.setPilot(ip, JSONObject().put("state", true).put("dimming", 20))
                    HIGH -> WizClient.setPilot(ip, JSONObject().put("state", true).put("dimming", 100))
                }
            } finally { pending.finish() }
        }
    }

    private fun buildViews(ctx: Context): RemoteViews {
        val v = RemoteViews(ctx.packageName, R.layout.widget)
        v.setOnClickPendingIntent(R.id.btn_toggle, pi(ctx, TOGGLE))
        v.setOnClickPendingIntent(R.id.btn_low, pi(ctx, LOW))
        v.setOnClickPendingIntent(R.id.btn_high, pi(ctx, HIGH))
        return v
    }

    private fun pi(ctx: Context, action: String): PendingIntent {
        val i = Intent(ctx, WizWidget::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            ctx, action.hashCode(), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val TOGGLE = "com.example.wizlite.TOGGLE"
        const val LOW = "com.example.wizlite.LOW"
        const val HIGH = "com.example.wizlite.HIGH"
    }
}
