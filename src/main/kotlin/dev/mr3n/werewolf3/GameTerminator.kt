package dev.mr3n.werewolf3

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.mr3n.werewolf3.citizens2.DeadBody
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.protocol.GlowPacketUtil
import dev.mr3n.werewolf3.protocol.TeamPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.WaitingSidebar
import dev.mr3n.werewolf3.utils.co
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.prefixedLang
import dev.mr3n.werewolf3.utils.role
import org.bukkit.*

object GameTerminator {

    fun end(win: Role.Faction, reason: String) {
        if(WereWolf3.STATUS==Status.ENDING||WereWolf3.STATUS==Status.WAITING) { return }
        WereWolf3.STATUS = Status.ENDING
        val players = Role.ROLES.map { it.key to it.value.map { uniqueId -> Bukkit.getOfflinePlayer(uniqueId) }.map { p -> p.name } }
        WereWolf3.PLAYERS.forEach { player ->
            player.sendTitle(languages("title.win.title", "%role%" to win.displayName, "%color%" to win.color), reason, 20, 100, 20)
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
            player.sendMessage("${languages("messages.result.prefix")}${languages("messages.result.header")}")
            players.forEach s@{  (role, players) ->
                if(players.isEmpty()) { return@s }
                player.sendMessage("${languages("messages.result.prefix")}${role.displayName}: ${players.joinToString(" ")}")
            }
            player.sendMessage("${languages("messages.result.prefix")}${languages("messages.result.header")}")
            player.sendMessage(prefixedLang("messages.result.winner", "%faction%" to win.displayName, "%color%" to win.color))
        }
        this.run()
    }
    fun run() {
        WereWolf3.STATUS = Status.WAITING
        WereWolf3.GAME_ID = null

        // 発光、チームをリセット
        WereWolf3.PLAYERS.forEach { player ->
            player.flySpeed = 0.2f
            player.walkSpeed = 0.2f
            ChatColor.values().forEach { color -> TeamPacketUtil.remove(player, color, WereWolf3.PLAYERS.map { it.name }) }
            WereWolf3.PLAYERS.forEach { player2 -> GlowPacketUtil.remove(player, player2) }
            if(player.role==Role.WOLF) { TeamPacketUtil.removeTeam(player, ChatColor.DARK_RED) }
            player.setDisplayName(player.name)
            player.setPlayerListName(player.name)
            player.role = null
            player.co = null
            player.inventory.clear()
            player.sidebar = WaitingSidebar()
            player.gameMode = GameMode.ADVENTURE
            player.teleport(player.world.spawnLocation)
        }
        // 死体を全削除
        DeadBody.DEAD_BODIES.forEach { it.destroy() }
        WereWolf3.INSTANCE.runTaskLater(100) {
            WereWolf3.STATUS = Status.WAITING
        }
        // TODO ハードコーディングしてるのでいつか修正するいつか
        IShopItem.ShopItem.ITEMS.forEach { it.onEnd() }
    }
}