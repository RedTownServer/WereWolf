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
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.scheduler.BukkitTask
import java.util.*

object SeerItem: IShopItem.ShopItem(Material.MUSIC_DISC_MALL) {
    override val id: String = "seer"

    override val displayName: String = languages("item.$id.name")

    override val price: Int = 300

    private val SEER_TITLE_TEXT = titleText("item.$id.title.seer")

    // map<占い師,triple<最後クリックした際のタイムスタンプ,対象者,クリックしたミリ秒>>
    private val LAST_CLICKED = mutableMapOf<UUID, SeerInfo>()

    private val SEER_TIME: Long = constant("seer_time")

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
            val seerInfo = LAST_CLICKED[player.uniqueId]
            val isFirst = seerInfo == null
            val currentMillis = System.currentTimeMillis()
            val lastClicked = seerInfo?.clicked?:currentMillis
            val lastTarget = seerInfo?.target?:target
            var length = (seerInfo?.length?:0)+(currentMillis-lastClicked)
            seerInfo?.bukkitTask?.cancel()
            // 長押ししていない/クリックしているプレイヤーが違う場合はクリック時間を0に戻す
            if(currentMillis-lastClicked > 250 || lastTarget!=target) { length = 0 }
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
                    player.sendTitle(SEER_TITLE_TEXT, messages("init", "%player%" to player.name), 0, Int.MAX_VALUE, 0)
                    player.playSound(player,Sound.BLOCK_ENCHANTMENT_TABLE_USE,1f,1f)
                }
                // クリックを離した際の処理
                val bukkitTask = WereWolf3.INSTANCE.runTaskLater(5) {
                    // 占いがキャンセルされた旨を通知
                    player.sendTitle(SEER_TITLE_TEXT, messages("canceled"), 0, 60, 20)
                    // キラリーンの音を鳴らす
                    player.playSound(player,Sound.ENTITY_EXPERIENCE_ORB_PICKUP,1f,1f)
                    // 占い方法をアクションバーに表示
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *TextComponent.fromLegacyText(messages("hint.how_to")))
                    LAST_CLICKED[player.uniqueId]?.bukkitTask?.cancel()
                    LAST_CLICKED.remove(player.uniqueId)
                }
                // if:クリックしている時間が足りない場合
                LAST_CLICKED[player.uniqueId] = SeerInfo(currentMillis,target,length,bukkitTask)
            }
        }
    }

    data class SeerInfo(val clicked: Long, val target: Player, val length: Long, val bukkitTask: BukkitTask)
}