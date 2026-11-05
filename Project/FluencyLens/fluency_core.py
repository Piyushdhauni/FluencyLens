import whisper

whisper_model = whisper.load_model("small")

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

    result = whisper_model.transcribe(path, language = "en", task="trancribe", word_timestamps = True,
     initial_prompt = "um , let me think , like hmm... Okay, so, uh, here's what I think. ",
     temperature = 0, condition_on_previous_text=False
     )
    
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
    word_count = len(all_words)
    wpm =  round((word_count / total_time) * 60)
    text_report = analyze_text(result["text"])

    return {
        "file" : path ,
        "wpm" : wpm ,
        "word_count" : word_count,
        "duration" : round(total_time, 2),
        "slow_words" : len(slow_words),
        "fillers" : text_report["total_fillers"],
        "filler_percent" : text_report["filler_percent"],
        "transcript" : result["text"],
    }
