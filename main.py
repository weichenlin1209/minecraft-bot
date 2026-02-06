import os
import asyncio
import logging
from fastapi import FastAPI, HTTPException, Security, Depends
from fastapi.security.api_key import APIKeyHeader
from pydantic import BaseModel
import ollama
from dotenv import load_dotenv

# 讀取 .env 檔案
load_dotenv()

app = FastAPI()

# --- 安全性配置 ---
# 從環境變數讀取 TOKEN，如果沒設定，預設為 windson_fallback_key (建議一定要在 .env 設定)
API_TOKEN = os.getenv('MY_SERVICE_TOKEN')
API_TOKEN_NAME = "X-API-TOKEN"
api_key_header = APIKeyHeader(name=API_TOKEN_NAME, auto_error=False)

async def verify_token(header_value: str = Security(api_key_header)):
    """驗證 Header 中的 Token 是否正確"""
    if header_value == API_TOKEN:
        return header_value
    raise HTTPException(
        status_code=403, 
        detail="Forbidden: Invalid API Token. Please provide a valid X-API-TOKEN header."
    )

# --- 模型設定與記憶 ---
model_id = "llama3.2:3b"
system_prompt = os.getenv('system_prompt', "You are a helpful assistant.")
# 注意：這是在伺服器生命週期內共享的記憶
memory = [{"role": "system", "content": system_prompt}]

class PromptRequest(BaseModel):
    prompt: str

async def get_ollama_response(user_prompt: str) -> str:
    """共用的 Ollama 呼叫邏輯"""
    memory.append({"role": "user", "content": user_prompt})
    try:
        response = await asyncio.to_thread(
            ollama.chat,
            model=model_id,
            messages=memory,
            options={
                "temperature": 0.7,
                "top_p": 0.9,
                "repeat_penalty": 1.2,
            }
        )
        reply = response.message.content
        memory.append({"role": "assistant", "content": reply})
        return reply
    except Exception as e:
        logging.error(f"Ollama error: {e}")
        raise e

# --- FastAPI Endpoints ---

@app.get("/")
def index():
    return {"status": "online", "message": "Secure API is running"}

@app.post("/chat")
async def chat_endpoint(
    request: PromptRequest, 
    token: str = Depends(verify_token) # 強制檢查 Token
):
    try:
        # 設定 50 秒超時，防止 Cloudflared 因為後端處理太久而斷開
        answer = await asyncio.wait_for(get_ollama_response(request.prompt), timeout=50)
        return {"answer": answer, "model": model_id}
    except asyncio.TimeoutError:
        raise HTTPException(status_code=504, detail="Response timeout from Ollama")
    except Exception as e:
        logging.error(f"Endpoint error: {e}")
        raise HTTPException(status_code=500, detail="Internal Server Error")

if __name__ == "__main__":
    import uvicorn
    # 使用你指定的 port 4567
    uvicorn.run(app, host="127.0.0.1", port=4567)