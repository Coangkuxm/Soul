package com.example.soul

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.soul.data.HealthResponse
import com.example.soul.api.RetrofitClient
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class MainActivity : AppCompatActivity() {
    private lateinit var btnTest: Button
    private lateinit var tvResult: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnTest = findViewById(R.id.btn_test)
        tvResult = findViewById(R.id.tv_result)

        btnTest.setOnClickListener {
            testServerConnection()
        }
    }

    private fun testServerConnection() {
        btnTest.isEnabled = false
        tvResult.text = "🔄 Đang kết nối đến server..."

        lifecycleScope.launch {
            try {
                Log.d("MainActivity", "Attempting to connect to server...")
                val response = RetrofitClient.apiService.testConnection()
                Log.d("MainActivity", "Response received: $response")
                
                runOnUiThread {
                    val displayText = """
                        |✅ Kết nối thành công!
                        |
                        |${response.getDisplayText()}
                    """.trimMargin()
                    
                    tvResult.text = displayText
                    btnTest.isEnabled = true
                }
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is JsonSyntaxException -> {
                        "Lỗi định dạng dữ liệu: ${e.message}"
                    }
                    is ConnectException -> "❌ Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng."
                    is SocketTimeoutException -> "⏱ Hết thời gian chờ kết nối. Server có thể đang quá tải."
                    is UnknownHostException -> "🌐 Không tìm thấy máy chủ. Vui lòng kiểm tra lại đường dẫn API."
                    else -> "⚠️ Lỗi: ${e.message ?: "Không xác định"}"
                }
                
                Log.e("MainActivity", "Connection error", e)
                runOnUiThread {
                    tvResult.text = """
                        |$errorMessage
                        |
                        |Vui lòng kiểm tra:
                        |1. Máy chủ có đang hoạt động không
                        |2. Kết nối mạng của bạn
                        |3. URL API: ${RetrofitClient.BASE_URL}
                    """.trimMargin()
                    
                    btnTest.isEnabled = true
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
