import sqlite3
from werkzeug.security import check_password_hash
import sqlite3
from werkzeug.security import generate_password_hash
import os 
import joblib
import pandas as pd
from dotenv import load_dotenv
from flask import Flask, request, jsonify, send_from_directory
from flask_cors import CORS
from fluency_core import extract_features

load_dotenv(override=True)

app = Flask(__name__)
CORS(app)

classifier = joblib.load("fluency_model.joblib")
FEATURES = ["wpm", "slow_words", "fillers", "filler_percent"]

def score_features(features):
    row = pd.DataFrame(
        [[features[f] for f in FEATURES]],
        columns=FEATURES
    )

    # AI classification
    label = classifier.predict(row)[0]

    # Individual metric scores
    wpm = features["wpm"]
    fillers = features["fillers"]
    slow_words = features["slow_words"]
    filler_percent = features["filler_percent"]

    # Speaking pace score (ideal range: 120–160 WPM)
    if 120 <= wpm <= 160:
        pace_score = 100
    elif wpm < 120:
        pace_score = max(0, (wpm / 120) * 100)
    else:
        pace_score = max(0, 100 - ((wpm - 160) * 2))

    # Filler score
    filler_score = max(0, 100 - (filler_percent * 5))

    # Slow word score
    slow_score = max(0, 100 - (slow_words * 5))

    # Weighted overall score
    fluency_score = round(
        (pace_score * 0.40) +
        (filler_score * 0.30) +
        (slow_score * 0.30)
    )

    return label, fluency_score


def generate_feedback(features, score):
    wpm = features["wpm"]
    fillers = features["fillers"]
    slow_words = features["slow_words"]
    filler_percent = features["filler_percent"]

    strengths = []
    improvements = []

    # Speaking pace
    if 120 <= wpm <= 160:
        strengths.append("Your speaking pace is in the target range.")
    elif wpm < 120:
        improvements.append(
            "Try speaking slightly faster. Aim for 120–160 words per minute."
        )
    else:
        improvements.append(
            "Try slowing down slightly so your words remain clear."
        )

    # Filler words
    if fillers == 0:
        strengths.append("Excellent! No filler words were detected.")
    elif filler_percent > 5:
        improvements.append(
            "Reduce filler words such as um, uh and like. "
            "Pause briefly instead of using fillers."
        )
    else:
        improvements.append(
            "Good control of filler words. Keep practising natural pauses."
        )

    # Slow words
    if slow_words > 10:
        improvements.append(
            "Work on maintaining a steady rhythm. "
            "Practise speaking complete sentences smoothly."
        )
    elif slow_words > 0:
        improvements.append(
            "A few words were slower than expected. "
            "Practise connecting words naturally."
        )
    else:
        strengths.append("Your speech had a consistent pace.")

    # Build personalized response
    if not strengths:
        strengths.append("You completed a speaking practice session.")

    if not improvements:
        improvements.append("Keep practising to maintain your speaking consistency.")

    return {
        "strengths": strengths,
        "improvements": improvements,
        "tip": improvements[0],
        "summary": (
            "Good progress! Keep practising."
            if score >= 75
            else "Keep practising. Focus on one improvement at a time."
        )
    }


history = []

def partner_reply(history):
    last = history[-1]["content"] if history else ""
    last_lower = last.lower()
    # pick a relevant follow-up question based on what they said
    if any(w in last_lower for w in ["job", "work", "company", "career", "hired", "interview", "office"]):
        prompts = ["What kind of tasks do you handle there?", "What do you enjoy about that role?"]
    elif any(w in last_lower for w in ["student", "university", "college", "study", "school"]):
        prompts = ["What subject do you find most interesting?", "What would you like to do after graduation?"]
    elif any(w in last_lower for w in ["home", "family", "parents", "siblings", "live"]):
        prompts = ["What do you like doing with them?", "Where do you live?"]
    elif any(w in last_lower for w in ["hobby", "music", "movie", "sport", "game", "travel", "book"]):
        prompts = ["Why do you like that?", "Can you tell me more about it?"]
    elif any(w in last_lower for w in ["problem", "difficult", "challenge", "hard"]):
        prompts = ["How did you solve it?", "What would you do next time?"]
    else:
        prompts = ["What else would you like to share?", "Can you tell me more about that?"]
    prompt = prompts[0] if len(prompts) > 1 and len(last) > 20 else prompts[-1]

    # short friendly correction for common mistakes
    if "dont" in last_lower:
        reply = "That is good. Quick tip: use \"don't\" with an apostrophe. "
    elif "cant" in last_lower:
        reply = "That is good. Quick tip: use \"can't\" with an apostrophe. "
    elif "i am" in last_lower:
        reply = "That is good. Quick tip: in casual speech you can also say \"I'm\". "
    else:
        reply = "That is good. "

    reply += last.strip() + ". " + prompt
    return reply[0].upper() + reply[1:]

@app.route("/")
def home():
    return "FluencyLens server is Running."

@app.route("/analyze", methods=["POST"])
def analyze():

    if "audio" not in request.files :
        return jsonify ({"error" : " NO audio file Provided"}), 400

    audio = request.files["audio"]
    temp_file = "temp_upload.wav"

    audio.save(temp_file)
    try:
        features = extract_features(temp_file)

        if features is None:
            return jsonify ({"error" : "No Speech Detected"}),400

        label, fluency_score = score_features(features)

        return jsonify ({
            "label" : label, 
            "fluency_score" : fluency_score, 
            "wpm" : features["wpm"],
            "fillers" : features["fillers"],
            "slow_words" : features["slow_words"],
            "filler_percent" : features["filler_percent"],
            "transcript": features["transcript"]
        })

    except Exception as e:
        return jsonify({"error": str(e)}), 500

    finally:
        if os.path.exists(temp_file):
            os.remove(temp_file)

@app.route("/chat", methods=["POST"])
def chat():
    if "audio" not in request.files:
        return jsonify({"error" : "No audio files is Provided"}),400

    audio = request.files["audio"]
    temp_file = "temp_upload.wav"

    audio.save(temp_file)

    try:
        features = extract_features(temp_file)
    
        if features is None:
            return jsonify ({"error" : "No Speech Detected"}),400

        history.append({
            "role" : "user",
            "content" : features["transcript"]
        })

        reply = partner_reply(history)

        history.append({
                "role" : "assistant",
                "content" : reply
        })

        label, score = score_features(features)

        feedback = generate_feedback(features, score)

        return jsonify ({
            "you_said" : features["transcript"],
            "reply" : reply,
            "fluency_score": score,
            "label" : label,
            "wpm": features["wpm"],
            "fillers": features["fillers"],
            "slow_words": features["slow_words"],
            "filler_percent": features["filler_percent"],
            "feedback": feedback,
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500

    finally:
            if os.path.exists(temp_file):
                os.remove(temp_file)

@app.route("/reset", methods=["POST"])
def reset():
    history.clear()
    return jsonify({
        "message": "Conversation history cleared successfully"
    })

@app.route("/register", methods=["POST"])
def register():
    data = request.get_json(silent=True) or {}

    name = (data.get("name") or "").strip()
    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""

    if not name or not email or not password:
        return jsonify({"error": "All fields are required"}), 400

    if len(password) < 8:
        return jsonify({"error": "Password must contain at least 8 characters"}), 400

    password_hash = generate_password_hash(password)

    try:
        with sqlite3.connect("users.db") as conn:
            conn.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL
                )
            """)

            conn.execute(
                "INSERT INTO users (name, email, password_hash) VALUES (?, ?, ?)",
                (name, email, password_hash)
            )

        return jsonify({
            "message": "Account created successfully"
        }), 201

    except sqlite3.IntegrityError:
        return jsonify({"error": "An account with this email already exists"}), 409

    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route("/login", methods=["POST"])
def login():
    data = request.get_json(silent=True) or {}

    email = (data.get("email") or "").strip().lower()
    password = data.get("password") or ""

    if not email or not password:
        return jsonify({"error": "Email and password are required"}), 400

    with sqlite3.connect("users.db") as conn:
        user = conn.execute(
            "SELECT id, name, password_hash FROM users WHERE email = ?",
            (email,)
        ).fetchone()

    if user is None or not check_password_hash(user[2], password):
        return jsonify({"error": "Invalid email or password"}), 401

    return jsonify({
        "message": "Login successful",
        "user_id": user[0],
        "name": user[1]
    }), 200

if __name__ == "__main__" :

    from flask import send_from_directory
    @app.route("/app")
    def web ():
        return send_from_directory("Web", "index.html")
    app.run(host="0.0.0.0", port = 5000 , debug= False)
