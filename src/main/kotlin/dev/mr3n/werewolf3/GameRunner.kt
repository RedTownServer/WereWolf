package dev.mr3n.werewolf3

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot
import dev.mr3n.werewolf3.protocol.InvisiblePacketUtil
import dev.mr3n.werewolf3.protocol.MetadataPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.DeadSidebar
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.RunningSidebar
import dev.mr3n.werewolf3.utils.*
import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.GameMode

object GameRunner {
    fun running(loopCount: Int) {
        WereWolf3.PLAYERS.forEach { player ->
            // サイドバーの情報を更新する
            val sidebar = player.sidebar
            if(sidebar is RunningSidebar) {
                sidebar.playersEst(WereWolf3.REMAINING_PLAYER_EST)
                sidebar.money(player.money)
            }
            if(sidebar is DeadSidebar) {
                sidebar.players(WereWolf3.PLAYERS.count { it.gameMode != GameMode.SPECTATOR })
            }
            if(player.gameMode != GameMode.SPECTATOR) {
                // プレイヤーのヘルメットを取得
                val helmet = player.inventory.helmet
                // ヘルメットのCoの役職を取得。nullだった場合はreturn
                val coRole = helmet?.getContainerValue(Role.HELMET_ROLE_TAG_KEY, Role.RoleTagType)
                // まだCoしていない役職だった場合
                if (player.co != coRole) {
                    if (coRole == null) {
                        player.setDisplayName(player.name)
                        player.setPlayerListName(player.name)
                        // 何をcoしたかをほぞん
                        player.co = null
                    } else {
                        // すべてのプレイヤーにCoした旨を伝える。
                        WereWolf3.PLAYERS.forEach {
                            it.sendMessage(languages("messages.coming_out", "%color%" to coRole.color, "%player%" to player.name, "%role%" to coRole.displayName))
                        }
                        // プレイヤーのprefixにCoした役職を表示
                        player.setDisplayName("${coRole.color}[${coRole.displayName}Co]${player.name}")
                        player.setPlayerListName("${coRole.color}[${coRole.displayName}Co]${player.name}")
                        // 何をcoしたかをほぞん
                        player.co = coRole
                    }
                }
                // 30秒おきにお金を追加する
                if (loopCount % (20 * 30) == 0) { player.money += Constants.ADD_MONEY }
                // 1秒おきに人狼に仲間の人狼の名前をアクションバーで通知する
                if (player.role == Role.WOLF) { player.spigot().sendMessage(ChatMessageType.ACTION_BAR, *TextComponent.fromLegacyText(languages("messages.wolf_actionbar", "%wolfs%" to WereWolf3.PLAYERS.filter { it.role == Role.WOLF }.joinToString(" ") { it.name }))) }
                if (loopCount % 4 == 0) {
                    val visiblePlayers = WereWolf3.PLAYERS
                        .filter { player2 -> player2 != player }
                        .filter { player2 -> player2.gameMode != GameMode.SPECTATOR }
                        .filterNot { player2 ->
                            player.location.clone().add(0.0, 1.6, 0.0)
                                .hasObstacleInPath(player2.location.clone().add(0.0, 1.8, 0.0))
                        }
                    WereWolf3.PLAYERS.forEach s@{ player2 ->
                        if (player.role == Role.WOLF && player2.role == Role.WOLF) { return@s }
                        if (visiblePlayers.contains(player2)) {
                            InvisiblePacketUtil.remove(player, player2, 0)
                            MetadataPacketUtil.removeFromInvisible(player, player2)
                        } else {
                            InvisiblePacketUtil.add(player, player2, 0, *ItemSlot.values())
                            MetadataPacketUtil.addToInvisible(player, player2)
                        }
                    }
                }
            }
        }
    }
}