package com.example.laba9

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView

class MenuAdapter(
    private val menuItems: List<MenuItem>,
    private val onItemClick: (MenuItem) -> Unit
) : RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {

    data class MenuItem(
        val id: Int,
        val name: String,
        val message: String? = null,
        val count: Int
    )

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val button: ImageButton = itemView.findViewById(R.id.chatBtn)
        private val name: TextView = itemView.findViewById(R.id.name)
        private val message: TextView = itemView.findViewById(R.id.message)
        private val count: TextView = itemView.findViewById(R.id.count)
        fun bind(item: MenuItem, onItemClick: (MenuItem) -> Unit) {

            name.setText(item.name)
            if(item.count > 0) {
                message.setText(item.message)
                count.setText(item.count.toString())
            }else{
                message.setText(" ")
                count.isVisible = false
            }

            button.setOnClickListener { onItemClick(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat, parent, false)
        return MenuViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menuItems[position], onItemClick)
    }

    override fun getItemCount() = menuItems.size
}