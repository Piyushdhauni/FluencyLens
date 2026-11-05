package com.example.fluencylens

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    // CHANGE THIS: your laptop's IPv4 address (run "ipconfig" on the laptop).
    // The phone and the laptop must be on the same Wi-Fi or hotspot.
    private val serverBase = "http://10.171.145.31:5000"

    private lateinit var recordBtn: Button
    private lateinit var endBtn: Button
    private lateinit var statusText: TextView
    private lateinit var scoreText: TextView
    private lateinit var statsText: TextView
    private lateinit var wpmValue: TextView
    private lateinit var fillersValue: TextView
    private lateinit var slowValue: TextView
    private lateinit var percentValue: TextView
    private lateinit var chatText: TextView

    private lateinit var feedbackStrengths: TextView
    private lateinit var feedbackImprovements: TextView
    private lateinit var feedbackTip: TextView

    private var recorder: MediaRecorder? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var conversationOn = false
    private lateinit var tts: TextToSpeech

    // Whisper can take a while, so the read timeout is long
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val micPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startRecording() else setStatus("Microphone permission is needed to practise.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val userName = intent.getStringExtra("USER_NAME") ?: "User"

        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        welcomeText.text = "Welcome back, $userName!"

        recordBtn = findViewById(R.id.recordBtn)
        endBtn = findViewById(R.id.endBtn)
        statusText = findViewById(R.id.statusText)
        scoreText = findViewById(R.id.scoreText)
        statsText = findViewById(R.id.statsText)

        wpmValue = findViewById(R.id.wpmValue)
        fillersValue = findViewById(R.id.fillersValue)
        slowValue = findViewById(R.id.slowValue)
        percentValue = findViewById(R.id.percentValue)

        feedbackStrengths = findViewById(R.id.feedbackStrengths)
        feedbackImprovements = findViewById(R.id.feedbackImprovements)
        feedbackTip = findViewById(R.id.feedbackTip)

        chatText = findViewById(R.id.chatText)

        // Text-to-speech: the AI partner talks. When it finishes, the mic opens again (hands-free loop).
        tts = TextToSpeech(this) { status ->





            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread { if (conversationOn) ensureMicThenRecord() }
                    }
                })
            }
        }

        recordBtn.setOnClickListener {
            conversationOn = true
            if (isRecording) stopAndSend() else ensureMicThenRecord()
        }
        endBtn.setOnClickListener { endConversation() }
    }

    /* ---------- Recording ---------- */

    private fun ensureMicThenRecord() {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) startRecording() else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startRecording() {
        tts.stop()
        val file = File(cacheDir, "recording.m4a")

        @Suppress("DEPRECATION")
        val r = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) MediaRecorder(this) else MediaRecorder()

        try {
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioSamplingRate(16000)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
        } catch (e: Exception) {
            r.release()
            showError("Could not start the microphone.")
            return
        }

        recorder = r
        audioFile = file
        isRecording = true
        recordBtn.isEnabled = true
        recordBtn.text = "Stop and send"
        setStatus("Recording. Speak, then tap Stop and send.")
    }

    private fun stopAndSend() {
        try { recorder?.stop() } catch (e: RuntimeException) { /* stopped too quickly */ }
        recorder?.release()
        recorder = null
        isRecording = false

        recordBtn.isEnabled = false
        recordBtn.text = "Analyzing..."
        setStatus("Analyzing your speech. This takes 10 to 20 seconds.")

        val file = audioFile ?: return
        sendToServer(file)
    }

    /* ---------- Upload to the Flask server ---------- */

    private fun sendToServer(file: File) {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("audio", "recording.m4a", file.asRequestBody("audio/mp4".toMediaType()))
            .build()

        val request = Request.Builder().url("$serverBase/chat").post(body).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { showError("Could not reach the server. Check the IP address and Wi-Fi.") }
            }

            override fun onResponse(call: Call, response: Response) {
                val text = response.body?.string() ?: ""
                runOnUiThread { handleResponse(text) }
            }
        })
    }


    private fun handleResponse(text: String) {
        recordBtn.isEnabled = true
        recordBtn.text = "Record"

        try {
            val json = JSONObject(text)
            if (json.has("error")) {
                showError(json.getString("error"))
                return
            }

            scoreText.text = json.getInt("fluency_score").toString()
            statsText.text = json.getString("label") + ", " +
                    json.getInt("wpm") + " wpm, " +
                    json.getInt("fillers") + " fillers, " +
                    json.getInt("slow_words") + " slow words"

            wpmValue.text = json.getInt("wpm").toString()
            fillersValue.text = json.getInt("fillers").toString()
            slowValue.text = json.getInt("slow_words").toString()

            percentValue.text = String.format(
                Locale.US,
                "%.1f%%",
                json.getDouble("filler_percent")
            )

            val reply = json.getString("reply")
            val feedback = json.getJSONObject("feedback")

            val strengths = feedback.getJSONArray("strengths")
            val improvements = feedback.getJSONArray("improvements")

            feedbackStrengths.text = (0 until strengths.length()).joinToString("\n") {
                "• " + strengths.getString(it)
            }

            feedbackImprovements.text = (0 until improvements.length()).joinToString("\n") {
                "• " + improvements.getString(it)
            }

            feedbackTip.text = feedback.getString("tip")

            findViewById<android.view.View>(R.id.feedbackCard).visibility =
                android.view.View.VISIBLE
            chatText.append("You: " + json.getString("you_said") + "\n\nFluencyLens: " + reply + "\n\n")

            if (conversationOn) {
                setStatus("The AI is replying...")
                tts.speak(reply, TextToSpeech.QUEUE_FLUSH, null, "reply")
            }
        } catch (e: Exception) {

            showError("Unexpected reply from the server.")
        }
    }

    /* ---------- End and clean up ---------- */

    private fun endConversation() {
        conversationOn = false
        tts.stop()

        if (isRecording) {
            try { recorder?.stop() } catch (e: RuntimeException) { /* ignore */ }
            recorder?.release()
            recorder = null
            isRecording = false
        }

        recordBtn.isEnabled = true
        recordBtn.text = "Record"
        setStatus("Conversation ended.")

        // Ask the server to forget the conversation so the next one starts fresh
        val reset = Request.Builder()
            .url("$serverBase/reset")
            .post("".toRequestBody(null))
            .build()
        client.newCall(reset).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    private fun setStatus(text: String) { statusText.text = text }

    private fun showError(text: String) {
        conversationOn = false
        recordBtn.isEnabled = true
        recordBtn.text = "Record"
        setStatus(text)
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder?.release()
        tts.shutdown()
    }
}
