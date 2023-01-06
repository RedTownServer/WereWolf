package dev.mr3n.werewolf3.items

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta

object HealPotion: IShopItem.ShopItem("heal_potion", Material.POTION) {
    private val HEAL_AMOUNT: Double = constant("heal_amount")

    private val CHARGER_TITLE_TEXT = titleText("item.$id.title.healing")

    override val description: List<String> = languages("item.${id}.description", "%amount%" to HEAL_AMOUNT).split("\n")

    override fun onSetItemMeta(itemMeta: ItemMeta) {
        if(itemMeta !is PotionMeta) { return }
        itemMeta.color = Color.RED
    }

    init {
        WereWolf3.INSTANCE.registerEvent<PlayerItemConsumeEvent> { event ->
            val player = event.player
            val item = event.item
            if(!isSimilar(item)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            player.health = minOf(player.healthScale, player.health + HEAL_AMOUNT)
            player.sendTitle(CHARGER_TITLE_TEXT, messages("healing", "%amount%" to HEAL_AMOUNT), 0, 60, 20)
            player.inventory.itemInMainHand.amount--
        }
    }
}