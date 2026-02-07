# AI ChatBot - Minecraft Fabric Mod

一個 Minecraft Fabric 模組，讓玩家可以在遊戲聊天室中與 AI 對話，AI 也能自動執行遊戲指令。

## 功能

- 在聊天輸入 `!ai <訊息>` 即可與 AI 對話
- AI 可以自動判斷並執行 Minecraft 指令（如 `/give`、`/tp`、`/gamemode` 等）
- 按 `K` 鍵快速開啟帶有 `!ai` 前綴的聊天框（僅客戶端）
- 支援 OpenAI、OpenRouter 等 OpenAI 相容 API
- 所有設定皆可透過 JSON 設定檔調整

## 環境需求

| 項目 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Fabric Loader | >= 0.15.0 |
| Fabric API | 0.92.7+1.20.1 |
| Java | 17 |
| Gradle | 8.8（透過 Wrapper 自動下載） |

## 專案結構

```
CTF/
├── build.gradle                          # Gradle 建構設定（Fabric Loom 1.6）
├── gradle.properties                     # Minecraft / Fabric / 模組版本號
├── settings.gradle                       # 專案名稱
├── gradlew.bat                           # Gradle Wrapper 啟動器
├── setup.bat                             # 環境檢查腳本
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── src/main/
    ├── java/com/aichatbot/
    │   ├── AIChatBotMod.java             # 伺服端入口：聊天監聽、指令執行
    │   ├── AIChatBotClientMod.java       # 客戶端入口：K 鍵快捷鍵
    │   ├── AIChatBot.java                # AI API 串接核心
    │   └── ModConfig.java                # 設定檔讀寫（Gson）
    └── resources/
        └── fabric.mod.json              # Fabric 模組描述檔
```

## 建構方式

### 1. 確認 Java 17 已安裝

```bash
java -version
```

> 注意：如果系統預設 Java 版本不是 17（例如安裝了 Java 25），需要手動指定 `JAVA_HOME`。

### 2. 編譯模組

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
gradlew.bat clean build
```

首次執行會自動下載 Gradle 8.8、Minecraft 反編譯原始碼、Fabric API 等依賴，可能需要幾分鐘。

### 3. 產出位置

```
build/libs/aichatbot-1.0.0.jar          # 模組 JAR（放入 mods/ 資料夾）
build/libs/aichatbot-1.0.0-sources.jar  # 原始碼 JAR
```

## 安裝方式

### 伺服器端（必裝）

1. 安裝 [Fabric Loader](https://fabricmc.net/use/installer/) for 1.20.1
2. 下載 [Fabric API](https://modrinth.com/mod/fabric-api) 放入 `mods/` 資料夾
3. 將 `aichatbot-1.0.0.jar` 放入伺服器的 `mods/` 資料夾
4. 啟動伺服器，設定檔會自動產生在 `config/aichatbot.json`
5. 編輯設定檔填入 API Key，重啟伺服器

### 客戶端（選裝）

客戶端不裝也能使用（手動在聊天輸入 `!ai` 即可）。
裝了會多一個按 `K` 快速開啟 AI 聊天的功能。

## 設定檔

首次啟動後自動產生於 `config/aichatbot.json`：

```json
{
  "apiUrl": "https://openrouter.ai/api/v1/chat/completions",
  "apiKey": "YOUR_API_KEY",
  "model": "arcee-ai/trinity-large-preview:free",
  "systemPrompt": "你是 Minecraft 的 AI 助手...",
  "prefix": "!ai",
  "temperature": 0.7
}
```

| 欄位 | 說明 | 預設值 |
|------|------|--------|
| `apiUrl` | API 端點 URL | OpenRouter |
| `apiKey` | API Key（必填） | `YOUR_API_KEY` |
| `model` | 模型名稱 | `arcee-ai/trinity-large-preview:free` |
| `systemPrompt` | AI 系統提示詞，定義 AI 行為 | Minecraft 助手 |
| `prefix` | 聊天觸發前綴 | `!ai` |
| `temperature` | 回覆隨機程度（0.0 ~ 1.0） | `0.7` |

### 支援的 API 供應商

| 供應商 | apiUrl | 說明 |
|--------|--------|------|
| OpenRouter | `https://openrouter.ai/api/v1/chat/completions` | 支援多種免費模型 |
| OpenAI | `https://api.openai.com/v1/chat/completions` | 需付費 API Key |

## 使用方式

### 一般聊天

```
!ai 你好
!ai 怎麼合成工作台？
!ai 跟我聊聊
```

AI 會以文字回覆，顯示為 `[AI] 回覆內容`。

### 請求執行指令

```
!ai 幫我切換成創造模式
!ai 給我一把鑽石劍
!ai 把天氣改成晴天
!ai 把時間設為白天
```

AI 會判斷是否需要執行指令。如果 AI 回覆以 `/` 開頭，模組會自動以**伺服器權限**執行該指令，並在聊天顯示 `[AI] 執行指令: /...`。

### 快捷鍵（客戶端）

| 按鍵 | 功能 |
|------|------|
| `K` | 開啟聊天框，自動帶入 `!ai ` 前綴 |

## 運作流程

```
玩家輸入 "!ai 給我一把鑽石劍"
  │
  ├─ ServerMessageEvents.CHAT_MESSAGE 事件觸發
  ├─ 檢測到 !ai 前綴 → 擷取提問內容
  ├─ 顯示 "[AI] 正在思考..."
  ├─ 開啟新線程，非同步呼叫 AI API（避免阻塞伺服器）
  │
  ├─ 回應以 / 開頭 → 回到主線程，透過 CommandDispatcher 執行指令
  │   └─ 廣播 "[AI] 執行指令: /give ..."
  │
  └─ 一般文字回應 → 廣播給所有玩家
      └─ 顯示 "[AI] 回覆內容..."
```

## 已知限制

- **指令以伺服器身分執行**：AI 執行的指令是以伺服器控制台（Console）身分執行，擁有最高權限。需注意 AI 提示詞的安全性設定。
- **需要指定玩家名稱**：某些指令（如 `/gamemode`）需要指定玩家名稱才能執行，例如 `/gamemode creative Steve`。建議在 systemPrompt 中提醒 AI 包含玩家名稱。
- **回應長度**：AI 的長篇回覆會一次顯示在聊天中，可能較難閱讀。
- **API 延遲**：回應速度取決於 API 供應商和網路狀況，通常需要 2-5 秒。

## 原始碼說明

### AIChatBotMod.java

伺服端入口點，實作 `ModInitializer`。
- 載入設定檔並初始化 `AIChatBot`
- 註冊 `ServerMessageEvents.CHAT_MESSAGE` 事件監聽玩家聊天
- 偵測 `!ai` 前綴後，在新線程中呼叫 AI API
- AI 回覆以 `/` 開頭時，回到主線程透過 `CommandDispatcher` 執行指令
- 指令執行失敗時顯示錯誤訊息

### AIChatBotClientMod.java

客戶端入口點，實作 `ClientModInitializer`。
- 註冊 `K` 鍵快捷鍵（透過 Fabric Key Binding API）
- 按下時開啟 `ChatScreen` 並自動帶入 `!ai ` 前綴
- 進入遊戲時在聊天顯示提示訊息

### AIChatBot.java

AI API 串接核心。
- 使用 `HttpURLConnection` 發送 POST 請求到 OpenAI 相容 API
- 從 `ModConfig` 讀取 API URL、Key、模型、系統提示詞等參數
- 手動解析 JSON 回應中的 `content` 欄位（不依賴外部 JSON 庫解析 API 回應）
- 連線逾時：10 秒；讀取逾時：30 秒
- 包含 JSON 字串跳脫處理（`escapeJson`）

### ModConfig.java

設定檔管理。
- 使用 Gson（Minecraft 內建）讀寫 JSON
- 設定檔路徑：`FabricLoader.getConfigDir() / aichatbot.json`
- 首次啟動時自動產生預設設定檔
- 支援 `load()` 載入和 `save()` 儲存
