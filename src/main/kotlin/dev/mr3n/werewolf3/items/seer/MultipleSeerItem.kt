package dev.mr3n.werewolf3.items.seer

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.asPrefixed
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.role
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.*
import org.bukkit.event.player.PlayerInteractEvent
import kotlin.math.cos
import kotlin.math.sin

object MultipleSeerItem: IShopItem.ShopItem("multiple_seer", Material.ENDER_EYE) {
    override val displayName: String = languages("item.$id.name")

    private val SEER_TITLE_TEXT = titleText("item.$id.title.seer")

    private val SEER_TIME: Long = constant("seer_time")

    private val DISTANCE: Double = constant("distance")

    // なんか明るめの紫色みたいなやつ。ChatColor.LIGHT_PURPLE。名前統一してくれ
    private val PARTICLE_COLOR = Particle.DustOptions(Color.FUCHSIA, 1f)

    /**
     * 円を描画する関数
     * centerは円の中心。roughnessは円のパーティクルの粗さ。
     */
    private fun renderCircle(center: Location, roughness: Int) {
        val world = center.world?:return
        (0 until 360 step roughness).forEach { num ->
            // circumference = 円周
            val circumference = center.clone().add(sin(Math.toRadians(num.toDouble())) * DISTANCE, 0.0, cos(Math.toRadians(num.toDouble())) * DISTANCE)
            world.spawnParticle(Particle.REDSTONE, circumference,10,0.0, 0.5, 0.0, PARTICLE_COLOR)
        }
    }

    init {
        WereWolf3.INSTANCE.registerEvent<PlayerInteractEvent> { event ->
            val player = event.player
            val item = player.inventory.itemInMainHand
            // 占いアイテムを手に持っていない場合はreturn
            if(!isSimilar(item)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            val base = player.location.clone()
            event.isCancelled = true
            // 半径 DISTANCE の円のパーティクルを描画する
            renderCircle(base,3)
            base.world?.playSound(base, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 1f)
            item.amount--
            WereWolf3.INSTANCE.runTaskLater(SEER_TIME) {
                val wolfInRange = WereWolf3.PLAYERS.filter { base.distance(it.location) < DISTANCE }.any { it.role == Role.WOLF }
                base.world?.playSound(base, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                if(wolfInRange) {
                    val result = messages("result.wolf")
                    player.sendTitle(SEER_TITLE_TEXT, result, 0, 100, 20)
                    player.sendMessage(result.asPrefixed())
                } else {
                    val result = messages("result.villager")
                    player.sendTitle(SEER_TITLE_TEXT, result, 0, 100, 20)
                    player.sendMessage(result.asPrefixed())
                }
            }
        }
    }
}