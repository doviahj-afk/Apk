package com.sysdownloader.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.sysdownloader.app.databinding.ActivityBrowserBinding

/**
 * Minimal in-app browser so the user can find a video/page without leaving
 * the app, then hand the current URL back to MainActivity to hand off to
 * yt-dlp — the same "browse, find, grab" flow apps like Vidmate use.
 *
 * This activity does NOT scrape, auto-detect embedded media, or bypass any
 * site's login/DRM/paywall. It only reads whatever URL is currently loaded
 * in the address bar. Whether that URL can actually be downloaded — and
 * whether doing so is allowed — is entirely up to yt-dlp's site support and
 * the site's own terms. Respect copyright and platform terms of service;
 * only download content you own or are otherwise permitted to save.
 */
class BrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBrowserBinding

    companion object {
        const val EXTRA_URL = "extra_url"
        private const val DEFAULT_URL = "https://www.google.com"
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding.webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
        }

        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { binding.addressBar.setText(it) }
                binding.browserStatus.text = "loading..."
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { binding.addressBar.setText(it) }
                binding.browserStatus.text = "ready"
            }
        }

        binding.backButton.setOnClickListener {
            if (binding.webView.canGoBack()) binding.webView.goBack()
        }
        binding.forwardButton.setOnClickListener {
            if (binding.webView.canGoForward()) binding.webView.goForward()
        }
        binding.goButton.setOnClickListener { loadFromAddressBar() }
        binding.addressBar.setOnEditorActionListener { _, actionId, event ->
            val enterPressed = event != null && event.keyCode == KeyEvent.KEYCODE_ENTER
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE || enterPressed) {
                loadFromAddressBar()
                true
            } else {
                false
            }
        }

        binding.useUrlButton.setOnClickListener {
            val current = binding.webView.url ?: binding.addressBar.text.toString().trim()
            if (current.isEmpty()) {
                Toast.makeText(this, "Load a page first", Toast.LENGTH_SHORT).show()
            } else {
                val result = Intent().putExtra(EXTRA_URL, current)
                setResult(RESULT_OK, result)
                finish()
            }
        }

        val startUrl = intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotBlank() } ?: DEFAULT_URL
        binding.addressBar.setText(startUrl)
        binding.webView.loadUrl(startUrl)
    }

    private fun loadFromAddressBar() {
        var input = binding.addressBar.text.toString().trim()
        if (input.isEmpty()) return
        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            input = "https://$input"
        }
        binding.webView.loadUrl(input)
        currentFocus?.let { hideKeyboard(it) }
    }

    private fun hideKeyboard(view: android.view.View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
