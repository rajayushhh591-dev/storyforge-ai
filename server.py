
import os
import requests
from flask import Flask, request, jsonify, send_from_directory

app = Flask(__name__)

GEMINI_MODEL = os.environ.get(
    "GEMINI_MODEL", "gemini-3.8-flash"
)


@app.route("/")
def home():
    return send_from_directory(".", "index.html")


@app.route("/api/chat", methods=["POST"])
def chat():
    key = os.environ.get("GEMINI_API_KEY")

    if not key:
        return jsonify({
            "error": "GEMINI_API_KEY server secret configure nahi hai."
        }), 500

    data = request.get_json(silent=True) or {}
    messages = data.get("messages", [])
    instructions = data.get("instructions", "")

    if not messages:
        return jsonify({"error": "Pehle story idea likho."}), 400

    contents = []

    for message in messages:
        role = message.get("role", "user")
        content = message.get("content", "")

        if not content:
            continue

        contents.append({
            "role": "model" if role == "assistant" else "user",
            "parts": [{"text": str(content)}]
        })

    if not contents:
        return jsonify({"error": "Story idea khaali hai."}), 400

    payload = {
        "system_instruction": {
            "parts": [{
                "text": (
                    "You are StoryForge AI, an expert creative "
                    "story writer. Write engaging, detailed stories "
                    "and maintain continuity. Follow these instructions: "
                    + instructions
                )
            }]
        },
        "contents": contents,
        "generationConfig": {
            "temperature": 0.85,
            "maxOutputTokens": 8192
        }
    }

    url = (
        "https://generativelanguage.googleapis.com/v1beta/models/"
        + GEMINI_MODEL + ":generateContent"
    )

    try:
        response = requests.post(
            url,
            headers={
                "x-goog-api-key": key,
                "Content-Type": "application/json"
            },
            json=payload,
            timeout=120
        )

        result = response.json()

        if response.status_code != 200:
            error = result.get("error", {})
            message = error.get(
                "message", "Gemini request failed."
            ) if isinstance(error, dict) else "Gemini request failed."

            return jsonify({"error": message}), 502

        candidates = result.get("candidates", [])

        if not candidates:
            return jsonify({
                "error": "Gemini ne story response nahi diya."
            }), 502

        parts = candidates[0].get("content", {}).get("parts", [])
        reply = "\n".join(
            part.get("text", "")
            for part in parts
            if part.get("text")
        )

        if not reply:
            return jsonify({
                "error": "Gemini ka response khaali tha."
            }), 502

        return jsonify({"reply": reply})

    except requests.RequestException:
        app.logger.exception("Gemini connection failed")
        return jsonify({
            "error": "Gemini server se connection nahi ho paya."
        }), 502


if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=int(os.environ.get("PORT", 10000))
    )
