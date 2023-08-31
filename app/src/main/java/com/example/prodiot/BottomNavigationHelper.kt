package com.example.prodiot

import android.content.Context
import android.content.Intent
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView

class BottomNavigationHelper(private val context: Context, private val navigationView: BottomNavigationView) {

    init {
        navigationView.setOnNavigationItemSelectedListener(::onNavigationItemSelected)
    }
    private fun onNavigationItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.btn_home -> {
                startActivity(MainMenu::class.java)
                true
            }
            R.id.btn_freeboard -> {
                startActivity(FreeBoardList::class.java)
                true
            }
            R.id.btn_codestep -> {
                startActivity(CodeStepList::class.java)
                true
            }
            R.id.menu_info -> {
                startActivity(AppUserInfo::class.java)
                true
            }
            else -> false
        }
    }
    private fun startActivity(cls: Class<*>) {
        val intent = Intent(context, cls)
        context.startActivity(intent)
    }
}
