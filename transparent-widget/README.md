# TransparentWidget

ホーム画面に透明な1×1ウィジェットを配置し、任意のアプリを起動できるAndroidアプリです。

## 使い方

### ビルド方法（GitHub Actions）

1. このリポジトリをGitHubにpushする
2. Actions タブを開く
3. 「Build APK」が自動で実行される（約3〜5分）
4. 完了後「Artifacts」から `transparent-widget-apk.zip` をダウンロード
5. zip解凍 → `app-debug.apk` をAndroidに転送してインストール

> ⚠️ インストール時は「提供元不明のアプリ」を許可する必要があります

---

### ウィジェット追加手順

1. ホーム画面を長押し → 「ウィジェット」を選択
2. 一覧から「TransparentWidget」を探してホーム画面にドラッグ
3. アプリ選択画面が表示される
4. 起動させたいアプリを選択 → 透明ウィジェットが配置される

### 再設定（アプリ変更）

- ウィジェットを長押し → 「ウィジェットの設定」または「編集」をタップ
- アプリ選択画面が再表示される

---

## 構成

```
app/src/main/java/.../
├── TransparentWidgetProvider.kt  ← ウィジェット本体・タップ処理
└── ConfigActivity.kt             ← アプリ選択UI（検索付き）
```
