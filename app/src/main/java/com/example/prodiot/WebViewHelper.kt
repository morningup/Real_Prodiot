package com.example.prodiot

import android.content.Context
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

class PostWebViewHelper(private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("post_data", Context.MODE_PRIVATE)

    private fun escapeCodeString(code: String): String {
        // 특수 문자나 특수 기호를 이스케이프 처리
        return code
            .replace("\\", "\\\\")  // 역슬래시
            .replace("\"", "\\\"")  // 큰따옴표
            .replace("\'", "\\\'")  // 작은따옴표
            .replace("\n", "\\n")   // 줄바꿈
            .replace("\r", "")      // 캐리지 리턴
    }

    fun configureWebView(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
    }

    fun submitCode(webView: WebView, progressDialog: CustomProgressDialog) {
        var CodeText = ""
        var InputText = ""
        val codeString = sharedPreferences.getString("CodeString", CodeText)
        Log.d("MainActivity", "Output Text: $codeString")
        val inputString = sharedPreferences.getString("InputString", InputText)
        Log.d("MainActivity", "Output Text: $inputString")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val js =
                    """var textarea = document.getElementById('file');""" // 페이지 로드가 완료된 후 textarea 요소 가져오기
                view?.evaluateJavascript(js, null)
                val js2 =
                    """textarea.value = "${escapeCodeString(codeString.toString())}";""" // textarea 내 값을 변경하기
                view?.evaluateJavascript(js2, null)
                val js3 =
                    """var textarea = document.getElementById('input');""" // 페이지 로드가 완료된 후 textarea 요소 가져오기
                view?.evaluateJavascript(js3, null)
                val js4 = """textarea.value = "${escapeCodeString(inputString.toString())}";"""
                view?.evaluateJavascript(js4, null)
                val js5 = """document.getElementsByName("Submit")[0].click();""" // Submit 버튼 클릭하기
                view?.evaluateJavascript(js5, null)
                // 로드가 완료된 후 실행할 코드
                GlobalScope.launch(Dispatchers.Main) {
                    do {
                        delay(1000)
                        val currentUrl = webView.url
                        val outputText = withContext(Dispatchers.IO) {
                            val docs = Jsoup.connect(currentUrl).get()
                            docs.select("#output-text").text()
                        }
                        Log.d("MainActivity", "Output Text: $outputText")
                    } while (outputText.isBlank())
                    val currentUrl = webView.url
                    val outputText = withContext(Dispatchers.IO) {
                        val docs = Jsoup.connect(currentUrl).get()
                        docs.select("#output-text").text()
                    }
                    val textView =
                        (context as AppCompatActivity).findViewById<TextView>(R.id.output_edittext)
                    textView.text = outputText
                    Log.d("MainActivity", "Output Text: $codeString")
                    Log.d("MainActivity", "Output Text: $inputString")
                    Log.d("MainActivity", "Output Text: $outputText")
                    Log.d("MainActivity", "Output Text: $currentUrl")
                    // progressDialog 종료
                    progressDialog.dismiss()
                }
            }
        }
        webView.loadUrl("https://ideone.com/")
    }
}

class StepWebViewHelper(private val context: Context) {
    private fun escapeCodeString(code: String): String {
        // 특수 문자나 특수 기호를 이스케이프 처리
        return code
            .replace("\\", "\\\\")  // 역슬래시
            .replace("\"", "\\\"")  // 큰따옴표
            .replace("\'", "\\\'")  // 작은따옴표
            .replace("\n", "\\n")   // 줄바꿈
            .replace("\r", "")      // 캐리지 리턴
    }

    fun configureWebView(webView: WebView) {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.useWideViewPort = true
    }

    fun submitCode(webView: WebView, progressDialog: CustomProgressDialog) {
        var CodeText = ""
        var InputText = ""
        val sharedPreferences = context.getSharedPreferences("step_data", Context.MODE_PRIVATE)
        val codeString = sharedPreferences.getString("CodeString", CodeText)
        Log.d("MainActivity", "Output Text: $codeString")
        val inputString = sharedPreferences.getString("InputString", InputText)
        Log.d("MainActivity", "Output Text: $inputString")
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val js =
                    """var textarea = document.getElementById('file');""" // 페이지 로드가 완료된 후 textarea 요소 가져오기
                view?.evaluateJavascript(js, null)
                val js2 =
                    """textarea.value = "${escapeCodeString(codeString.toString())}";""" // textarea 내 값을 변경하기
                view?.evaluateJavascript(js2, null)
                val js3 =
                    """var textarea = document.getElementById('input');""" // 페이지 로드가 완료된 후 textarea 요소 가져오기
                view?.evaluateJavascript(js3, null)
                val js4 = """textarea.value = "${escapeCodeString(inputString.toString())}";"""
                view?.evaluateJavascript(js4, null)
                val js5 = """document.getElementsByName("Submit")[0].click();""" // Submit 버튼 클릭하기
                view?.evaluateJavascript(js5, null)
                // 로드가 완료된 후 실행할 코드
                GlobalScope.launch(Dispatchers.Main) {
                    do {
                        delay(1000)
                        val currentUrl = webView.url
                        val outputText = withContext(Dispatchers.IO) {
                            val docs = Jsoup.connect(currentUrl).get()
                            docs.select("#output-text").text()
                        }
                        Log.d("MainActivity", "Output Text: $outputText")
                    } while (outputText.isBlank())
                    val currentUrl = webView.url
                    val outputText = withContext(Dispatchers.IO) {
                        val docs = Jsoup.connect(currentUrl).get()
                        docs.select("#output-text").text()
                    }
                    val textView =
                        (context as AppCompatActivity).findViewById<TextView>(R.id.output_edittext)
                    textView.text = outputText
                    Log.d("MainActivity", "Output Text: $codeString")
                    Log.d("MainActivity", "Output Text: $inputString")
                    Log.d("MainActivity", "Output Text: $outputText")
                    Log.d("MainActivity", "Output Text: $currentUrl")
                    // progressDialog 종료
                    progressDialog.dismiss()
                }
            }
        }
        webView.loadUrl("https://ideone.com/")
    }
}

