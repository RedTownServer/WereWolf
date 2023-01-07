package dev.mr3n.werewolf3.items.seer

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Time
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.*
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*

object SeerItem: IShopItem.ShopItem("seer", Material.MUSIC_DISC_CHIRP) {
    override val displayName: String = languages("item.$id.name")

    private val SEER_TITLE_TEXT = titleText("item.$id.title.seer")

    // map<占い師,triple<最後クリックした際のタイムスタンプ,対象者,クリックしたミリ秒>>
    private val LAST_CLICKED = mutableMapOf<UUID, SeerInfo>()

    private val SEER_TIME: Long = constant("seer_time")

    override fun buy(player: Player): Boolean {
        if(player.isBE) {
            player.sendMessage(languages("be_cannot_use_item"))
            return false
        }
        return super.buy(player)
    }

    init {
        WereWolf3.INSTANCE.registerEvent<PlayerInteractAtEntityEvent> { event ->
            val player = event.player
            val item = player.inventory.itemInMainHand
            // 占いアイテムを手に持っていない場合はreturn
            if(!isSimilar(item)) { return@registerEvent }
            val target = event.rightClicked
            if(target !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(target)) { return@registerEvent }
            if(WereWolf3.TIME==Time.NIGHT) {
                // 時間が夜だった場合
                val seerInfo = LAST_CLICKED[player.uniqueId]
                var isFirst = seerInfo == null
                val currentMillis = System.currentTimeMillis()
                val lastClicked = seerInfo?.clicked?:currentMillis
                val lastTarget = seerInfo?.target?:target
                var length = (seerInfo?.length?:0)+(currentMillis-lastClicked)
                seerInfo?.bukkitTask?.cancel()
                // 長押ししていない/クリックしているプレイヤーが違う場合はクリック時間を0に戻す
                if(currentMillis-lastClicked > 280 || lastTarget!=target) {
                    isFirst = true
                    length = 0
                }
                if(length >= SEER_TIME * 50) {
                    // if:3秒以上押し続けている場合
                    LAST_CLICKED.remove(player.uniqueId)
                    item.amount--
                    seerInfo?.bukkitTask?.cancel()
                    val result = if(target.role==Role.WOLF) messages("result.wolf", "%player%" to target.name) else messages("result.villager", "%player%" to target.name)
                    player.sendTitle(SEER_TITLE_TEXT, result, 0, 100, 20)
                    player.sendMessage(result.asPrefixed())
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                } else {
                    // 初めてクリックしていた場合
                    if(isFirst) {
                        player.sendTitle(SEER_TITLE_TEXT, messages("init", "%player%" to target.name), 0, Int.MAX_VALUE, 0)
                        player.playSound(player,Sound.BLOCK_ENCHANTMENT_TABLE_USE,1f,1f)
                    }
                    // クリックを離した際の処理
                    val bukkitTask = WereWolf3.INSTANCE.runTaskLater(6) {
                        // 占いがキャンセルされた旨を通知
                        player.sendTitle(SEER_TITLE_TEXT, messages("canceled"), 0, 60, 20)
                        // キラリーンの音を鳴らす
                        player.playSound(player,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f)
                        LAST_CLICKED[player.uniqueId]?.bukkitTask?.cancel()
                        LAST_CLICKED.remove(player.uniqueId)
                    }
                    // if:クリックしている時間が足りない場合
                    LAST_CLICKED[player.uniqueId] = SeerInfo(currentMillis,target,length,bukkitTask)
                }
            } else {
                // if:時間が朝だった場合
                // 夜のみ使用できることを伝える
                player.sendTitle(SEER_TITLE_TEXT, messages("night_only"), 0, 60, 20)
            }
        }
    }

    data class SeerInfo(val clicked: Long, val target: Player, val length: Long, val bukkitTask: BukkitTask)
}