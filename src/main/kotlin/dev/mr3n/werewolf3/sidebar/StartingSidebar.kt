package dev.mr3n.werewolf3.sidebar

import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.parseTime
import dev.mr3n.werewolf3.utils.role
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot

/**
 * 待機中に表示するサイドバー
 */
class StartingSidebar(val player: Player): ISideBar {
    // サイドバーのスコアボード
    override val scoreboard = checkNotNull(Bukkit.getScoreboardManager()?.newScoreboard)

    // 待機中のプレイヤー数を表示するためのチーム
    private val playersTeam = scoreboard.registerNewTeam("players").apply { addEntry(languages("sidebar.starting.players.display")) }
    // プレイヤー人数を設定
    fun players(value: Int) { if(playersTeam.suffix!="${value}人"){ playersTeam.suffix = "${value}人" } }

    // 待機時間を表示するためのチーム
    private val timeTeam = scoreboard.registerNewTeam("time").apply { addEntry(languages("sidebar.starting.time.display")) }
    // 待機時間を設定: 秒
    fun time(value: Int) { if(timeTeam.suffix!=value.parseTime()){ timeTeam.suffix = value.parseTime() } }

    // 現在のステータつ情報を表示するためのチーム
    private val statusTeam = scoreboard.registerNewTeam("status").apply { addEntry(languages("sidebar.global.status.display")) }
    // ステータス情報を設定
    fun status(value: String) { if(statusTeam.suffix!=value){ statusTeam.suffix = value } }

    // 現在のステータつ情報を表示するためのチーム
    private val roleTeam = scoreboard.registerNewTeam("role").apply { addEntry(languages("sidebar.global.role.display")) }
    // ステータス情報を設定
    fun role(value: Role?) { if(roleTeam.suffix != "${value?.color}${ChatColor.BOLD}${value?.displayName}") { roleTeam.suffix = "${value?.color}${ChatColor.BOLD}${value?.displayName}" } }

    // サイドバーに表示するオブジェクト
    val objective = scoreboard.registerNewObjective("starting", Criteria.DUMMY, languages("sidebar.title")).apply {
        // スロットをサイドバーに
        displaySlot = DisplaySlot.SIDEBAR
        // サイドバーの一覧に下に表示するかっこいいやつ(//////<-これ)
        getScore(languages("sidebar.bottom")).apply { score = 0 }
        // マージン
        getScore("").apply { score = 1 }
        // 役職
        getScore(languages("sidebar.global.role.display")).apply { score = 2 }
        // マージン
        getScore(" ").apply { score = 3 }
        // ステータス
        getScore(languages("sidebar.global.status.display")).apply { score = 4 }
        // マージン
        getScore("  ").apply { score = 5 }
        // 待機プレイヤー数
        getScore(languages("sidebar.starting.players.display")).apply { score = 6 }
        // マージン
        getScore("   ").apply { score = 7 }
        // 待機時間
        getScore(languages("sidebar.starting.time.display")).apply { score = 8 }
        // 待機プレイヤー数を設定
        players(WereWolf3.PLAYERS.size)
        // 待機時間を設定
        time(0)

        status(languages("sidebar.global.status.starting"))
        // ステータスを待機中に変更
        role(player.role)
    }
}