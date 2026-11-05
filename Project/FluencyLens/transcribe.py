import csv
import os 

import whisper

model = whisper.load_model("base")

def analyze_text(transcript):
    words = [w.strip(".,!?").lower() for w in transcript.split()]
    
    if len(words) == 0:
        return {"total_words": 0, "total_fillers": 0, "filler_percent": 0}
    
    fillers = ["um", "uh", "hmm", "like"]
    count = 0
    found = []

    for word in words:
        if word in fillers:
            count = count + 1
            found.append(word)

    return {
        "total_words": len(words),
        "total_fillers": count,
        "filler_percent": round(count / len(words)* 100,1),
        "fillers_found": found ,
    }

def extract_features(path):

    result = model.transcribe(path, language = "en", word_timestamps = True, initial_prompt = "um , let me think , like hmm... Okay, so, uh, here's what I think. ")

    slow_words = []
    all_words = []

    for segment in result["segments"]:
        for w in segment["words"]:
            all_words.append(w)

            clean_word = w["word"].strip().lower().strip(".,!?")
            duration = w["end"] - w["start"]

            if duration > 0.8:
                slow_words.append(clean_word)

    if len(all_words) == 0:
                return None

    total_time = all_words[-1]["end"] - all_words[0]["start"]
    wpm = len(all_words) / total_time * 60
    text_report = analyze_text(result["text"])

    return {
        "file" : path ,
        "wpm" : round(wpm) ,
        "slow_words" : len(slow_words),
        "fillers" : text_report["total_fillers"],
        "filler_percent" : text_report["filler_percent"]
    }

clips = [
    "Clips/Voice1.wav",
    "Clips/Voice2.wav",
    "Clips/Voice3.wav",
    "Clips/Voice4.wav",
    "Clips/Voice5.wav",

    "Clips/Voice6.wav",
    "Clips/Voice7.wav",
    "Clips/Voice8.wav",
    "Clips/Voice9.wav",
    "Clips/Voice10.wav",
    "Clips/Voice11.wav",
    "Clips/Voice12.wav",
    "Clips/Voice13.wav",
    "Clips/Voice14.wav",
    "Clips/Voice15.wav",
    "Clips/Voice16.wav",
    "Clips/Voice17.wav",
    "Clips/Voice18.wav",
    "Clips/Voice19.wav"
]

rows = []

# Label Rule : hesitant = 3 or more fillers OR Long stall/restart remember //
    # fluent = 0-2 smooth and fluent

labels = {
            "Clips/Voice1.wav": "hesitant",
            "Clips/Voice2.wav": "hesitant",
            "Clips/Voice3.wav": "hesitant",
            "Clips/Voice4.wav": "hesitant",
            "Clips/Voice5.wav": "hesitant",

            "Clips/Voice6.wav": "fluent",
            "Clips/Voice7.wav": "hesitant",
            "Clips/Voice8.wav": "fluent",
            "Clips/Voice9.wav": "hesitant",
            "Clips/Voice10.wav": "fluent",
            "Clips/Voice11.wav": "hesitant",
            "Clips/Voice12.wav": "fluent",
            "Clips/Voice13.wav": "hesitant",
            "Clips/Voice14.wav": "fluent",
            "Clips/Voice15.wav": "fluent",
            "Clips/Voice16.wav": "fluent",
            "Clips/Voice17.wav": "hesitant",
            "Clips/Voice18.wav": "hesitant",
            "Clips/Voice19.wav": "hesitant",
        }

for clip in clips :
    if not os.path.exists(clip):
        print("File missing !, Skipping :", clip)
        continue
    print("Working on !", clip , "...")
        
    row = extract_features(clip)
    
    if row is not None:
        row["label"] = labels[clip]
        rows.append(row)

with open ("dataset.csv", "w", newline = "" ) as f:
    writer = csv.DictWriter(f, fieldnames=rows[0].keys())
    writer.writeheader()
    writer.writerows(rows)

print("Done, Wrote", len(rows), "rows to dataset.csv")