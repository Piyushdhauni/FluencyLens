transcript = "so um I really think that uh communication is like very important"

def analyze_text(transcript):
    words = transcript.lower().split()
    
    if len(words) == 0:
        return {"total_words": 0, "total_fillers": 0, "filler_percent": 0}
    
    fillers = ["um", "uh", "hmm", "like"]
    count = 0

    for word in words:
        if word in fillers:
            count = count + 1

    return {
        "total_words": len(words),
        "total_fillers": count,
        "filler_percent": round(count / len(words)* 100,1)
    }

print(analyze_text(""))

samples = [
    "so um I really think this is good",
    "communication is really important",
    "uh like um you know it is hmm hard"
]
for text in samples:
    result = analyze_text(text)
    print(text, "->", result["filler_percent"], "%")
