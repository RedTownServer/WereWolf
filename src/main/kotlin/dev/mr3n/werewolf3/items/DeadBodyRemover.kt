package dev.mr3n.werewolf3.items

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.events.WereWolf3DeadBodyClickEvent
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Sound

object DeadBodyRemover: IShopItem.ShopItem("dead_body_remover", Material.FLINT_AND_STEEL) {
    private val REMOVER_TITLE_TEXT = titleText("item.$id.title.remover")

    init {
        WereWolf3.INSTANCE.registerEvent<WereWolf3DeadBodyClickEvent> { event ->
            val player = event.player
            val item = player.inventory.itemInMainHand
            if(!isSimilar(item)) { return@registerEvent }
            item.amount--
            player.world.playSound(player, Sound.ITEM_FLINTANDSTEEL_USE, 1f, 1f)
            player.world.playSound(player, Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f)
            player.sendTitle(REMOVER_TITLE_TEXT, messages("subtitle", "%player%" to event.deadBody.name), 0, 100, 20)
            event.deadBody.destroy()
            event.isCancelled = true
        }
    }
}