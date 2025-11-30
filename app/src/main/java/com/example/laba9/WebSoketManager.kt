package com.example.laba9

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class WebSocket {
    private var webSocket: WebSocket? = null
    var onMessageReceived: ((chatId: Int, message: String) -> Unit)? = null

    fun connect(client : OkHttpClient, serverUrl: String, token: String) {
        val request = Request.Builder()
            .url(serverUrl)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Origin", "192.168.137.1")
            .addHeader("User-Agent", "Android-Chat-App")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.v("WebSocket", "WebSocket подключен")
                this@WebSocket.webSocket = webSocket
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val type = json.getString("type")

                    if (type == "new_message") {
                        val chatId = json.getInt("chat_id")
                        val content = json.getString("content")

                        // Передаем сообщение в активность
                        onMessageReceived?.invoke(chatId, content)
                    }
                } catch (e: Exception) {
                    Log.e("WebSocket","Ошибка парсинга: ${e.message}")
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocket", "Ошибка WebSocket: ${t.message}")
            }
        }

        client.newWebSocket(request, listener)
    }

    fun sendMessage(chatId: Int, message: String) {
        val json = """{
            "type": "message",
            "chat_id": $chatId,
            "content": "$message"
        }"""

        webSocket?.send(json)
        Log.v("WebSocket", "Отправлено: $message")
    }

    fun disconnect() {
        webSocket?.close(1000, "Пользователь вышел")
    }
}