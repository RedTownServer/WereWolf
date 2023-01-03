package dev.mr3n.werewolf3.items

import dev.mr3n.werewolf3.utils.languages
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta

object StoneSword: IShopItem.ShopItem("stone_sword", Material.STONE_SWORD) {
    override val displayName: String = languages("item.${id}.name")

    override fun onSetItemMeta(itemMeta: ItemMeta) {
        if(itemMeta !is Damageable) { return }
        itemMeta.isUnbreakable = true
        itemMeta.addEnchant(Enchantment.DAMAGE_ALL, 2, false)
    }
}