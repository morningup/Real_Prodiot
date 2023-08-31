package com.example.prodiot

import java.util.Date

class Step {
    var key: String? = ""
    var title: String = ""
    var code: String = ""
    var author: String = ""
    var input: String = ""
    var timestamp: Date = Date()
    var views: Long = 0

    constructor() {
        // 인자 없는 생성자를 추가하여 Firebase에서 객체로 변환할 수 있도록 합니다.
    }
}

