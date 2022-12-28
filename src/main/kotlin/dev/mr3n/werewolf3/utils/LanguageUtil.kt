package dev.mr3n.werewolf3.utils

import dev.mr3n.werewolf3.WereWolf3
import net.md_5.bungee.api.ChatColor
import org.bukkit.entity.Player


/**
 * languages.ymlから翻訳されたメッセージを取得。
 */
fun languages(key: String, vararg args: Pair<String, Any>): String {
    var message = WereWolf3.LANGUAGES.config()?.getString(key)?:"&cMessage not found"
    args.forEach { message = message.replace(it.first,it.second.toString()) }
    return ChatColor.translateAlternateColorCodes('&',message)
}

/**
 * プラグインのprefix([人狼pvp])付きでメッセーを取得
 */
fun prefixedLang(key: String, vararg args: Pair<String, Any>) = "${languages("prefix")} ${languages(key,*args)}"