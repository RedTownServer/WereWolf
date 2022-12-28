package dev.mr3n.werewolf3.utils

import net.md_5.bungee.api.ChatColor

fun String.toTitleText(color: Any): String {
    val text = StringBuilder()
    text.append("$color${ChatColor.BOLD}${ChatColor.MAGIC}~ ")
    text.append("$color${ChatColor.BOLD}${ChatColor.stripColor(this)}")
    text.append("$color${ChatColor.BOLD}${ChatColor.MAGIC} ~")
    return ChatColor.translateAlternateColorCodes('&',text.toString())
}

fun titleText(path: String): String {
    return languages("${path}.title").toTitleText(languages("${path}.color"))
}