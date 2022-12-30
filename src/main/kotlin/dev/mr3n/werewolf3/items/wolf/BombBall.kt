package dev.mr3n.werewolf3.items.wolf

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.utils.damageTo
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object BombBall: IShopItem.ShopItem(Material.SNOWBALL) {
    override val id: String = "bomb_ball"

    override val displayName: String = languages("item.$id.name")

    override val price: Int = 300


    private val WARNING_TITLE_TEXT = titleText("item.$id.title.warning")

    private const val ENTITY_TYPE = "BOMB_BALL"

    private val DAMAGE_DISTANCE: Int = constant("damage_distance")
    private val DEATH_DISTANCE: Int = constant("death_distance")
    private val FUSE_TIME: Long = constant("fuse_time")
    private val MAX_DAMAGE: Int = constant("max_damage")
    private val WARNING_COUNT: Int = constant("warning_count")

    /**
     * 距離から爆発玉の爆発ダメージを計算します。
     */
    private fun calcDamage(distance: Double): Double {
        return if(distance > DAMAGE_DISTANCE) {
            // if:ダメージ範囲の外にいる場合は0
            0.0
        } else if(distance< DEATH_DISTANCE) {
            // if:死亡範囲内にいる場合は100万ダメージを与える
            1000000.0
        } else {
            // 死亡範囲を除いたダメージ範囲を計算。
            val damageDistance = DAMAGE_DISTANCE - DEATH_DISTANCE
            // 最大ダメージが範囲内でmaxDamageにになるように係数を計算
            val coefficient = MAX_DAMAGE / damageDistance
            // 距離を反転させて係数をかける。
            (damageDistance - ((distance / 2) - DEATH_DISTANCE)) * coefficient
        }
    }

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
            // TNTのアイテムをワールドにスポーン
            val tnt = world.spawn(location, Item::class.java)
            // アイテムの内容をTNTに変更
            tnt.itemStack = ItemStack(Material.TNT)
            // アイテムを拾えないようにする
            tnt.pickupDelay = Int.MAX_VALUE
            // アイテムが消えないようにする
            tnt.isUnlimitedLifetime = true
            // TNTの着火音で警告
            world.playSound(location, Sound.ENTITY_TNT_PRIMED, 1f, 1f)
            repeat(WARNING_COUNT) { count ->
                WereWolf3.INSTANCE.runTaskLater((FUSE_TIME / (WARNING_COUNT)) * count) {
                    WereWolf3.PLAYERS.filter { it.location.distance(location) < DAMAGE_DISTANCE }.forEach { player -> player.sendTitle(
                        WARNING_TITLE_TEXT, "", 0, 0, (FUSE_TIME / 4).toInt()) }
                    world.playSound(location, Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1.8f)
                }
            }
            WereWolf3.INSTANCE.runTaskLater(FUSE_TIME) {
                tnt.remove()
                world.spawnParticle(Particle.EXPLOSION_HUGE, location, 30, .0, .0, .0)
                world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f)
                WereWolf3.PLAYERS.forEach { player ->
                    // 着弾点とプレイヤーの距離
                    val distance = location.distance(player.location)
                    // shooterがプレイヤーにダメージを与えている判定にする
                    shooter.damageTo(player, calcDamage(distance))
                }
            }
        }
    }
}