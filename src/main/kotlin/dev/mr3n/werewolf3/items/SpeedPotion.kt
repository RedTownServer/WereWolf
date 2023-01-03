package dev.mr3n.werewolf3.items

import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object SpeedPotion: IShopItem.ShopItem("speed_potion", Material.POTION) {
    private val TIME: Int = constant("time")

    private val LEVEL: Int = constant("level")

    private val INVISIBLE_TITLE_TEXT = titleText("item.$id.title.speed")

    override val description: String = languages("item.$id.description", "%time%" to TIME / 20)

    override fun onSetItemMeta(itemMeta: ItemMeta) {
        if(itemMeta !is PotionMeta) { return }
        itemMeta.color = Color.BLUE
        itemMeta.itemFlags.add(ItemFlag.HIDE_POTION_EFFECTS)
    }

    init {
        WereWolf3.INSTANCE.runTaskTimer(20L,20L) {
            WereWolf3.PLAYERS.filter { it.hasPotionEffect(PotionEffectType.INVISIBILITY) }.forEach { player ->
                player.sendTitle(INVISIBLE_TITLE_TEXT, messages("remaining_time", "%time%" to (player.getPotionEffect(PotionEffectType.INVISIBILITY)?.duration?.div(20)?:-1)), 0, 30, 0)
            }
        }
        WereWolf3.INSTANCE.registerEvent<PlayerItemConsumeEvent> { event ->
            val player = event.player
            val item = event.item
            if(!isSimilar(item)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, TIME, LEVEL, false, false, true))
            player.inventory.itemInMainHand.amount--
        }
    }
}