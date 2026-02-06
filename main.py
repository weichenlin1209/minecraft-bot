import os
import asyncio
import logging

import discord
from dotenv import load_dotenv
import ollama

load_dotenv()
discord_token = os.getenv('DISCORD_TOKEN')

system_prompt = os.getenv('system_prompt')
model_id = "llama3.2:3b"  # You can change this to your desired model ID
memory = [{"role": "system", "content": system_prompt}]

# bot = discord.Client(intents=discord.Intents.default())
bot = discord.Bot(intents=discord.Intents.all())

@bot.event
async def on_ready():
    print(f' {bot.user} is online!')

@bot.event
async def on_message(message):
    if message.author == bot.user:
        return

    if bot.user.mentioned_in(message):
        prompt = message.content.replace(f'<@{bot.user.id}>', '').strip()

        if not prompt:
            await message.reply("Please provide a question or prompt after mentioning me.")
            return

        thinking_message = await message.reply("Thinking...")

        try:
            answer = await asyncio.wait_for(generate_reply(prompt), timeout=50)  # Set a timeout for response generation
        except Exception as e:
            answer = "Wait a moment, I can't answer right now."  # Fallback message if the response generation fails or times out 
            logging.error(f"Error generating response: {e}") # Log the error for debugging 

        await thinking_message.edit(content=answer)  # Edit the thinking message with the answer    

@bot.event
async def generate_reply(user_prompt: str) -> str:
    memory.append({"role": "user", "content": user_prompt})

    try:
        response = await asyncio.to_thread(
            ollama.chat, 
            model=model_id,
            messages=memory,
            options={
                "temperature": 0.7,
                "top_p": 0.9,
                "top_k": 40,
                "repeat_penalty": 1.2,
                "presence_penalty": 0.5,
            }
            '''messages=[
                {"role": "system", "content": system_prompt}, 
                {"role": "user", "content": user_prompt}
            ],'''
        )
        
        reply = response.message.content

        memory.append({"role": "assistant", "content": reply})

        return f"{reply}\n\nby {model_id}"

    except Exception as e:
        logging.error(f"Ollama error: {e}")
        return "Sorry, I couldn't generate a response at this time."

if __name__ == "__main__":
    bot.run(discord_token)
