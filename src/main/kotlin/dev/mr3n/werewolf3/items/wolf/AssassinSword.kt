package dev.mr3n.werewolf3.items.wolf

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent

object AssassinSword: IShopItem.ShopItem(Material.IRON_SWORD) {
    override val id: String = "assassin_sword"

    override val displayName: String = languages("item.$id.name")

    override val price: Int = 300

    private val SUCCESS_TITLE_TEXT = titleText("item.$id.title.assassin_success")

    private val FAILED_TITLE_TEXT = titleText("item.$id.title.assassin_failed")

    private val ATTACK_ANGLE: Double = constant("attack_angle")

    init {
        WereWolf3.INSTANCE.registerEvent<EntityDamageByEntityEvent> { event ->
            val player = event.damager
            val target = event.entity
            if(player !is Player) { return@registerEvent }
            if(target !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(target)) { return@registerEvent }
            val item = player.inventory.itemInMainHand
            if(!isSimilar(item)) { return@registerEvent }
            val direction = target.location.toVector().subtract(target.location.toVector())
            val lookAtTarget = player.location.clone()
            lookAtTarget.direction = direction
            Bukkit.broadcastMessage(lookAtTarget.yaw.toString())

        }
    }
}