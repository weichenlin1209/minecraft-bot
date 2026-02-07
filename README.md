# AI ChatBot - Minecraft Fabric Mod

在 Minecraft 聊天室中與 AI 對話，AI 能回答問題也能直接執行遊戲指令。
後端使用 FastAPI + Ollama，搭配 RAG（檢索增強生成）讓 AI 參考 Minecraft 知識庫回答。

## 功能

- 聊天輸入 `!ai <訊息>` 與 AI 對話
- AI 自動判斷並執行 Minecraft 指令（`/give`、`/tp`、`/gamemode`、`/gamerule` 等）
- 按 `K` 鍵快速開啟 AI 聊天框（客戶端選裝）
- RAG 知識庫：內建生態系、指令、附魔、食物、遊戲規則、村民交易、伺服器設定等文件
- 支援自訂文件擴充知識庫（txt / pdf / docx）

## 架構

```
玩家 ── !ai 給我鑽石 ──▶  Minecraft Server (Fabric Mod)
                                │
                          HTTP POST /chat
                           X-API-TOKEN 驗證
                                ▼
                        FastAPI Backend (Python)
                          │            │
                       Ollama        FAISS
                     (LLM 推理)   (RAG 向量搜尋)
                          │            │
                          ▼            ▼
                       組合 context + prompt → 生成回覆
                                │
                                ▼
                     ┌─ 回覆以 / 開頭 → 以 level 4 權限執行指令
                     └─ 一般文字 → 廣播給所有玩家
```

## 環境需求

### Minecraft Mod

| 項目 | 版本 |
|------|------|
| Minecraft | 1.20.1（Java 版） |
| Fabric Loader | >= 0.15.0 |
| Fabric API | 0.92.7+1.20.1 |
| Java | 17 |
| Gradle | 8.8（Wrapper 自動下載） |

### Backend

| 項目 | 版本 |
|------|------|
| Python | >= 3.13 |
| Ollama | 本地安裝 |
| 預設模型 | `llama3.2:3b` |

## 專案結構

```
CTF/
├── build.gradle                          # Gradle 建構設定（Fabric Loom 1.6）
├── gradle.properties                     # 版本號設定
├── settings.gradle                       # 專案名稱
├── gradlew.bat                           # Gradle Wrapper
├── setup.bat                             # 環境檢查腳本
├── .env.example                          # Mod 環境變數範本
│
├── backend/                              # Python 後端
│   ├── main.py                           # FastAPI 伺服器（port 4567）
│   ├── embedding.py                      # 嵌入模型（embeddinggemma-300m）
│   ├── rag_builder.py                    # FAISS 向量資料庫建構
│   ├── dcbot.py                          # Discord Bot（選用）
│   ├── pyproject.toml                    # Python 依賴
│   ├── .env.example                      # 後端環境變數範本
│   ├── loaded_docs/                      # RAG 知識庫文件
│   │   ├── biomes.txt                    #   生態系
│   │   ├── commands.txt                  #   指令參考
│   │   ├── enchantments.txt              #   附魔
│   │   ├── food_stats.txt                #   食物數值
│   │   ├── game_rules.txt                #   遊戲規則
│   │   ├── server_properties.txt         #   伺服器設定
│   │   └── villager_trading.txt          #   村民交易
│   └── faiss_db/                         # 向量資料庫（建構後生成）
│
├── src/main/
│   ├── java/com/aichatbot/
│   │   ├── AIChatBotMod.java             # 伺服端：聊天監聽 + 指令執行
│   │   ├── AIChatBotClientMod.java       # 客戶端：K 鍵快捷鍵
│   │   ├── AIChatBot.java               # API 串接（HTTP POST）
│   │   └── ModConfig.java               # 設定檔 + .env 讀取
│   └── resources/
│       └── fabric.mod.json              # Fabric 模組描述
│
└── gradle/wrapper/
    ├── gradle-wrapper.jar
    └── gradle-wrapper.properties
```

## 快速開始

### 1. 啟動後端

```bash
# 安裝 Ollama 並拉取模型
ollama pull llama3.2:3b

# 進入後端目錄
cd backend

# 複製環境變數範本
cp .env.example .env
# 編輯 .env，填入 MY_SERVICE_TOKEN 和 HUGGING_FACE_TOKEN

# 安裝依賴並啟動
uv sync
uv run python main.py
```

後端啟動於 `http://localhost:4567`。

### 2. 建構 RAG 向量資料庫

專案已內建 Minecraft 知識庫文件（`loaded_docs/`）。首次使用或更新文件後執行：

```bash
cd backend
uv run python rag_builder.py
```

也可以自行放入 `.txt`、`.pdf`、`.docx` 文件到 `loaded_docs/` 擴充知識庫。

文件會以 300 字元為單位分段，50 字元重疊，使用 `embeddinggemma-300m` 模型建立向量索引。

### 3. 編譯 Minecraft Mod

```cmd
set JAVA_HOME=C:\Program Files\Java\jdk-17
gradlew.bat clean build
```

首次編譯會自動下載 Gradle、Minecraft 原始碼、Fabric API 等依賴。

產出：
```
build/libs/aichatbot-1.0.0.jar          # 模組 JAR
build/libs/aichatbot-1.0.0-sources.jar  # 原始碼 JAR
```

### 4. 安裝到 Minecraft

**伺服器端（必裝）**

1. 安裝 [Fabric Loader](https://fabricmc.net/use/installer/) for 1.20.1
2. 下載 [Fabric API](https://modrinth.com/mod/fabric-api) 放入 `mods/`
3. 將 `aichatbot-1.0.0.jar` 放入 `mods/`
4. 啟動伺服器 → 自動產生 `config/aichatbot.json` 和 `config/aichatbot.env`
5. 在 `aichatbot.env` 填入 API Token，重啟伺服器

**客戶端（選裝）**

不裝也能用（手動輸入 `!ai` 即可），裝了多一個 `K` 鍵快捷功能。

## 設定檔

### config/aichatbot.json

首次啟動自動產生：

```json
{
  "apiUrl": "http://localhost:4567/chat",
  "apiToken": "YOUR_API_TOKEN",
  "apiKey": "",
  "model": "",
  "systemPrompt": "你是 Minecraft 伺服器的 AI 助手...",
  "prefix": "!ai",
  "temperature": 0.7
}
```

| 欄位 | 說明 | 預設值 |
|------|------|--------|
| `apiUrl` | 後端 API 端點 | `http://localhost:4567/chat` |
| `apiToken` | FastAPI Token（`X-API-TOKEN` header） | `YOUR_API_TOKEN` |
| `apiKey` | OpenAI 相容 API Key（`Bearer` header） | 空 |
| `model` | 模型名稱 | 空（由後端決定） |
| `systemPrompt` | 系統提示詞，定義 AI 行為與指令格式 | Minecraft 助手 |
| `prefix` | 聊天觸發前綴 | `!ai` |
| `temperature` | 回覆隨機程度 0.0 ~ 1.0 | `0.7` |

### config/aichatbot.env

環境變數可覆蓋 JSON 設定，適合存放敏感資料：

| 環境變數 | 覆蓋欄位 |
|----------|----------|
| `AICHATBOT_API_TOKEN` | `apiToken` |
| `AICHATBOT_API_KEY` | `apiKey` |
| `AICHATBOT_API_URL` | `apiUrl` |
| `AICHATBOT_MODEL` | `model` |
| `AICHATBOT_PREFIX` | `prefix` |
| `AICHATBOT_TEMPERATURE` | `temperature` |

### backend/.env

| 環境變數 | 說明 |
|----------|------|
| `MY_SERVICE_TOKEN` | FastAPI 認證 Token（需與 Mod 的 `apiToken` 一致） |
| `HUGGING_FACE_TOKEN` | HuggingFace Token（建構向量資料庫時需要） |

## 使用方式

### 一般聊天

```
!ai 怎麼合成工作台？
!ai 附近哪裡有村莊？
!ai keepInventory 怎麼開？
```

AI 回覆顯示為 `[AI] 回覆內容`。

### 執行指令

```
!ai 幫我切換成創造模式
!ai 給我 64 顆鑽石
!ai 把天氣改成晴天
!ai 把時間設為白天
```

AI 回覆以 `/` 開頭時，Mod 自動以 **level 4 伺服器權限**執行，顯示 `[AI] 執行指令: /...`。

### 快捷鍵

| 按鍵 | 功能 |
|------|------|
| `K` | 開啟聊天框，自動帶入 `!ai ` 前綴（需安裝客戶端 Mod） |

## 運作流程

```
玩家輸入 "!ai 給我一把鑽石劍"
  │
  ├─ AIChatBotMod 攔截 CHAT_MESSAGE 事件
  ├─ 偵測到 !ai 前綴 → 擷取提問內容
  ├─ 廣播 "[AI] 正在思考..."
  ├─ 開啟新線程 → AIChatBot 發送 HTTP POST 到後端
  │
  │  後端處理：
  │  ├─ 驗證 X-API-TOKEN
  │  ├─ FAISS 搜尋 top-4 相關文件片段（RAG）
  │  ├─ 組合 RAG context + system prompt + 使用者訊息
  │  ├─ 呼叫 Ollama（llama3.2:3b）生成回覆
  │  └─ 回傳 {"answer": "..."}
  │
  ├─ 回覆以 / 開頭
  │   → 回到主線程，CommandDispatcher 以 level 4 執行
  │   → 廣播 "[AI] 執行指令: /give ..."
  │
  └─ 一般文字
      → 廣播 "[AI] 回覆內容..."
```

## 內建 RAG 知識庫

`backend/loaded_docs/` 已包含以下 Minecraft 1.20.1 Java 版參考文件：

| 文件 | 內容 |
|------|------|
| `commands.txt` | 完整指令參考（目標選擇器、座標系統、NBT 語法） |
| `game_rules.txt` | 所有 `/gamerule` 規則說明 |
| `biomes.txt` | 生態系資訊 |
| `enchantments.txt` | 附魔資料 |
| `food_stats.txt` | 食物與飢餓機制 |
| `villager_trading.txt` | 村民交易機制 |
| `server_properties.txt` | 伺服器設定項目 |

## 已知限制

- **指令權限**：AI 執行的指令擁有最高權限（level 4），需注意 system prompt 的安全性
- **共享記憶**：後端的對話記憶在所有玩家間共享，重啟後端清除
- **回應長度**：system prompt 限制回覆 50 字以內
- **超時設定**：Mod 端連線 10 秒 / 讀取 60 秒，後端 Ollama 50 秒
- **Ollama 需持續運行**：後端依賴本地 Ollama 服務

## 原始碼說明

### Minecraft Mod（Java）

| 檔案 | 說明 |
|------|------|
| `AIChatBotMod.java` | 伺服端入口。監聽 `ServerMessageEvents.CHAT_MESSAGE`，偵測前綴後開新線程呼叫 API。回覆以 `/` 開頭時透過 `CommandDispatcher` 以 level 4 權限執行 |
| `AIChatBotClientMod.java` | 客戶端入口。註冊 `K` 鍵（`GLFW_KEY_K`），按下時開啟 `ChatScreen` 帶入 `!ai ` 前綴 |
| `AIChatBot.java` | API 串接核心。`HttpURLConnection` POST `{"prompt": "..."}` 到後端，支援 `X-API-TOKEN` 與 `Bearer` 兩種認證，解析回傳 JSON 的 `answer` 欄位 |
| `ModConfig.java` | 設定檔管理。Gson 讀寫 `aichatbot.json`，支援 `aichatbot.env` 環境變數覆蓋，首次啟動自動產生預設設定 |

### Backend（Python）

| 檔案 | 說明 |
|------|------|
| `main.py` | FastAPI 伺服器（port 4567）。Token 認證 → FAISS RAG 搜尋 → Ollama 生成回覆。Ollama 參數：temperature 0.7、top_p 0.9、repeat_penalty 1.2 |
| `embedding.py` | 自訂嵌入模型，包裝 Google `embeddinggemma-300m`，為文本加前綴並正規化向量 |
| `rag_builder.py` | 從 `loaded_docs/` 載入文件 → 300 字元分段（50 重疊）→ 建立 FAISS 索引存入 `faiss_db/` |
| `dcbot.py` | Discord Bot（選用）。@mention 觸發，使用同一個 Ollama 模型回覆 |

## License

MIT
