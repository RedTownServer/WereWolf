package dev.mr3n.werewolf3

import com.rylinaux.plugman.util.PluginUtil
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
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Sound

object GameTerminator {

    fun end(win: Role.Faction, reason: String) {
        if(WereWolf3.STATUS==Status.ENDING||WereWolf3.STATUS==Status.WAITING) { return }
        WereWolf3.STATUS = Status.ENDING
        val players = Role.ROLES.map { it.key to it.value.map { uniqueId -> Bukkit.getOfflinePlayer(uniqueId) }.map { p -> p.name } }
        WereWolf3.PLAYERS.forEach { player ->
            repeat(20) {
                player.sendMessage("\n")
            }
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

    fun run(shutdown: Boolean = false) {
        WereWolf3.STATUS = Status.WAITING
        WereWolf3.GAME_ID = null

        IShopItem.ShopItem.ITEMS.forEach { it.onEnd() }

        // 発光、チームをリセット
        WereWolf3.PLAYERS.forEach { player ->
            player.flySpeed = 0.2f
            player.walkSpeed = 0.2f
            TeamPacketUtil.removeAll(player, ChatColor.DARK_RED,)
            GlowPacketUtil.removeAll(player)
            player.setDisplayName(player.name)
            player.setPlayerListName(player.name)
            player.role = null
            player.co = null
            player.inventory.clear()
            player.sidebar = WaitingSidebar()
            player.gameMode = GameMode.ADVENTURE
            val tc = (0..100)
            player.teleport(player.world.spawnLocation.clone().add(tc.random()/100.0,0.0,tc.random()/100.0))
        }
        // 死体を全削除
        DeadBody.DEAD_BODIES.forEach { it.destroy() }
        WereWolf3.STATUS = Status.WAITING
        if(!shutdown) {
            if(WereWolf3.isPlugmanLoaded) { PluginUtil.reload(WereWolf3.INSTANCE) } else { Bukkit.getServer().reload() }
        }
    }

    fun init() {

    }
}