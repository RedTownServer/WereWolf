package dev.mr3n.werewolf3.items

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object StanBall: IShopItem.ShopItem("stan_ball", Material.SNOWBALL) {
    override val displayName: String = languages("item.${id}.name")

    private const val ENTITY_TYPE = "STAN_BALL"

    private val STAN_RADIUS: Int = constant("radius")
    private val STAN_TIME: Int = constant("stan_time")

    private val STAN_TITLE_TEXT = titleText("item.${id}.title.stan")

    private val stanPlayers = mutableListOf<Player>()

    init {
        WereWolf3.INSTANCE.registerEvent<ProjectileLaunchEvent> { event ->
            val projectile = event.entity
            val shooter = projectile.shooter?:return@registerEvent
            if(shooter !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(shooter)) { return@registerEvent }
            if(!isSimilar(shooter.inventory.itemInMainHand)) { return@registerEvent }
            // 爆発玉識別用のタグを付与
            projectile.persistentDataContainer.set(Keys.ENTITY_TYPE, PersistentDataType.STRING, ENTITY_TYPE)
        }
        WereWolf3.INSTANCE.registerEvent<PlayerMoveEvent> { event ->
            if(stanPlayers.contains(event.player)) {
                event.player.teleport(event.player.location.also { it.pitch=-90f;it.yaw=0f })
            }
        }
        WereWolf3.INSTANCE.registerEvent<ProjectileHitEvent> { event ->
            val projectile = event.entity
            // あたった発射物が爆発玉じゃない場合はreturn
            if(projectile.persistentDataContainer.get(Keys.ENTITY_TYPE, PersistentDataType.STRING) != ENTITY_TYPE) { return@registerEvent }
            val shooter = projectile.shooter?:return@registerEvent
            if(shooter !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(shooter)) { return@registerEvent }
            // 着弾点
            val location = projectile.location.clone()
            // worldがnullableなためnullではないことを保証する
            val world = location.world?:return@registerEvent
            // まれに発射物が二回当たった判定になる場合があるので発射物を削除
            projectile.remove()
            event.isCancelled = true

            world.spawnParticle(Particle.EXPLOSION_HUGE, location, 30, .0, .0, .0)
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)

            WereWolf3.PLAYERS.forEach { player ->
                when(player.location.distance(location).toInt()) {
                    in 0..STAN_RADIUS -> {
                        player.flySpeed = -1f
                        player.walkSpeed = -1f
                        player.addPotionEffect(PotionEffect(PotionEffectType.JUMP, Int.MAX_VALUE, 250, false, false, false))
                        player.playSound(player, Sound.ITEM_TOTEM_USE, 1F,0.7F)
                        player.sendTitle(STAN_TITLE_TEXT, messages("stan"), 0, STAN_TIME, 10)
                        stanPlayers.add(player)
                        player.teleport(player.location.also { it.pitch=-90f;it.yaw=0f })
                        WereWolf3.INSTANCE.runTaskLater(STAN_TIME.toLong()) {
                            player.flySpeed = 0.2f
                            player.walkSpeed = 0.2f
                            player.removePotionEffect(PotionEffectType.JUMP)
                            stanPlayers.remove(player)
                        }
                    }
                }
            }
        }
    }
}