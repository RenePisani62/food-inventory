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
import android.widget.ScrollView
import android.app.AlertDialog
import org.json.JSONObject
import org.json.JSONTokener

/** PP-158B.4: quantity-anchored read-only extraction diagnostic. */
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
            text = "Open a completed order, then tap Check. Quantity-anchored diagnostic; no order data is saved."
            setTextColor(Color.BLACK)
            textSize = 13f
            setPadding(16, 12, 16, 12)
        }
        val check = Button(this).apply { text = "Validate Coles order (read only)" }
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
  var root=document.body;
  if(!root)return JSON.stringify({error:'Page body unavailable'});
  var text=root.innerText||'';
  var lines=text.split(/\n/).map(function(s){return s.replace(/\s+/g,' ').trim()}).filter(Boolean);
  var qtyRe=/^(?:Quantity|Qty)\s*:\s*(\d+(?:\.\d+)?)$/i;
  var weightRe=/^Weight\s*:\s*(\d+(?:\.\d+)?)\s*(kg|g)?$/i;
  var moneyRe=/^\$\s*(\d{1,4}(?:,\d{3})*\.\d{2})$/;
  var reject=/^(items in your order|taxable item|non.taxable item|quantity|weight|special|orders|search|account|subtotal|total|delivery|checkout|shop|view order|add to cart|save|price|price per kg|unit price|each|in.store|order details|discount|savings|continue shopping|back to orders|previous orders|help|contact us|refund|payment|paid|receipt|order summary)$/i;
  function isName(s){
    return s.length>=5 && s.length<=170 && /[a-z]{2}/i.test(s) && !reject.test(s) &&
      !/^(?:Quantity|Qty|Weight)\s*:/i.test(s) && !/^\$/.test(s) &&
      !/^(?:special|was|now|save|total|subtotal|taxable|order|items in)/i.test(s) &&
      !/^(?:\d+(?:\.\d+)?|\d+\s*(?:kg|g|ml|l))$/i.test(s);
  }
  // Each complete Quantity/Weight line anchors one product. Never scan ahead
  // to borrow a quantity from the next product, as the previous test did.
  var items=[],rejected=0,unpriced=0;
  for(var i=0;i<lines.length;i++){
    var q=lines[i].match(qtyRe), w=lines[i].match(weightRe);
    if(!q&&!w)continue;
    // Find the nearest preceding product description, optionally spanning two
    // consecutive name lines. Stop at another quantity, price or section label.
    var j=i-1;
    while(j>=0 && /^(?:Special|On special|Was|Now|Save)$/i.test(lines[j]))j--;
    if(j<0 || !isName(lines[j])){rejected++;continue;}
    var name=lines[j];
    if(j>0 && isName(lines[j-1]) && !qtyRe.test(lines[j-1]) && !weightRe.test(lines[j-1]) &&
       !moneyRe.test(lines[j-1]) && lines[j-1].length<95 &&
       !/^(?:items in|taxable|your order|order details)/i.test(lines[j-1])){
       // Only join if the previous line is plausibly a wrapped description;
       // otherwise preserve the nearer line and flag for review.
       if(name.length<35 && !/\b(?:g|kg|ml|l|pack|each|per kg)\b/i.test(lines[j-1]))
          name=lines[j-1]+' '+name;
    }
    // Inspect only the section between this quantity and the next quantity,
    // bounded to a short span. Price matching remains provisional until DOM
    // row structure is independently confirmed.
    var next=i+1;
    while(next<lines.length && next<i+9 && !qtyRe.test(lines[next]) && !weightRe.test(lines[next]))next++;
    var prices=[];
    for(var k=i+1;k<next;k++){
      var m=lines[k].match(moneyRe);
      if(m)prices.push(Number(m[1].replace(/,/g,'')));
    }
    var price=prices.length===1?prices[0]:null;
    if(price===null)unpriced++;
    items.push({name:name,quantity:q?Number(q[1]):null,weightKg:w?Number(w[1])*(w[2]&&w[2].toLowerCase()==='g'?0.001:1):null,
      tentativeLinePrice:price,priceCandidates:prices.length});
  }
  var sum=items.reduce(function(s,x){return s+(x.tentativeLinePrice||0)},0);
  return JSON.stringify({characters:text.length,images:document.images.length,
    items:items,rejectedLabels:rejected,unpriced:unpriced,pricedTotal:Math.round(sum*100)/100});
})()"""
            browser.evaluateJavascript(js) { encoded ->
                try {
                    val decoded = JSONTokener(encoded).nextValue() as String
                    val data = JSONObject(decoded)
                    if (data.has("error")) {
                        status.text = data.optString("error")
                    } else {
                        val candidates = data.getJSONArray("items")
                        val summary = StringBuilder().apply {
                            append("${candidates.length()} anchored product entries | ${data.optInt("characters")} characters | ${data.optInt("images")} images\n")
                            append("Rejected quantity/weight labels: ${data.optInt("rejectedLabels")} | Prices needing review: ${data.optInt("unpriced")}\n")
                            append("Sum of tentative line prices: $" + String.format(java.util.Locale.US, "%.2f", data.optDouble("pricedTotal")) + "\n")
                            append("IMPORTANT: This is NOT an invoice reconciliation. Check every row and compare with the Coles invoice total.\n")
                            append("PREVIEW ONLY — nothing is saved or imported.\n")
                            for (i in 0 until candidates.length()) {
                                val item = candidates.getJSONObject(i)
                                append("\n${i + 1}. ${item.optString("name")}")
                                if (!item.isNull("quantity")) append(" | Qty ${item.optString("quantity")}")
                                if (!item.isNull("weightKg")) append(" | Weight ${item.optString("weightKg")} kg")
                                if (!item.isNull("tentativeLinePrice")) append(" | Tentative $${String.format(java.util.Locale.US, "%.2f", item.optDouble("tentativeLinePrice"))}")
                                else append(" | Price unverified")
                                append("\n")
                            }
                        }
                        status.text = "Found ${candidates.length()} quantity-anchored entries. Review all rows and totals."
                        val output = TextView(this@ColesCaptureFeasibilityActivity).apply {
                            text = summary.toString()
                            textSize = 14f
                            setPadding(24, 16, 24, 16)
                        }
                        val scroller = ScrollView(this@ColesCaptureFeasibilityActivity).apply {
                            addView(output)
                        }
                        AlertDialog.Builder(this@ColesCaptureFeasibilityActivity)
                            .setTitle("Coles order preview — read only")
                            .setView(scroller)
                            .setPositiveButton("Close", null)
                            .show()
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
