import os
import asyncio
import logging
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import ollama
from dotenv import load_dotenv

load_dotenv()

app = FastAPI()

# 設定與記憶
model_id = "llama3.2:3b"
system_prompt = os.getenv('system_prompt')
memory = [{"role": "system", "content": system_prompt}]

# 定義 API 請求格式
class PromptRequest(BaseModel):
    prompt: str

async def get_ollama_response(user_prompt: str) -> str:
    """共用的 Ollama 呼叫邏輯"""
    memory.append({"role": "user", "content": user_prompt})
    try:
        # 使用 asyncio.to_thread 避免阻礙 FastAPI 事件循環
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
    return {"status": "online", "model": model_id}

@app.post("/chat")
async def chat_endpoint(request: PromptRequest):
    try:
        # 設定 50 秒超時，避免 cloudflared 等太久斷線
        answer = await asyncio.wait_for(get_ollama_response(request.prompt), timeout=50)
        return {"answer": answer, "model": model_id}
    except asyncio.TimeoutError:
        throw HTTPException(status_code=504, detail="Response timeout")
    except Exception as e:
        throw HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    # 執行在 8000 port
    uvicorn.run(app, host="0.0.0.0", port=8000)