package com.example.myapplication

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import org.json.JSONObject
import org.json.JSONTokener

/** PP-158B.2: read-only displayed-order extraction feasibility test. */
class ColesCaptureFeasibilityActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var status: TextView
    private val startUrl = "https://www.coles.com.au/account/orders"
    private val logTag = "PPColesAuth"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        status = TextView(this).apply {
            text = "Open a completed order, then tap Check. Candidate labels are approximate; no order data is saved."
            setTextColor(Color.BLACK)
            textSize = 13f
            setPadding(16, 12, 16, 12)
        }
        val check = Button(this).apply { text = "Check product labels (read only)" }
        val reset = Button(this).apply { text = "Reset login (only if needed)" }
        val orders = Button(this).apply { text = "Go to Coles Orders" }
        browser = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    // Auth may legitimately redirect to a different HTTPS host.
                    // Never intercept credentials or open non-HTTPS schemes in WebView.
                    if (uri.scheme != "https") {
                        Log.d(logTag, "Blocked non-HTTPS navigation")
                        return true
                    }
                    Log.d(logTag, "Navigation host: ${uri.host.orEmpty()}")
                    return false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    Log.d(logTag, "Finished host: ${Uri.parse(url.orEmpty()).host.orEmpty()}")
                    super.onPageFinished(view, url)
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        val code = error?.errorCode
                        Log.w(logTag, "Main-frame error code=$code host=${request.url.host.orEmpty()}")
                        status.text = "Page loading error ($code). Check Android Studio Logcat: $logTag."
                    }
                    super.onReceivedError(view, request, error)
                }
            }
            loadUrl(startUrl)
        }
        layout.addView(status)
        layout.addView(check)
        layout.addView(orders)
        layout.addView(reset)
        layout.addView(browser, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        setContentView(layout)

        orders.setOnClickListener { browser.loadUrl(startUrl) }
        check.setOnClickListener {
            val uri = Uri.parse(browser.url.orEmpty())
            val host = uri.host.orEmpty()
            if (uri.scheme != "https" || (host != "coles.com.au" && !host.endsWith(".coles.com.au"))) {
                status.text = "Finish authentication and return to a Coles order page before checking."
                return@setOnClickListener
            }
            // Inspect only the currently rendered DOM; no network requests, cookies,
            // credentials, HTML or product data are stored or sent anywhere.
            // Coles may virtualise product rows, so this is NOT a complete-order count.
            val js = """(function(){
              var body=document.body;
              if(!body) return JSON.stringify({error:'Page body unavailable'});
              var all=body.innerText||'';
              var lines=all.split(/\n/).map(function(s){return s.trim()}).filter(Boolean);
              var results=[];
              var seen={};
              for(var i=0;i<lines.length;i++){
                var label=lines[i];
                if(label.length<7 || label.length>150)continue;
                if(/^(quantity|weight|special|orders|search|account|add to|view|total|price|subtotal|delivery|checkout|shop|col es)/i.test(label))continue;
                var tail=lines.slice(i+1,i+5).join(' ');
                var match=tail.match(/(?:Quantity\s*:\s*([\d.]+)|Weight\s*:\s*([\d.]+))/i);
                if(!match)continue;
                // The nearest preceding label is a candidate, not a verified product.
                // Keep price unknown unless confidently tied to the row.
                var key=label.toLowerCase();
                if(seen[key])continue;
                seen[key]=true;
                results.push({name:label,quantity:match[1]||null,weight:match[2]||null});
                if(results.length>=60)break;
              }
              return JSON.stringify({characters:all.length,images:document.images.length,
                candidates:results,visibleTextLines:lines.length});
            })()"""
            browser.evaluateJavascript(js) { encoded ->
                try {
                    val decoded = JSONTokener(encoded).nextValue() as String
                    val data = JSONObject(decoded)
                    if (data.has("error")) {
                        status.text = data.optString("error")
                    } else {
                        val candidates = data.getJSONArray("candidates")
                        val summary = StringBuilder().apply {
                            append("Read-only DOM test: ${data.optInt("characters")} characters, ")
                            append("${data.optInt("images")} images; ")
                            append("${candidates.length()} candidate product labels. ")
                            append("May be incomplete or include false matches.\n")
                            for (i in 0 until minOf(candidates.length(), 8)) {
                                val item = candidates.getJSONObject(i)
                                append("\n${i + 1}. ${item.optString("name")}")
                                if (!item.isNull("quantity")) append(" | Qty ${item.optString("quantity")}")
                                if (!item.isNull("weight")) append(" | Weight ${item.optString("weight")}")
                            }
                            if (candidates.length() > 8) append("\n…more candidates not shown")
                            append("\n\nPREVIEW ONLY. No data saved or imported.")
                        }
                        status.text = summary.toString()
                    }
                } catch (e: Exception) {
                    Log.w(logTag, "Page metadata inspection failed", e)
                    status.text = "Page inspection unavailable. No import performed."
                }
            }
        }
        reset.setOnClickListener {
            browser.stopLoading()
            browser.clearHistory()
            browser.clearCache(true)
            browser.clearFormData()
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                status.text = "Embedded-browser cookies cleared. Retrying Coles..."
                browser.loadUrl(startUrl)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (::browser.isInitialized && browser.canGoBack()) browser.goBack() else super.onBackPressed()
    }

    override fun onDestroy() {
        if (::browser.isInitialized) browser.destroy()
        super.onDestroy()
    }
}
