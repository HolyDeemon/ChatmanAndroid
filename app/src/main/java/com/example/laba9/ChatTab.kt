package com.example.laba9

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainer
import androidx.recyclerview.widget.RecyclerView
import com.example.laba9.MenuAdapter.MenuItem
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class ChatFragment : Fragment() {

    private var chatId: Int = -1
    private var callback: ChatCallback? = null
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button
    private lateinit var messagesContainer : LinearLayout
    private lateinit var scrollView : ScrollView
    companion object {
        private const val ARG_CHAT_ID = "chat_id"

        fun newInstance(chatId: Int): ChatFragment {
            val fragment = ChatFragment()
            val args = Bundle()
            args.putInt(ARG_CHAT_ID, chatId)
            fragment.arguments = args
            return fragment
        }
    }

    interface ChatCallback {
        fun onMessageSent(chatId: Int, message: String)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Проверяем, что Activity реализует наш интерфейс
        if (context is ChatCallback) {
            callback = context
        } else {
            throw RuntimeException("$context must implement ChatCallback")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.chat_fragment_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatId = arguments?.getInt(ARG_CHAT_ID) ?: -1
        messageInput = view.findViewById(R.id.messageInput)
        sendButton = view.findViewById(R.id.sendButton)
        messagesContainer = view.findViewById<LinearLayout>(R.id.messages)
        scrollView = view.findViewById<ScrollView>(R.id.messagesScroll)

        setupChat(chatId)
    }

    private fun setupChat(chatId: Int) {
        Log.d("Chat", "Opening chat with ID: $chatId")

        sendButton.setOnClickListener {
            val message = messageInput.text.toString()
            if (message.isNotEmpty()) {
                // Вызываем колбэк
                callback?.onMessageSent(chatId, message)
                messageInput.text.clear()
            }
        }
    }

    private fun addMessage(text: String, isMyMessage: Boolean) {

        val messageView = LayoutInflater.from(requireContext())
            .inflate(R.layout.message_layout, messagesContainer, false)

        val messageContainer = messageView.findViewById<LinearLayout>(R.id.messageContainer)
        val messageBubble = messageView.findViewById<LinearLayout>(R.id.messageBubble)
        val messageText = messageView.findViewById<TextView>(R.id.messageText)

        messageText.setText(text)

        if (isMyMessage) {
            messageContainer.gravity = Gravity.END
            messageBubble.setBackgroundResource(R.drawable.message_bubble)
        } else {

            messageContainer.gravity = Gravity.START
            messageBubble.setBackgroundResource(R.drawable.other_message_bubble)
        }

        messagesContainer.addView(messageView)

        // Прокручиваем вниз
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    fun addNewMessage(message: String, isMyMessage: Boolean) {
        addMessage(message, isMyMessage)
    }

    override fun onDetach() {
        super.onDetach()
        callback = null
    }
}