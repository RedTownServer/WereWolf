package dev.mr3n.werewolf3.items.medium

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.events.WereWolf3DeadBodyClickEvent
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.protocol.DeadBody
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitTask
import java.util.*

object MediumItem: IShopItem.ShopItem("medium", Material.MUSIC_DISC_WAIT) {
    override val displayName: String = languages("item.$id.name")

    private val MEDIUM_TITLE_TEXT = titleText("item.$id.title.medium")

    // map<占い師,triple<最後クリックした際のタイムスタンプ,対象者,クリックしたミリ秒>>
    private val LAST_CLICKED = mutableMapOf<UUID, MediumInfo>()

    private val MEDIUM_TIME: Long = constant("medium_time")

    init {
        WereWolf3.INSTANCE.registerEvent<WereWolf3DeadBodyClickEvent> { event ->
            val player = event.player
            val item = player.inventory.itemInMainHand
            // 占いアイテムを手に持っていない場合はreturn
            if(!isSimilar(item)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            val target = event.deadBody
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
            if(length >= MEDIUM_TIME * 50) {
                // if:3秒以上押し続けている場合
                LAST_CLICKED.remove(player.uniqueId)
                item.amount--
                seerInfo?.bukkitTask?.cancel()
                player.sendTitle(MEDIUM_TITLE_TEXT, messages("success"), 0, 100, 20)
                player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                player.sendMessage(messages("header"))
                val role = event.deadBody.role
                player.sendMessage(messages("team", "%team%" to "${role?.team?.color}${role?.team?.displayName}"))
                player.sendMessage(messages("time", "%sec%" to (System.currentTimeMillis()-event.deadBody.time) / 1000))
                player.sendMessage(messages("will", "%will%" to event.deadBody.will))

            } else {
                // 初めてクリックしていた場合
                if(isFirst) {
                    player.sendTitle(MEDIUM_TITLE_TEXT, messages("init", "%player%" to event.deadBody.name), 0, Int.MAX_VALUE, 0)
                    player.playSound(player,Sound.BLOCK_ENCHANTMENT_TABLE_USE,1f,1f)
                }
                // クリックを離した際の処理
                val bukkitTask = WereWolf3.INSTANCE.runTaskLater(6) {
                    // 占いがキャンセルされた旨を通知
                    player.sendTitle(MEDIUM_TITLE_TEXT, messages("canceled"), 0, 60, 20)
                    // キラリーンの音を鳴らす
                    player.playSound(player,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f)
                    LAST_CLICKED[player.uniqueId]?.bukkitTask?.cancel()
                    LAST_CLICKED.remove(player.uniqueId)
                }
                // if:クリックしている時間が足りない場合
                LAST_CLICKED[player.uniqueId] = MediumInfo(currentMillis,target,length,bukkitTask)
            }
            event.isCancelled = true
        }
    }

    data class MediumInfo(val clicked: Long, val target: DeadBody, val length: Long, val bukkitTask: BukkitTask)
}