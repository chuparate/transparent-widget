package com.example.transparentwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView

class ConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var adapter: AppListAdapter
    private val allApps = mutableListOf<AppInfo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ウィジェットIDを取得
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // キャンセル結果をデフォルトにセット
        setResult(RESULT_CANCELED)

        setContentView(R.layout.activity_config)

        // RecyclerView セットアップ
        val recyclerView = findViewById<RecyclerView>(R.id.app_list)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(
            DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        )

        adapter = AppListAdapter(emptyList()) { appInfo ->
            onAppSelected(appInfo)
        }
        recyclerView.adapter = adapter

        // 検索バー
        val searchBar = findViewById<EditText>(R.id.search_bar)
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // アプリ一覧をバックグラウンドで読み込み
        loadApps()
    }

    private fun loadApps() {
        Thread {
            val pm = packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfoList = pm.queryIntentActivities(intent, 0)
            val apps = resolveInfoList
                .map { ri ->
                    AppInfo(
                        name = ri.loadLabel(pm).toString(),
                        packageName = ri.activityInfo.packageName,
                        icon = ri.loadIcon(pm)
                    )
                }
                .sortedBy { it.name.lowercase() }

            allApps.clear()
            allApps.addAll(apps)

            runOnUiThread {
                adapter.updateList(apps)
            }
        }.start()
    }

    private fun filterApps(query: String) {
        val filtered = if (query.isEmpty()) {
            allApps
        } else {
            allApps.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateList(filtered)
    }

    private fun onAppSelected(appInfo: AppInfo) {
        // 選択を保存
        TransparentWidgetProvider.savePackageName(this, appWidgetId, appInfo.packageName)

        // ウィジェットを更新
        val appWidgetManager = AppWidgetManager.getInstance(this)
        TransparentWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId)

        // 成功結果を返す
        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(RESULT_OK, resultValue)
        finish()
    }
}

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Drawable
)

class AppListAdapter(
    private var apps: List<AppInfo>,
    private val onItemClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<AppListAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.app_icon)
        val name: TextView = view.findViewById(R.id.app_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.itemView.setOnClickListener { onItemClick(app) }
    }

    override fun getItemCount() = apps.size

    fun updateList(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}
