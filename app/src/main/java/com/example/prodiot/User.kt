package com.example.prodiot

class User {
    var name: String = ""
    var imageUrl: String = "" // Change the property type to String

    constructor() {
        // 인자 없는 생성자를 추가하여 Firebase에서 객체로 변환할 수 있도록 합니다.
    }
}