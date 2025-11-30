package com.example.laba9

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.laba9.MenuAdapter.MenuItem
import com.google.android.material.appbar.MaterialToolbar
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.Request
import okhttp3.Response
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocketListener
import okhttp3.WebSocket
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import androidx.core.view.isNotEmpty
import androidx.core.view.get

class MainActivity : AppCompatActivity(), ChatFragment.ChatCallback {
    private var token : String? = null
    private val homeUrl: String = "https://192.168.137.1:443/chat"
    private lateinit var layoutInflater : LayoutInflater
    var cookieClass : PersistentCookieJar = PersistentCookieJar()

    private lateinit var menuRecyclerView: RecyclerView
    private lateinit var chatContainer: FrameLayout
    private lateinit var toolbar: MaterialToolbar
    private val webSocket = WebSocket()
    private var currentChatId : Int = -1


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkToken()
        if(token != null) {
            cookieClass.addCookie(homeUrl,"users_access_token", (token).toString())
            getUsers()
        }

        menuRecyclerView = findViewById(R.id.chatsmenu)
        chatContainer = findViewById(R.id.chatContainer)

        layoutInflater = LayoutInflater.from(applicationContext)


        toolbar = findViewById<MaterialToolbar>(R.id.toolbar)

        webSocket.onMessageReceived = { chatId, message ->
            runOnUiThread {
                // Обновляем UI с новым сообщением
                if (currentChatId == chatId) {
                    addMessageToChat(message, false) // false = не мое сообщение
                }
            }
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.leave -> {
                    logout()
                    true
                }
                else -> false
            }
        }

        setupNavigationIcon()

    }
    private fun setupNavigationIcon() {
        // Устанавливаем иконку назад и слушатель
        toolbar.setNavigationIcon(R.drawable.v_back)
        toolbar.setNavigationOnClickListener {
            closeChatAndShowMenu()
        }

        // Сначала скрываем
        toolbar.navigationIcon = null
        toolbar.setTitle(R.string.my_chats)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)
        return true
    }

    fun checkToken(){
        val extras = getIntent().getExtras()
        if(extras != null){
            val extStr = extras.getString("token")
            token = extStr
        }
        if(token == null){
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    fun getUsers() {
        try {
            val getRequest = Request.Builder().url("$homeUrl/users").build()
            createUnsafeOkHttpClient().newCall(getRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Network", "Request failed", e)
                    runOnUiThread {
                        // Показываем ошибку пользователю
                        Toast.makeText(this@MainActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        Log.d("Network", "Response code: ${response.code}")
                        val responseBody = response.body?.string() ?: ""
                        Log.d("Network", "Response body: $responseBody")

                        runOnUiThread {
                            addChats(responseBody)
                        }
                    } catch (e: Exception) {
                        Log.e("Network", "Error processing response", e)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Network", "Error in getUsers", e)
        }
    }

    fun addChats(data: String) {
        Log.d("Chats", "addChats called with data: ${data.take(100)}...")

        try {
            val jsonObject = JSONObject(data)
            val count = jsonObject.get("count").toString().toInt()
            Log.d("Chats", "Count: $count")

            val items = mutableListOf<MenuItem>()
            val actions = mutableMapOf<Int, () -> Unit>()

            for (i in 0 until count) {
                try {
                    val USER = jsonObject.get(i.toString()).toString()
                    val ID = jsonObject.get((i + count).toString()).toString().toInt()

                    Log.d("Chats", "Adding user: $USER with ID: $ID")

                    items.add(MenuItem(ID, USER, "Загрузка...", 0))
                    actions[ID] = {
                        Log.d("Chats", "Clicked on user: $USER")
                        openChat(ID, USER)

                    }
                } catch (e: Exception) {
                    Log.e("Chats", "Error processing user $i", e)
                }
            }

            Log.d("Chats", "Setting up menu with ${items.size} items")
            setupMenu(items, actions)

        } catch (e: Exception) {
            Log.e("Chats", "Error in addChats", e)
            // Показываем пустое меню при ошибке
            setupMenu(emptyList(), emptyMap())
        }
    }

    private fun setupMenu(menuItems: List<MenuItem>, actions: Map<Int, () -> Unit>) {
        Log.d("Menu", "setupMenu called with ${menuItems.size} items")

        try {
            val menuRecyclerView = findViewById<RecyclerView>(R.id.chatsmenu)
            if (menuRecyclerView == null) {
                Log.e("Menu", "RecyclerView not found!")
                return
            }

            val layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            menuRecyclerView.layoutManager = layoutManager

            val adapter = MenuAdapter(menuItems) { menuItem ->
                Log.d("Menu", "Menu item clicked: ${menuItem.name} (ID: ${menuItem.id})")
                try {
                    actions[menuItem.id]?.invoke()
                } catch (e: Exception) {
                    Log.e("Menu", "Error executing action", e)
                }
            }

            menuRecyclerView.adapter = adapter
            Log.d("Menu", "Adapter set successfully")

        } catch (e: Exception) {
            Log.e("Menu", "Error in setupMenu", e)
            e.printStackTrace()
        }
    }

    private fun openChat(chatId: Int, userName: String) {
        val chatFragment = ChatFragment.newInstance(chatId)

        currentChatId = chatId
        UpdateMessages(chatId)

        supportFragmentManager.beginTransaction()
            .replace(R.id.chatContainer, chatFragment)
            .addToBackStack("chat_$chatId")
            .commit()

        menuRecyclerView.visibility = View.GONE
        toolbar.setNavigationIcon(R.drawable.v_back)
        toolbar.title = userName
    }

    override fun onMessageSent(chatId: Int, message: String) {
        Log.d("Chat", "Message sent to chat $chatId: $message")

        sendMessageToServer(chatId, message)
        UpdateMessages(chatId)
    }

    private fun UpdateMessages(chatId : Int) {
        clearMessages()
        try {
            val getRequest = Request.Builder().url("$homeUrl/messages/${chatId}").build()
            createUnsafeOkHttpClient().newCall(getRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Network", "Request failed", e)
                    runOnUiThread {
                        // Показываем ошибку пользователю
                        Toast.makeText(this@MainActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        Log.d("Network", "Response code: ${response.code}")
                        val responseBody = response.body?.string() ?: ""
                        Log.d("Network", "Response body: $responseBody")
                        runOnUiThread {
                            var g = Gson()
                            var messages = g.fromJson<Array<Message>>(responseBody, Array<Message>::class.java).toList()

                            for(i in 0 until messages.count()){
                                addMessageToChat(messages[i].content, chatId != messages[i].to_id)
                            }

                        }
                    } catch (e: Exception) {
                        Log.e("Network", "Error processing response", e)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Network", "Error in getUsers", e)
        }
    }

    private fun addMessageToChat(message: String, isMyMessage: Boolean) {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.chatContainer)
        if (currentFragment is ChatFragment) {
            currentFragment.addNewMessage(message, isMyMessage)
        }
    }

    private fun sendMessageToServer(chatId: Int, message: String) {
        Log.v("HTTP", "sending message")
        try {
            val jsonObject = JSONObject().apply {
                put("to_id", chatId)
                put("content", message)
            }
            val requestBody = jsonObject.toString()
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder().url("$homeUrl/messages")
                .header("Authorization", token.toString())
                .post(requestBody)
                .build()

            // Отправляем запрос
            createUnsafeOkHttpClient().newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("HTTP", "Failed to send message via HTTP", e)
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Ошибка отправки сообщения", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.d("HTTP", "Message sent successfully via HTTP")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Сообщение отправлено", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e("HTTP", "Failed to send message, code: ${response.code}")
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Ошибка отправки: ${response.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })

        } catch (e: Exception) {
            Log.e("HTTP", "Error creating request", e)
            runOnUiThread {
                Toast.makeText(this@MainActivity, "Ошибка создания запроса", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun closeChatAndShowMenu() {
        menuRecyclerView.visibility = View.VISIBLE

        clearMessages()
        toolbar.navigationIcon = null
        toolbar.setTitle(R.string.my_chats)

        supportFragmentManager.popBackStack()
    }

    private fun clearMessages() {
        try {
            val messagesLayout = findViewById<LinearLayout>(R.id.messages)
            val scrollView = findViewById<ScrollView>(R.id.messagesScroll)

            messagesLayout?.removeAllViews()
            scrollView?.scrollTo(0, 0)

        } catch (e: Exception) {
            Log.e("MainActivity", "Error clearing messages: ${e.message}")
        }
    }
    fun logout(){
        try {
            val getRequest = Request.Builder()
                .url("https://192.168.137.1:443/auth/logout")
                .build()
            createUnsafeOkHttpClient().newCall(getRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Logout", "Request failed", e)
                    runOnUiThread {
                        // Показываем ошибку пользователю
                        Toast.makeText(this@MainActivity, "Ошибка сети", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    try {
                        moveLogin()
                    } catch (e: Exception) {
                        Log.e("Logout", "Error processing response", e)
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        } catch (e: Exception) {
            Log.e("Logout", "Error in getUsers", e)
        }
    }
    fun moveLogin(){
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
    fun createUnsafeOkHttpClient(): OkHttpClient{
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .cookieJar(cookieClass)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocket.disconnect()
    }
}

class PersistentCookieJar : CookieJar {
    private val cookieStore = ConcurrentHashMap<String, List<Cookie>>()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore[url.host] = cookies
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    fun clearCookies() {
        cookieStore.clear()
    }

    fun addCookie(url: String, name: String, value: String, maxAge: Long = 86400L) {
        val httpUrl = url.toHttpUrlOrNull() ?: return

        val cookie = Cookie.Builder()
            .name(name)
            .value(value)
            .domain(httpUrl.host)
            .path("/")
            .expiresAt(System.currentTimeMillis() + maxAge * 1000)
            .build()

        addCookie(httpUrl, cookie)
    }

    fun addCookie(url: HttpUrl, cookie: Cookie) {
        val host = url.host
        val existingCookies = (cookieStore[host] ?: mutableListOf()).toMutableList()

        existingCookies.removeAll { it.name == cookie.name }

        existingCookies.add(cookie)
        cookieStore[host] = existingCookies
    }


}

class Message{
    var from_id : Int = 0
    var to_id: Int = 0
    var content: String = ""
}
