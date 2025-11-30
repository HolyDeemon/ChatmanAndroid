package com.example.laba9

import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter


class PageAdapter (fm: FragmentManager, var mNumOfTabs: Int) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment {
        when (position) {
            0 -> {
                val tab1: LoginTab =  LoginTab()
                return tab1
            }

            else -> {
                val tab2: RegisterTab = RegisterTab ()
                return tab2
            }
        }
    }

    override fun getCount(): Int {
        return mNumOfTabs
    }
}