package dev.mr3n.werewolf3.utils

fun Int.parseTime(): String {
    val minutes = this/60
    val seconds =  this%60
    return "${minutes}分${seconds}秒"
}