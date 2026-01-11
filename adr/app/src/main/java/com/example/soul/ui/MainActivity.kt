package com.example.soul.ui

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.soul.R
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
                
                // Since testConnection() returns HealthResponse directly
                val message = "✅ Kết nối thành công!\n$response"
                
                runOnUiThread {
                    tvResult.text = message
                    btnTest.isEnabled = true
                }
                
            } catch (e: Exception) {
                val errorMessage = when (e) {
                    is ConnectException -> "❌ Không thể kết nối đến máy chủ. Vui lòng kiểm tra kết nối mạng."
                    is SocketTimeoutException -> "⏱️ Hết thời gian chờ kết nối. Vui lòng thử lại."
                    is UnknownHostException -> "🔍 Không tìm thấy máy chủ. Vui lòng kiểm tra URL."
                    is JsonSyntaxException -> "📄 Lỗi phân tích dữ liệu: ${e.message}"
                    else -> "❌ Lỗi: ${e.message}"
                }
                
                Log.e("MainActivity", "API call failed", e)
                runOnUiThread {
                    tvResult.text = errorMessage
                    btnTest.isEnabled = true
                    Toast.makeText(this@MainActivity, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
