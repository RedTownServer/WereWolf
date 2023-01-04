package dev.mr3n.werewolf3.items.quickchat

import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.LeatherArmorMeta

object WithYou: QuickChat("with_you", Material.LEATHER_HORSE_ARMOR, 10000) {
    override fun onSetItemMeta(itemMeta: ItemMeta) {
        if(itemMeta !is LeatherArmorMeta) { return }
        itemMeta.setColor(Color.GRAY)
    }
}