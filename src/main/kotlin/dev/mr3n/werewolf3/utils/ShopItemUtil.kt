package dev.mr3n.werewolf3.utils

import net.md_5.bungee.api.ChatColor
import org.bukkit.inventory.ItemStack

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

fun ItemStack.addLore(vararg lore: String): ItemStack {
    this.itemMeta = this.itemMeta?.also { meta ->
        meta.lore = (meta.lore?:mutableListOf()).also { list -> list.addAll(lore.map { "${ChatColor.GRAY}$it" }) }
    }
    return this
}