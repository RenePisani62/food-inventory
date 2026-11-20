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
import android.app.Dialog
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import org.json.JSONObject
import org.json.JSONTokener

/** PP-158B.4: quantity-anchored read-only extraction diagnostic. */
class ColesCaptureFeasibilityActivity : Activity() {
    private lateinit var browser: WebView
    private lateinit var status: TextView
    private val startUrl = "https://www.coles.com.au/account/orders"
    private val logTag = "PPColesAuth"
    private var latestValidatedItems: String? = null
    private var latestOrderMetadata: String? = null

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
  // PP-158B.8b: use bounded searches around Coles' visible summary labels.
  // Labels and values may occupy different DOM text nodes or multiple lines.
  function findLabel(label){
    return lines.findIndex(function(x){return x.toLowerCase()===label.toLowerCase();});
  }
  function nearLabel(label, test, maxAhead){
    var i=findLabel(label);
    if(i<0)return null;
    for(var j=i+1;j<Math.min(lines.length,i+1+maxAhead);j++){
      if(test.test(lines[j]))return lines[j];
    }
    return null;
  }
  var receiptId=nearLabel('Receipt ID',/^[A-Z0-9-]{1,40}$/i,4);
  var datePattern=/^(?:(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\s+)?\d{1,2}\s+[A-Za-z]+(?:\s+\d{4})?,?\s+\d{1,2}:\d{2}\s*(?:AM|PM)$/i;
  // Prefer the first date-like value following When, regardless of whether
  // Coles splits weekday, day, month and time across text nodes.
  var when=nearLabel('When',datePattern,6);
  var whenDiagnostic=null;
  var whenIx=findLabel('When');
  if(whenIx<0)whenDiagnostic='When label not found';
  if(whenIx>=0){
    var nearby=lines.slice(whenIx+1,Math.min(lines.length,whenIx+8));
    // Bounded, date-only diagnostic; never include arbitrary account data.
    whenDiagnostic=nearby.map(function(v){
      var found=v.match(/(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday|\d{1,2}:\d{2}\s*(?:AM|PM)?|\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\b|\b\d{1,2}\b)/gi);
      return found?found.join(' '):'';
    }).filter(Boolean).slice(0,4).join(' | ')||'No date-like text nearby';
    if(!when){
      var joined=nearby.slice(0,4).join(' ');
      var flexible=joined.match(/(?:(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)\s+)?\d{1,2}\s+[A-Za-z]{3,9}(?:\s+\d{4})?\s*,?\s*\d{1,2}:\d{2}\s*(?:AM|PM)/i);
      if(flexible)when=flexible[0];
    }
  }
  // PP-158B.8e: inspect DOM adjacent to the visible "When" label.
  // Report only date-shaped matches and structural information, not arbitrary
  // nearby text (which could contain private account information).
  var whenDomDiagnostic='When element not found';
  var candidates=Array.from(document.querySelectorAll('span,div,p,dt,dd,strong,b,label,h2,h3,h4'))
    .filter(function(el){
      return el.children.length===0 && (el.textContent||'').trim().toLowerCase()==='when';
    }).slice(0,5);
  if(candidates.length){
    var probes=[];
    candidates.forEach(function(el){
      var nodes=[el.nextElementSibling,el.parentElement&&el.parentElement.nextElementSibling,
        el.parentElement&&el.parentElement.parentElement,
        el.parentElement&&el.parentElement.parentElement&&el.parentElement.parentElement.nextElementSibling];
      nodes.forEach(function(node){
        if(!node)return;
        var value=(node.innerText||node.textContent||'').trim().slice(0,500);
        var dateMatch=value.match(/(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday)?\s*\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*(?:\s+\d{4})?\s*,?\s*\d{1,2}:\d{2}\s*(?:AM|PM)/i);
        if(dateMatch && !when)when=dateMatch[0].trim();
        probes.push(node.tagName.toLowerCase()+':'+value.length+'chars'+(dateMatch?':date found':':no date'));
      });
    });
    whenDomDiagnostic='When nodes '+candidates.length+'; '+probes.slice(0,8).join(' | ');
  }
  // Store must be in the Order summary area, not an arbitrary Coles product.
  var store=null, summaryIx=findLabel('Order summary');
  if(summaryIx>=0){
    var endSummary=Math.min(lines.length,summaryIx+28);
    for(var si=summaryIx+1;si<endSummary;si++){
      if(/^Coles\s+[A-Za-z][A-Za-z -]{1,70}$/i.test(lines[si])){
        store=lines[si];break;
      }
      if(/^Items in your order/i.test(lines[si]))break;
    }
  }
  // Count can be split into adjacent DOM text nodes or separate lines.
  var expectedCount=null;
  var countText=text.match(/Items\s+in\s+your\s+order\s*\(\s*(\d{1,3})\s*\)/i);
  if(countText)expectedCount=Number(countText[1]);
  if(expectedCount===null){
    var headingIx=lines.findIndex(function(x){return /^Items in your order/i.test(x);});
    if(headingIx>=0){
      var heading=lines.slice(headingIx,headingIx+3).join(' ');
      var count=heading.match(/\(\s*(\d{1,3})\s*\)/);
      if(count)expectedCount=Number(count[1]);
    }
  }
  return JSON.stringify({receiptId:receiptId,orderDate:when,whenDiagnostic:whenDiagnostic,whenDomDiagnostic:whenDomDiagnostic,storeName:store,expectedCount:expectedCount,characters:text.length,images:document.images.length,
    items:items,rejectedLabels:rejected,unpriced:unpriced,pricedTotal:Math.round(sum*100)/100});
})()"""
            // Expand the order's collapsed item section before inspecting the DOM.
            // This interacts only with the currently displayed Coles page.
            val expandJs = """(function(){
              var nodes=Array.from(document.querySelectorAll('button,a,[role="button"]'));
              var match=nodes.find(function(n){return /^(view all items|show all items)$/i.test((n.innerText||n.textContent||'').trim());});
              if(match){match.click();return 'expanded';}
              return 'not-found';
            })()"""
            browser.evaluateJavascript(expandJs) { expansion ->
                status.text = if (expansion.contains("expanded"))
                    "Expanding all items; checking order…" else
                    "Checking visible order items (no expansion link detected)…"
                browser.postDelayed({
                    if (isFinishing || isDestroyed) return@postDelayed
                    browser.evaluateJavascript(js) { encoded ->
                try {
                    val decoded = JSONTokener(encoded).nextValue() as String
                    val data = JSONObject(decoded)
                    if (data.has("error")) {
                        status.text = data.optString("error")
                    } else {
                        val candidates = data.getJSONArray("items")
                        latestValidatedItems = candidates.toString()
                        latestOrderMetadata = JSONObject().apply {
                            put("receiptId", data.optString("receiptId", ""))
                            put("storeName", data.optString("storeName", ""))
                            put("expectedCount", data.optInt("expectedCount", -1))
                            put("orderDate", data.optString("orderDate", ""))
                            put("pageUrl", browser.url.orEmpty().take(500))
                        }.toString()
                        val summary = StringBuilder().apply {
                            append("${candidates.length()} product entries | ${data.optInt("characters")} characters | ${data.optInt("images")} images\n")
                            append("Rejected quantity/weight labels: ${data.optInt("rejectedLabels")} | Prices needing review: ${data.optInt("unpriced")}\n")
                            append("Receipt ID: " + data.optString("receiptId").ifBlank { "Not identified" } + " | When: " + (data.optString("orderDate").takeUnless { it.isBlank() || it == "null" } ?: "Not identified") + "\n")
                            append("Store: " + data.optString("storeName").ifBlank { "Not identified" } + " | Expected items: " + data.optInt("expectedCount", -1).let { if (it < 0) "Unknown" else it.toString() } + "\n")
                            append("Tentative sum: $" + String.format(java.util.Locale.US, "%.2f", data.optDouble("pricedTotal")) + "\n")
                            append("NOT an invoice reconciliation. Compare each row and the invoice total.\n")
                            for (i in 0 until candidates.length()) {
                                val item = candidates.getJSONObject(i)
                                append("\n${i + 1}. ${item.optString("name")}")
                                if (!item.isNull("quantity")) append(" | Qty ${item.optString("quantity")}")
                                if (!item.isNull("weightKg")) append(" | Weight ${item.optString("weightKg")} kg")
                                if (!item.isNull("tentativeLinePrice")) append(" | $${String.format(java.util.Locale.US, "%.2f", item.optDouble("tentativeLinePrice"))}")
                                else append(" | Price unverified")
                                append("\n")
                            }
                        }
                        status.text = "Found ${candidates.length()} quantity-anchored entries. Review all rows and totals."
                        // Custom dialog: only the item list scrolls; actions stay visible.
                        val dialog = Dialog(this@ColesCaptureFeasibilityActivity)
                        val density = resources.displayMetrics.density
                        fun dp(n: Int) = (n * density).toInt()
                        val root = LinearLayout(this@ColesCaptureFeasibilityActivity).apply {
                            orientation = LinearLayout.VERTICAL
                            setBackgroundColor(Color.WHITE)
                            setPadding(dp(16), dp(14), dp(16), dp(12))
                        }
                        val title = TextView(this@ColesCaptureFeasibilityActivity).apply {
                            text = "Coles order preview — read only"
                            textSize = 19f
                            setTextColor(Color.BLACK)
                        }
                        root.addView(title)
                        root.addView(TextView(this@ColesCaptureFeasibilityActivity).apply {
                            text = "${candidates.length()} products · $" + String.format(java.util.Locale.US, "%.2f", data.optDouble("pricedTotal")) + " provisional\nNo receipt or inventory changes."
                            textSize = 14f
                            setTextColor(Color.DKGRAY)
                            setPadding(0, dp(8), 0, dp(8))
                        })
                        val output = TextView(this@ColesCaptureFeasibilityActivity).apply {
                            text = summary.toString()
                            textSize = 13f
                            setTextColor(Color.DKGRAY)
                            setPadding(0, dp(6), 0, dp(6))
                        }
                        val scroller = ScrollView(this@ColesCaptureFeasibilityActivity).apply {
                            addView(output)
                            isFillViewport = false
                        }
                        root.addView(scroller, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                        val actions = LinearLayout(this@ColesCaptureFeasibilityActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = Gravity.END or Gravity.CENTER_VERTICAL
                        }
                        val close = Button(this@ColesCaptureFeasibilityActivity).apply {
                            text = "Close"
                            setOnClickListener { dialog.dismiss() }
                        }
                        val stage = Button(this@ColesCaptureFeasibilityActivity).apply {
                            text = "Stage in PantryPal"
                            isAllCaps = false
                            setOnClickListener {
                                val payload = latestValidatedItems ?: return@setOnClickListener
                                setResult(RESULT_OK, android.content.Intent()
                                    .putExtra("pp_coles_staging_items", payload)
                                    .putExtra("pp_coles_order_metadata", latestOrderMetadata))
                                dialog.dismiss()
                                finish()
                            }
                        }
                        actions.addView(close, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                        actions.addView(stage, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f))
                        root.addView(actions)
                        dialog.setContentView(root)
                        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.94).toInt(), (resources.displayMetrics.heightPixels * 0.76).toInt())
                        dialog.show()
                        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.94).toInt(), (resources.displayMetrics.heightPixels * 0.76).toInt())
                    }
                } catch (e: Exception) {
                    Log.w(logTag, "Page metadata inspection failed", e)
                    status.text = "Page inspection unavailable. No import performed."
                }
                    }
                }, 1100L)
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
