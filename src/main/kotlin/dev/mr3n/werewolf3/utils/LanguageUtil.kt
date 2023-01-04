package dev.mr3n.werewolf3.utils

import dev.mr3n.werewolf3.WereWolf3
import net.md_5.bungee.api.ChatColor


/**
 * languages.ymlから翻訳されたメッセージを取得。
 */
fun languages(key: String, vararg args: Pair<String, Any>): String {
    var message = WereWolf3.LANGUAGES.config()?.getString(key)?:"&cMessage not found"
    args.forEach { message = message.replace(it.first,it.second.toString()) }
    return ChatColor.translateAlternateColorCodes('&',message)
}

fun String.asPrefixed() = "${languages("prefix")} $this"