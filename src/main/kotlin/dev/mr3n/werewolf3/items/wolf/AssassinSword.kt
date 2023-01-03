package dev.mr3n.werewolf3.items.wolf

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.utils.damageTo
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import kotlin.math.abs

object AssassinSword: IShopItem.ShopItem("assassin_sword", Material.IRON_SWORD) {
    override val displayName: String = languages("item.$id.name")

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
            val direction = target.location.toVector().subtract(player.location.toVector())
            val lookAtTarget = player.location.clone().setDirection(direction)
            var yaw = lookAtTarget.yaw
            if(yaw > 180) { yaw -= 360 }
            val targetYaw = target.location.yaw
            // しっかりと背後から殴ったかどうかを判定
            val success = if(abs(targetYaw) > 180 - ATTACK_ANGLE) {
                val pos: ClosedFloatingPointRange<Double>
                val neg: ClosedFloatingPointRange<Double>
                if(targetYaw > 0) {
                    // if:yawが正の値の場合
                    pos = targetYaw-ATTACK_ANGLE..180.0
                    neg = -180.0..targetYaw-(360-ATTACK_ANGLE)
                } else {
                    // if:yawが負の値の場合
                    neg = -180.0..targetYaw+ATTACK_ANGLE
                    pos = targetYaw+(360-ATTACK_ANGLE)..180.0
                }
                yaw in pos || yaw in neg
            } else {
                yaw in (targetYaw-ATTACK_ANGLE)..(targetYaw+ATTACK_ANGLE)
            }
            if(success) {
                // if:背後から殴っていた場合
                item.amount--
                player.damageTo(target, -1.0)
                player.sendTitle(SUCCESS_TITLE_TEXT,messages("success"),0,60,20)
            } else {
                // if:背後から殴っていなかった場合
                player.sendTitle(FAILED_TITLE_TEXT,messages("need_from_back"),0,60,20)
                player.world.playSound(player,Sound.ENTITY_ITEM_BREAK,1f,1f)
            }
            event.isCancelled = true
        }
    }
}