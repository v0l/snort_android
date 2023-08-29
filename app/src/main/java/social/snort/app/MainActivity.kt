package social.snort.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.PermissionRequest
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat


class MainActivity : ComponentActivity() {
    private var getContentCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        webView.settings.databaseEnabled = true;
        val getContent =
            registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
                if (uri != null) {
                    getContentCallback!!.onReceiveValue(arrayOf(uri))
                } else {
                    getContentCallback!!.onReceiveValue(emptyArray())
                }
                getContentCallback = null
            }

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this)).build()
        webView.webViewClient = LocalContentWebViewClient(assetLoader)
        webView.addJavascriptInterface(Nip7Extension(), "nostr_os");
        webView.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }

            override fun onShowFileChooser(
                vw: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                getContentCallback = filePathCallback
                getContent.launch("*/*")
                return true
            }
        }
        webView.loadUrl("https://appassets.androidplatform.net/")
    }
}

private class LocalContentWebViewClient(private val assetLoader: WebViewAssetLoader) :
    WebViewClientCompat() {
    @RequiresApi(21)
    override fun shouldInterceptRequest(
        view: WebView, request: WebResourceRequest
    ): WebResourceResponse? {
        // rewrite root url to index.html
        if (request.url.path.equals("/") && request.url.host.equals("appassets.androidplatform.net")) {
            return assetLoader.shouldInterceptRequest(Uri.parse("https://appassets.androidplatform.net/index.html"))
        }
        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if (request.url.host.equals("appassets.androidplatform.net")) {
            return false
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, request.url)
            view.context.startActivity(intent)
        } catch (ex: Exception) {
            // ignored
        }
        return true
    }
}