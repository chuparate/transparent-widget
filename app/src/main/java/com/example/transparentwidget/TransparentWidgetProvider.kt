package com.example.transparentwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.RemoteViews

class TransparentWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_RECONFIGURE) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val configIntent = Intent(context, ConfigActivity::class.java).apply {
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(configIntent)
            }
        }
    }

    companion object {
        const val ACTION_RECONFIGURE = "com.example.transparentwidget.RECONFIGURE"
        const val PREFS_NAME = "TransparentWidgetPrefs"
        const val PREF_PREFIX_KEY = "appwidget_"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val prefs: SharedPreferences =
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val packageName = prefs.getString(PREF_PREFIX_KEY + appWidgetId, null)

            val views = RemoteViews(context.packageName, R.layout.widget_layout)

            if (packageName != null) {
                // タップ → アプリ起動
                val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (launchIntent != null) {
                    launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_tap_area, pendingIntent)
                }
            }

            // 長押し → 再設定（Android 12+対応）
            val reconfigureIntent = Intent(context, TransparentWidgetProvider::class.java).apply {
                action = ACTION_RECONFIGURE
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            // Note: 長押しメニューはAndroidシステムが自動提供
            // "ウィジェットの設定"からConfigActivityが再起動される

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        fun savePackageName(context: Context, appWidgetId: Int, packageName: String) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(PREF_PREFIX_KEY + appWidgetId, packageName).apply()
        }

        fun deletePackageName(context: Context, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().remove(PREF_PREFIX_KEY + appWidgetId).apply()
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            deletePackageName(context, appWidgetId)
        }
    }
}
