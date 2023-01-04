package dev.mr3n.werewolf3.items.quickchat

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta

object YouAreWolf: QuickChat("you_are_wolf", Material.LEATHER_HORSE_ARMOR, 10000) {
    override fun onSetItemMeta(itemMeta: ItemMeta) {
        if(itemMeta !is LeatherArmorMeta) { return }
        itemMeta.setColor(Color.RED)
    }
}