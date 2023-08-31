package com.example.prodiot

import OptionMenuHandler
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.*


class CodeStepList : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var bottomNavigationHelper: BottomNavigationHelper
    private lateinit var recyclerView: RecyclerView
    private lateinit var optionMenuHandler: OptionMenuHandler
    private lateinit var editTextSearch: EditText
    private lateinit var buttonSearch: Button
    private lateinit var buttonList: Button
    private lateinit var originalItems: List<Step>
    private lateinit var filteredItems: MutableList<Step>
    private lateinit var adapter: codestepadapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_codesteplist)
        // 툴바 설정
        setSupportActionBar(findViewById(R.id.toolbar))

        // 네비게이션 아이템 클릭 리스너 설정
        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationHelper = BottomNavigationHelper(this, bottomNavigationView)

// 리사이클러뷰 설정
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 검색 기능을 위한 UI 요소 초기화
        editTextSearch = findViewById(R.id.editTextSearch)
        buttonSearch = findViewById(R.id.buttonSearch)
        buttonList = findViewById(R.id.buttonList)

        // 파이어베이스 데이터베이스 초기화
        val firebaseDatabase = FirebaseDatabase.getInstance()
        val databaseReference = firebaseDatabase.reference.child("steps")
        // 파이어베이스 데이터베이스에서 데이터 가져오기
        databaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val allItems = mutableListOf<Step>()
                for (stepSnapshot in dataSnapshot.children) {
                    val step = stepSnapshot.getValue(Step::class.java)
                    step?.let {
                        val key = stepSnapshot.key // 아이템의 키값 가져오기
                        it.key = key // Step 객체에 키값 저장
                        allItems.add(it)
                    }
                }
                val adapter = codestepadapter(allItems)
                recyclerView.adapter = adapter
            }
            override fun onCancelled(databaseError: DatabaseError) {
                // 오류 처리 (선택사항)
            }
        })

        // 검색 버튼 클릭 시 검색 기능 수행
        buttonSearch.setOnClickListener {
            val query = editTextSearch.text.toString().trim()
            searchSteps(query)
        }

        // 검색 버튼 클릭 시 검색 기능 수행
        buttonList.setOnClickListener {
            buttonList.setOnClickListener {
                // 전체 게시물 목록을 filteredItems에 대입
                filteredItems.clear()
                filteredItems.addAll(originalItems)

                // 어댑터에 변경된 목록을 알려줌
                adapter.notifyDataSetChanged()
            }
        }

        // 페이지 이동 함수
        val moveToAnotherPage = { destination: Class<*> ->
            startActivity(Intent(this, destination))
            overridePendingTransition(R.anim.left_in, R.anim.left_out)
        }
        // 이벤트 핸들러
        findViewById<Button>(R.id.btn_write_test).setOnClickListener {
            moveToAnotherPage(CodeStepWrite::class.java)
        }

        // OptionMenuHandler 초기화
        optionMenuHandler = OptionMenuHandler(this)
    }
    // 옵션 메뉴 생성
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.mainmenu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return optionMenuHandler.handleItemSelected(item) || super.onOptionsItemSelected(item)
    }

    //검색부분
    private fun searchSteps(query: String) {
        filteredItems.clear() // 리스트 초기화
        if (query.isEmpty()) { // 검색어 부분이 비어있는지 확인
            filteredItems.addAll(originalItems) //비어있다면 모든 리스트값들 불러오기
        } else {// 아니라면
            val lowerCaseQuery = query.toLowerCase(Locale.getDefault()) // 대소문 구분없이 전부다 소문자처리
            //LOCALE.GETDEFAULT는 쿼리의 데이터형식을 기본값을 읽어오기
            for (step in originalItems) { // 이후 반복하여 해당 쿼리에 맞는것들을 리스트에 담아줌
                if (step.title.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) || //제목이 같거나
                    step.author.toLowerCase(Locale.getDefault()).contains(lowerCaseQuery) // 내용이 같거나
                ) {
                    filteredItems.add(step) // 해당한다면 리스트에 추가
                }
            }
        }
        adapter.notifyDataSetChanged() // 해당 결과값으로 어댑터의 리사이클러뷰 리스트 초기화
    }
}
