package social.snort.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val webView = WebView(this)
        setContentView(webView)

        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()
        webView.webViewClient = LocalContentWebViewClient(assetLoader)
        webView.loadUrl("https://appassets.androidplatform.net/")
    }
}

private class LocalContentWebViewClient(private val assetLoader: WebViewAssetLoader) :
    WebViewClientCompat() {
    @RequiresApi(21)
    override fun shouldInterceptRequest(
        view: WebView,
        request: WebResourceRequest
    ): WebResourceResponse? {
        // rewrite root url to index.html
        if (request.url.path.equals("/") && request.url.host.equals("appassets.androidplatform.net")) {
            return assetLoader.shouldInterceptRequest(Uri.parse("https://appassets.androidplatform.net/index.html"))
        }
        return assetLoader.shouldInterceptRequest(request.url)
    }

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        if(request.url.host.equals("appassets.androidplatform.net")) {
            return false;
        }
        val intent = Intent(Intent.ACTION_VIEW, request.url)
        view.context.startActivity(intent)
        return true
    }
}