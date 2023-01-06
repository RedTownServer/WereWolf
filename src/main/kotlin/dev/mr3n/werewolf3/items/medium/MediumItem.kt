package dev.mr3n.werewolf3.items.medium

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.events.WereWolf3DeadBodyClickEvent
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Sound

object MediumItem: IShopItem.ShopItem("medium", Material.MUSIC_DISC_WAIT) {
    override val displayName: String = languages("item.$id.name")

    private val MEDIUM_TITLE_TEXT = titleText("item.$id.title.medium")

    init {
        WereWolf3.INSTANCE.registerEvent<WereWolf3DeadBodyClickEvent> { event ->
            val player = event.player
            val item = player.inventory.itemInMainHand
            // 占いアイテムを手に持っていない場合はreturn
            if(!isSimilar(item)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            val target = event.deadBody
            event.isCancelled = true
            item.amount--
            player.sendTitle(MEDIUM_TITLE_TEXT, messages("success"), 0, 100, 20)
            player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
            player.sendMessage(messages("header"))
            val role = target.role
            player.sendMessage(messages("team", "%team%" to "${role?.team?.color}${role?.team?.displayName}"))
            player.sendMessage(messages("time", "%sec%" to (System.currentTimeMillis() - target.time) / 1000))
            player.sendMessage(messages("will", "%will%" to "\"${target.will}\""))
        }
    }
}