package com.example.laba9

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener
import com.google.android.material.tabs.TabLayout.TabLayoutOnPageChangeListener
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.*
import kotlin.collections.set


class LoginActivity : AppCompatActivity() {

    private var adapter : PageAdapter = PageAdapter(getSupportFragmentManager(), 0)
    val homeUrl : String = "https://192.168.137.1:443/auth"

    val CHANNEL_ID = "chatman1"
    val NOTIFICATION_ID = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tabLayout = findViewById<View?>(R.id.tab_layout) as TabLayout
        tabLayout.setTabGravity(TabLayout.GRAVITY_FILL)

        val viewPager = findViewById<View?>(R.id.pager) as ViewPager
        adapter = PageAdapter(getSupportFragmentManager(), tabLayout.getTabCount())
        viewPager.setAdapter(adapter)
        viewPager.addOnPageChangeListener(TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.setOnTabSelectedListener(object : OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.getPosition())
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
            }

        })
        createNotificationChannel()
    }

    fun loginClicked(view: View)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            val REQUEST_CODE = 100
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET),
                REQUEST_CODE)
        } else {

            val requestBody = RequestBody.create( "application/json".toMediaTypeOrNull(),
                    JSONObject().put("email", getViewText(R.id.loginEmailEdit))
                    .put("password", getViewText(R.id.loginPasswordEdit)
                    ).toString()
                )

            val getRequest = Request.Builder()
                .url(homeUrl + "/login")
                .post(requestBody)
                .build()

            createUnsafeOkHttpClient().newCall(getRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.v("papaeva", e.toString())
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    Log.v("papaeva", response.code.toString())
                    responseGet(response)
                }
            })
        }
    }

    fun regisClicked(view: View)
    {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            val REQUEST_CODE = 100
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.INTERNET),
                REQUEST_CODE)
        } else {

            val requestBody = RequestBody.create( "application/json".toMediaTypeOrNull(),
                JSONObject().put("name", getViewText(R.id.regisNameEdit))
                    .put("email", getViewText(R.id.regisEmailEdit))
                    .put("password", getViewText(R.id.regisPasswordEdit))
                    .put("password_check", getViewText(R.id.regisCHPasswordEdit)).toString()
            )

            val getRequest = Request.Builder()
                .url(homeUrl + "/register")
                .post(requestBody)
                .build()

            createUnsafeOkHttpClient().newCall(getRequest).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.v("papaeva", e.toString())
                }

                @Throws(IOException::class)
                override fun onResponse(call: Call, response: Response) {
                    Log.v("papaeva", response.code.toString())
                    if(response.code == 200){ restart() }
                }
            })
        }
    }

    fun responseGet(response: Response){
        if(response.code == 200){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
                }
            }
            sendNotification()

            val intent = Intent(this, MainActivity::class.java)
            var token = JSONObject(response.body.string()).get("access_token")
            intent.putExtra("token", token.toString())
            startActivity(intent)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                NotificationManagerCompat.from(getApplicationContext())
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null)
            {
                val name = getString(R.string.app_name)
                val descriptionText = getString(R.string.channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(
                    CHANNEL_ID, name,
                    importance
                ).apply { description = descriptionText }
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    fun restart(){
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    fun createUnsafeOkHttpClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, SecureRandom())

        return OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Отключает проверку hostname
            .build()
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun sendNotification(){
        // Intent that triggers when the notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder: NotificationCompat.Builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.v_send)
            .setContentTitle(getString(R.string.greetings))
            .setContentText(getString(R.string.description))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }


    fun getViewText(id : Int) : String{
        return findViewById<EditText>(id)?.getText().toString()
    }
    fun setViewText(id : Int, str : String){
        findViewById<EditText>(id)?.setText(str)
    }
}

