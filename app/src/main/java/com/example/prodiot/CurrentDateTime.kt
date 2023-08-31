package com.example.prodiot

import java.text.SimpleDateFormat
import java.util.*

fun CurrentDateTime(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date())
}
