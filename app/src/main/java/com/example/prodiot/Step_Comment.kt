package com.example.prodiot

// 댓글 데이터 클래스
class Step_Comment(
    var key: String? = null, // 댓글 ID
    val stepId: String? = "", // 게시물 ID
    val content: String = "", // 댓글 내용
    val author: String = "", // 작성자
    val time: String = "", // 작성 시간
    val parent: String? = null, // 상위 댓글 ID (댓글의 답글인 경우)
    val depth: Int = 0 // 댓글의 단계 (댓글의 답글인 경우 1, 댓글의 답글의 답글인 경우 2)
) {

    // 인자 없는 생성자 추가
    constructor() : this("", "", "", "", "", null, 0)
}
