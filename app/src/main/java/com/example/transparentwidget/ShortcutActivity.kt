package com.example.transparentwidget

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ShortcutActivity : AppCompatActivity() {

    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(TransparentWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        val packageName = prefs.getString("shortcut_target", null)

        if (packageName != null) {
            packageManager.getLaunchIntentForPackage(packageName)?.let { intent ->
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                startActivity(intent)
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
            prefs.edit().putString("shortcut_target", appInfo.packageName).apply()
            Toast.makeText(this, "設定完了！ドックにこのアイコンを配置してください", Toast.LENGTH_LONG).show()
            finish()
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
}
