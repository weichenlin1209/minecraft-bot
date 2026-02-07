# AI ChatBot - Minecraft Fabric Mod

一個 Minecraft Fabric 模組，讓玩家可以在遊戲聊天室中與 AI 對話，AI 也能自動執行遊戲指令。
後端使用 FastAPI + Ollama，支援 RAG（檢索增強生成）讓 AI 能參考自訂文件回答問題。

## 功能

- 在聊天輸入 `!ai <訊息>` 即可與 AI 對話
- AI 可自動判斷並執行 Minecraft 指令（如 `/give`、`/tp`、`/gamemode` 等）
- 按 `K` 鍵快速開啟帶有 `!ai` 前綴的聊天框（僅客戶端）
- RAG 支援：載入自訂文件（txt / pdf / docx），AI 回答時自動參考相關內容
- 支援 Ollama 本地模型與 OpenAI 相容 API

## 架構

```
玩家 ──!ai 你好──▶ Minecraft Server (Fabric Mod)
                         │
                    HTTP POST /chat
                         ▼
                  FastAPI Backend (Python)
                    │         │
                 Ollama     FAISS
                (LLM 推理)  (RAG 向量搜尋)
                    │         │
                    ▼         ▼
                  組合 context + prompt
                         │
                         ▼
                  回傳 AI 回覆給 Minecraft
```

## 環境需求

### Minecraft Mod（前端）

| 項目 | 版本 |
|------|------|
| Minecraft | 1.20.1 |
| Fabric Loader | >= 0.15.0 |
| Fabric API | 0.92.7+1.20.1 |
| Java | 17 |
| Gradle | 8.8（透過 Wrapper 自動下載） |

### Backend（後端）

| 項目 | 版本 |
|------|------|
| Python | >= 3.13 |
| Ollama | 本地安裝並啟動 |
| 模型 | `llama3.2:3b`（可更換） |

## 專案結構

```
CTF/
├── build.gradle                          # Gradle 建構設定（Fabric Loom 1.6）
├── gradle.properties                     # Minecraft / Fabric / 模組版本號
├── settings.gradle                       # 專案名稱
├── gradlew.bat                           # Gradle Wrapper 啟動器
├── setup.bat                             # 環境檢查腳本
├── backend/                              # Python 後端
│   ├── main.py                           # FastAPI 伺服器（port 4567）
│   ├── embedding.py                      # HuggingFace 嵌入模型
│   ├── rag_builder.py                    # FAISS 向量資料庫建構工具
│   ├── dcbot.py                          # Discord Bot（選用）
│   ├── pyproject.toml                    # Python 依賴管理
│   ├── .env.example                      # 環境變數範本
│   ├── loaded_docs/                      # 放入 RAG 參考文件
│   └── faiss_db/                         # 向量資料庫（自動生成）
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── src/main/
    ├── java/com/aichatbot/
    │   ├── AIChatBotMod.java             # 伺服端入口：聊天監聽、指令執行
    │   ├── AIChatBotClientMod.java       # 客戶端入口：K 鍵快捷鍵
    │   ├── AIChatBot.java                # AI API 串接核心
    │   └── ModConfig.java                # 設定檔讀寫（Gson）+ .env 支援
    └── resources/
        └── fabric.mod.json              # Fabric 模組描述檔
```

## 快速開始

### 1. 啟動後端

```bash
# 安裝 Ollama 並拉取模型
ollama pull llama3.2:3b

# 進入後端目錄
cd backend

# 複製環境變數範本並填入
cp .env.example .env
# 編輯 .env，設定 MY_SERVICE_TOKEN 和 HUGGING_FACE_TOKEN

# 安裝依賴並啟動
uv sync
uv run python main.py
```

後端會在 `http://localhost:4567` 啟動。

### 2. 建構 RAG 向量資料庫（選用）

將文件放入 `backend/loaded_docs/`（支援 `.txt`、`.pdf`、`.docx`），然後執行：

```bash
cd backend
uv run python rag_builder.py
```

這會在 `faiss_db/` 生成向量資料庫，之後 AI 回答時會自動參考這些文件。

### 3. 編譯 Minecraft Mod

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
gradlew.bat clean build
```

首次執行會自動下載 Gradle 8.8、Minecraft 反編譯原始碼、Fabric API 等依賴，可能需要幾分鐘。

產出位置：
```
build/libs/aichatbot-1.0.0.jar          # 模組 JAR（放入 mods/ 資料夾）
build/libs/aichatbot-1.0.0-sources.jar  # 原始碼 JAR
```

### 4. 安裝到 Minecraft

**伺服器端（必裝）**

1. 安裝 [Fabric Loader](https://fabricmc.net/use/installer/) for 1.20.1
2. 下載 [Fabric API](https://modrinth.com/mod/fabric-api) 放入 `mods/` 資料夾
3. 將 `aichatbot-1.0.0.jar` 放入伺服器的 `mods/` 資料夾
4. 啟動伺服器，設定檔會自動產生在 `config/aichatbot.json`
5. 編輯設定檔或 `.env` 填入 API Token，重啟伺服器

**客戶端（選裝）**

客戶端不裝也能使用（手動在聊天輸入 `!ai` 即可）。
裝了會多一個按 `K` 快速開啟 AI 聊天的功能。

## 設定檔

首次啟動後自動產生於 `config/aichatbot.json`：

```json
{
  "apiUrl": "http://localhost:4567/chat",
  "apiToken": "YOUR_API_TOKEN",
  "apiKey": "",
  "model": "",
  "systemPrompt": "你是 Minecraft 的 AI 助手...",
  "prefix": "!ai",
  "temperature": 0.7
}
```

| 欄位 | 說明 | 預設值 |
|------|------|--------|
| `apiUrl` | API 端點 URL | `http://localhost:4567/chat` |
| `apiToken` | Ollama FastAPI Token（X-API-TOKEN） | `YOUR_API_TOKEN` |
| `apiKey` | OpenAI/OpenRouter API Key（Bearer） | 空 |
| `model` | 模型名稱 | 空（由後端決定） |
| `systemPrompt` | AI 系統提示詞 | Minecraft 助手 |
| `prefix` | 聊天觸發前綴 | `!ai` |
| `temperature` | 回覆隨機程度（0.0 ~ 1.0） | `0.7` |

### 環境變數覆蓋

可透過 `aichatbot.env` 覆蓋設定檔，避免將敏感資料寫入 JSON：

| 環境變數 | 覆蓋欄位 |
|----------|----------|
| `AICHATBOT_API_TOKEN` | `apiToken` |
| `AICHATBOT_API_KEY` | `apiKey` |
| `AICHATBOT_API_URL` | `apiUrl` |
| `AICHATBOT_MODEL` | `model` |
| `AICHATBOT_PREFIX` | `prefix` |
| `AICHATBOT_TEMPERATURE` | `temperature` |

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
```

AI 回覆以 `/` 開頭時，模組會自動以**伺服器權限（level 4）**執行該指令，並在聊天顯示 `[AI] 執行指令: /...`。

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
  ├─ 開啟新線程，POST { "prompt": "..." } 到後端
  │
  ├─ 後端收到請求
  │   ├─ FAISS 搜尋相關文件片段（RAG）
  │   ├─ 組合 context + system prompt + 使用者訊息
  │   └─ 呼叫 Ollama 生成回覆
  │
  ├─ 回應以 / 開頭 → 回到主線程，透過 CommandDispatcher 執行指令
  │   └─ 廣播 "[AI] 執行指令: /give ..."
  │
  └─ 一般文字回應 → 廣播給所有玩家
      └─ 顯示 "[AI] 回覆內容..."
```

## 已知限制

- **指令以伺服器身分執行**：AI 執行的指令擁有最高權限（level 4），需注意系統提示詞的安全性設定
- **需要指定玩家名稱**：某些指令（如 `/gamemode`）需要指定玩家名稱，建議在 systemPrompt 中提醒 AI 包含玩家名稱
- **回應長度**：AI 的長篇回覆會一次顯示在聊天中，可能較難閱讀
- **API 延遲**：回應速度取決於 Ollama 硬體效能，通常需要數秒
- **Ollama 需持續運行**：後端依賴本地 Ollama 服務，需確保 Ollama 已啟動

## 原始碼說明

### Minecraft Mod（Java）

| 檔案 | 說明 |
|------|------|
| `AIChatBotMod.java` | 伺服端入口。監聽聊天事件，偵測 `!ai` 前綴後在新線程中呼叫 API，AI 回覆以 `/` 開頭時透過 `CommandDispatcher` 執行指令 |
| `AIChatBotClientMod.java` | 客戶端入口。註冊 `K` 鍵快捷鍵，按下時開啟聊天框並帶入 `!ai ` 前綴 |
| `AIChatBot.java` | API 串接核心。使用 `HttpURLConnection` 發送 POST 請求，支援 X-API-TOKEN 與 Bearer 兩種認證方式，連線逾時 10 秒 / 讀取逾時 60 秒 |
| `ModConfig.java` | 設定檔管理。使用 Gson 讀寫 JSON，支援 `.env` 環境變數覆蓋敏感設定 |

### Backend（Python）

| 檔案 | 說明 |
|------|------|
| `main.py` | FastAPI 伺服器（port 4567）。接收 `/chat` POST 請求，透過 FAISS 搜尋相關文件後呼叫 Ollama 生成回覆 |
| `embedding.py` | 自訂嵌入模型類別，使用 Google `embeddinggemma-300m` 產生向量 |
| `rag_builder.py` | FAISS 向量資料庫建構工具。從 `loaded_docs/` 載入文件，以 300 token 為單位分段並建立索引 |
| `dcbot.py` | Discord Bot（選用），可在 Discord 上使用同一個 AI |
