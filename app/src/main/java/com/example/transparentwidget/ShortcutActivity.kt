package com.example.transparentwidget

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShortcutActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TARGET_PACKAGE = "target_package"
    }

    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val targetPackage = intent.getStringExtra(EXTRA_TARGET_PACKAGE)
        if (targetPackage != null) {
            packageManager.getLaunchIntentForPackage(targetPackage)?.let { launchIntent ->
                launchIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                startActivity(launchIntent)
            }
            overridePendingTransition(0, 0)
            finish()
            return
        }

        setContentView(R.layout.activity_config)

        val recyclerView = findViewById<RecyclerView>(R.id.app_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        val adapter = AppListAdapter(emptyList()) { appInfo ->
            createPinnedShortcut(appInfo.packageName)
        }
        recyclerView.adapter = adapter

        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val q = s?.toString() ?: ""
                adapter.updateList(if (q.isEmpty()) allApps else allApps.filter { it.name.contains(q, true) })
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val apps = pm.queryIntentActivities(intent, 0)
                .map { AppInfo(it.loadLabel(pm).toString(), it.activityInfo.packageName, it.loadIcon(pm)) }
                .sortedBy { it.name.lowercase() }
            allApps.addAll(apps)
            runOnUiThread { adapter.updateList(apps) }
        }.start()
    }

    private fun createPinnedShortcut(packageName: String) {
        val transparentBitmap = Bitmap.createBitmap(108, 108, Bitmap.Config.ARGB_8888)

        val launchIntent = Intent(this, ShortcutActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(EXTRA_TARGET_PACKAGE, packageName)
        }

        val shortcut = ShortcutInfoCompat.Builder(this, "shortcut_${packageName}_${System.currentTimeMillis()}")
            .setShortLabel(" ")
            .setIcon(IconCompat.createWithBitmap(transparentBitmap))
            .setIntent(launchIntent)
            .build()

        ShortcutManagerCompat.requestPinShortcut(this, shortcut, null)
        finish()
    }
}
