package com.example.prodiot

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.Toolbar

class SignUpName : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin3)

        val moveButton = findViewById<Button>(R.id.btn_next3)
        val regsterName = findViewById<EditText>(R.id.regster_name)

        moveButton.setOnClickListener{
            val sharedPref = getSharedPreferences("my_prefs", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()
            editor.putString("name", regsterName.text.toString())
            editor.apply()
            moveToAnotherPage()
        }
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)	//툴바 사용 설정
    }
    private fun moveToAnotherPage(){
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.left_in, R.anim.left_out)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)		//작성한 메뉴파일 설정
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item!!.itemId){
            R.id.menu_select->{
                Toast.makeText(applicationContext, "선택 이벤트 실행", Toast.LENGTH_LONG).show()
            }
            R.id.menu_info-> {
                val intentMenu = Intent(this, AppUserInfo::class.java)
                startActivity(intentMenu)
                overridePendingTransition(R.anim.left_in, R.anim.left_out)
            }
        }
        return super.onOptionsItemSelected(item)
    }
}
