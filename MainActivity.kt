package com.sysdownloader.app

import android.os.Bundle
import android.os.Environment
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sysdownloader.app.databinding.ActivityMainBinding
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var ytdlpReady = false

    private val qualityOptions = listOf("Best", "1080p", "720p", "480p")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.qualitySpinner.adapter = ArrayAdapter(
            this, android.R.layout.simple_spinner_dropdown_item, qualityOptions
        )

        log("$ initializing yt-dlp runtime...")
        initRuntime()

        binding.downloadButton.setOnClickListener { startDownload() }
    }

    private fun initRuntime() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                FFmpeg.getInstance().init(applicationContext)
                ytdlpReady = true
                withContext(Dispatchers.Main) {
                    log("$ yt-dlp runtime ready.")
                    setStatus("idle", R.color.term_yellow)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    log("! init failed: ${e.message}")
                    setStatus("init error", R.color.term_red)
                }
            }
        }
    }

    private fun startDownload() {
        val url = binding.urlInput.text.toString().trim()
        if (url.isEmpty()) {
            Toast.makeText(this, "Enter a URL first", Toast.LENGTH_SHORT).show()
            return
        }
        if (!ytdlpReady) {
            Toast.makeText(this, "yt-dlp still initializing, wait a moment", Toast.LENGTH_SHORT).show()
            return
        }

        val audioOnly = binding.radioAudio.isChecked
        val playlist = binding.playlistCheck.isChecked
        val quality = qualityOptions[binding.qualitySpinner.selectedItemPosition]

        binding.downloadButton.isEnabled = false
        binding.progressBar.progress = 0
        setStatus("starting...", R.color.term_cyan)
        log("$ downloading: $url")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val outDir = File(getExternalFilesDir(null), "downloads")
                if (!outDir.exists()) outDir.mkdirs()

                val request = YoutubeDLRequest(url)
                request.addOption("-o", outDir.absolutePath + "/%(title)s.%(ext)s")

                if (!playlist) request.addOption("--no-playlist")
                else request.addOption("--yes-playlist")

                if (audioOnly) {
                    request.addOption("-x")
                    request.addOption("--audio-format", "mp3")
                    request.addOption("--audio-quality", "0")
                } else {
                    val fmt = when (quality) {
                        "1080p" -> "bv*[height<=1080]+ba/b[height<=1080]"
                        "720p"  -> "bv*[height<=720]+ba/b[height<=720]"
                        "480p"  -> "bv*[height<=480]+ba/b[height<=480]"
                        else    -> "bv*+ba/b"
                    }
                    request.addOption("-f", fmt)
                    request.addOption("--merge-output-format", "mp4")
                }

                YoutubeDL.getInstance().execute(request) { progress, _, line ->
                    CoroutineScope(Dispatchers.Main).launch {
                        binding.progressBar.progress = progress.toInt().coerceIn(0, 100)
                        setStatus("${progress.toInt()}%", R.color.term_cyan)
                        if (line.isNotBlank()) log(line)
                    }
                }

                withContext(Dispatchers.Main) {
                    setStatus("done", R.color.term_green)
                    log("$ saved to: ${outDir.absolutePath}")
                    binding.downloadButton.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setStatus("failed", R.color.term_red)
                    log("! error: ${e.message}")
                    binding.downloadButton.isEnabled = true
                }
            }
        }
    }

    private fun log(line: String) {
        binding.logConsole.append("$line\n")
    }

    private fun setStatus(text: String, colorRes: Int) {
        binding.statusLabel.text = text
        binding.statusLabel.setTextColor(getColor(colorRes))
    }
}
