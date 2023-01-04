package dev.mr3n.werewolf3

import com.rylinaux.plugman.util.PluginUtil
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.protocol.DeadBody
import dev.mr3n.werewolf3.protocol.MetadataPacketUtil
import dev.mr3n.werewolf3.protocol.TeamPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.WaitingSidebar
import dev.mr3n.werewolf3.utils.*
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player

object GameTerminator {

    /**
     * 結果発表とともにゲームを終了する際に使用します。
     */
    fun end(win: Role.Team, reason: String) {
        if(WereWolf3.STATUS==Status.ENDING||WereWolf3.STATUS==Status.WAITING) { return }
        WereWolf3.STATUS = Status.ENDING
        // 役職 to プレイヤー一覧 のマップ
        val players = Role.ROLES.map { it.key to it.value.map { uniqueId -> Bukkit.getOfflinePlayer(uniqueId) } }
        WereWolf3.PLAYERS.forEach { player ->
            // 改行を大量に送信して上のチャットを削除
            repeat(20) { player.sendMessage("\n") }
            // どちらサイドが勝利したかをタイトルで表示
            player.sendTitle(languages("title.win.title", "%role%" to win.displayName, "%color%" to win.color), reason, 20, 100, 20)
            // ﾋﾟﾛﾘｰﾝ
            player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
            // 結果発表のヘッダー
            player.sendMessage("${languages("messages.result.prefix")}${languages("messages.result.header")}")
            players.forEach s@{  (role, players) ->
                // 役職にプレイヤーが一人も属していない場合は内訳の表示をスキップ
                if(players.isEmpty()) { return@s }
                val textComponents = TextComponent("${role.color}${role.displayName}:")
                players.associateWith {p->if(p is Player) p.kills?: intArrayOf() else intArrayOf() }
                    .forEach { (player, kills) ->
                        // 身内を殺した回数
                        val killTeams = kills.count { WereWolf3.PLAYER_BY_ENTITY_ID[it]?.role?.team == role.team }
                        // プレイヤー名にホバーした際に身内キル/敵キルのなどの内訳を表示
                        val hover = HoverEvent(HoverEvent.Action.SHOW_TEXT, Text(languages("messages.result.kills_info", "%kills%" to kills.size-killTeams, "%kill_teams%" to killTeams)))
                        // テキスト化
                        val component = TextComponent("${role.color}${player.name}(${if(kills.isEmpty()) "0" else "${ChatColor.UNDERLINE}${kills.size}${role.color}"})")
                        // ホバーした歳のイベントを設定
                        component.hoverEvent = hover
                        // プレイヤー名の間に空白を挿入
                        textComponents.addExtra(" ")
                        // それをメインのテキストに追加
                        textComponents.addExtra(component)
                    }
                player.spigot().sendMessage(textComponents)
            }
            player.sendMessage("${languages("messages.result.prefix")}${languages("messages.result.header")}")
            player.sendMessage(prefixedLang("messages.result.winner", "%team%" to win.displayName, "%color%" to win.color))
        }
        this.run()
    }

    /**
     * 結果発表を無視して強制的にゲームを終了する際に使用します。
     */
    fun run(shutdown: Boolean = false) {
        try {
            WereWolf3.STATUS = Status.WAITING
            WereWolf3.GAME_ID = null
        } catch(_: Exception) { }

        try { IShopItem.ShopItem.ITEMS.forEach { it.onEnd() } } catch(_: Exception) { }

        Bukkit.getWorlds()[0].time = 6000

        try {
            // 発光、チームをリセット
            WereWolf3.PLAYERS.forEach { player ->
                player.flySpeed = 0.2f
                player.walkSpeed = 0.2f
                // チームの色を削除
                TeamPacketUtil.removeAll(player, ChatColor.DARK_RED,)
                // パケットで発光を送信していた場合は削除
                MetadataPacketUtil.removeAllGlowing(player)
                // パケットで透明を送信していた場合は削除
                MetadataPacketUtil.removeAllInvisible(player)
                // co帽子などで表示名が変わっている場合は戻す
                player.setDisplayName(player.name)
                player.setPlayerListName(player.name)
                player.activePotionEffects.forEach { player.removePotionEffect(it.type) }
                // 人狼関係のデータを削除
                player.kills = null
                player.role = null
                player.co = null
                // インベントリを削除
                player.inventory.clear()
                // サイドバーを待機中のものに変更
                player.sidebar = WaitingSidebar()
                player.gameMode = GameMode.ADVENTURE
                // >>> バグって動かないようにちょっとずらしてスポーン地点にてレポート >>>
                val tc = (0..100)
                player.teleport(player.world.spawnLocation.clone().add(tc.random()/100.0,0.0,tc.random()/100.0))
                // <<< バグって動かないようにちょっとずらしてスポーン地点にてレポート <<<
            }
        } catch(e: Exception) { e.printStackTrace() }
        // 死体を全削除
        try { DeadBody.DEAD_BODIES.forEach { it.destroy() } } catch(_: Exception) {}
        try { WereWolf3.PLAYERS.clear() } catch(_: Exception) {}
        // プラグインをreload
        if(!shutdown) {
            if(WereWolf3.isPlugmanLoaded) { PluginUtil.reload(WereWolf3.INSTANCE) } else { Bukkit.getServer().reload() }
        }
    }

    fun init() {

    }
}