import os
import requests
from flask import Flask, request, jsonify, send_from_directory

app = Flask(name)

@app.route("/")
def home():
return send_from_directory(".", "index.html")

@app.route("/api/chat", methods=["POST"])
def chat():
key = os.environ.get("OPENROUTER_API_KEY")
if not key:
return jsonify({"error": "Server API key configure nahi hui hai."}), 500

data = request.get_json(silent=True) or {}
messages = data.get("messages", [])
instructions = data.get("instructions", "")

if not messages:
    return jsonify({"error": "Pehle story idea likho."}), 400

payload = {
    "model": os.environ.get("OPENROUTER_MODEL", "openai/gpt-4o-mini"),
    "messages": [
        {"role": "system", "content":
         "You are StoryForge AI, a creative long-form story writer. "
         "Write engaging, coherent stories and maintain continuity. "
         + instructions}
    ] + messages,
    "temperature": 0.85
}

try:
    response = requests.post(
        "https://openrouter.ai/api/v1/chat/completions",
        headers={
            "Authorization": "Bearer " + key,
            "Content-Type": "application/json"
        },
        json=payload,
        timeout=120
    )
    result = response.json()

    if response.status_code != 200:
        return jsonify({"error": result.get("error", {}).get(
            "message", "AI request failed.")}), 502

    reply = result["choices"][0]["message"]["content"]
    return jsonify({"reply": reply})

except Exception:
    return jsonify({"error": "AI server se connection nahi ho paya."}), 502

if name == "main":
app.run(host="0.0.0.0", port=int(os.environ.get("PORT", 10000)))
