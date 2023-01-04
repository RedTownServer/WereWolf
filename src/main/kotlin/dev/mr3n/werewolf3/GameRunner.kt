package dev.mr3n.werewolf3

import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot
import dev.moru3.minepie.Executor.Companion.runTaskAsync
import dev.mr3n.werewolf3.protocol.DeadBody
import dev.mr3n.werewolf3.protocol.InvisibleEquipmentPacketUtil
import dev.mr3n.werewolf3.protocol.MetadataPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.DeathSidebar
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.RunningSidebar
import dev.mr3n.werewolf3.utils.*
import org.bukkit.GameMode

object GameRunner {
    fun running(loopCount: Int) {
        WereWolf3.PLAYERS.forEach { player ->
            // サイドバーの情報を更新する
            val sidebar = player.sidebar
            if(sidebar is RunningSidebar) {
                sidebar.playersEst(WereWolf3.PLAYERS_EST)
                sidebar.money(player.money)
            }
            if(sidebar is DeathSidebar) {
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
                if (loopCount % 2 == 0) {
                    WereWolf3.INSTANCE.runTaskAsync {
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
                                InvisibleEquipmentPacketUtil.remove(player, player2, 0)
                                MetadataPacketUtil.removeFromInvisible(player, player2)
                            } else {
                                InvisibleEquipmentPacketUtil.add(player, player2, 0, *ItemSlot.values())
                                MetadataPacketUtil.addToInvisible(player, player2)
                            }
                        }
                        val visibleDeadBodies = DeadBody.DEAD_BODIES
                            .filterNot { deadBody ->
                                player.location.clone().add(0.0, 1.6, 0.0).hasObstacleInPath(deadBody.location.clone())
                            }
                        DeadBody.DEAD_BODIES.forEach { deadBody ->
                            if(visibleDeadBodies.contains(deadBody)) {
                                deadBody.show(listOf(player))
                            } else {
                                deadBody.hide(listOf(player))
                            }
                        }
                    }
                }
            }
        }
    }
}