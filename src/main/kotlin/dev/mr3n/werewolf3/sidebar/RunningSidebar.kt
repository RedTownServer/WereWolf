package dev.mr3n.werewolf3.sidebar

import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.role
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot

/**
 * 待機中に表示するサイドバー
 */
open class RunningSidebar(val player: Player): ISideBar {
    // サイドバーのスコアボード
    final override val scoreboard = checkNotNull(Bukkit.getScoreboardManager()?.newScoreboard)

    // 待機中のプレイヤー数を表示するためのチーム
    private val playersEstTeam = scoreboard.registerNewTeam("playersEst").apply { addEntry(languages("sidebar.running.players.display")) }
    // プレイヤー人数を設定
    fun playersEst(value: Int) { if(playersEstTeam.suffix!="${value}人"){ playersEstTeam.suffix = "${value}人" } }


    // 現在のステータつ情報を表示するためのチーム
    private val roleTeam = scoreboard.registerNewTeam("role").apply { addEntry(languages("sidebar.global.role.display")) }
    // ステータス情報を設定
    fun role(value: Role?) { if(roleTeam.suffix != "${value?.color}${ChatColor.BOLD}${value?.displayName}") { roleTeam.suffix = "${value?.color}${ChatColor.BOLD}${value?.displayName}" } }

    // 現在のステータつ情報を表示するためのチーム
    private val moneyTeam = scoreboard.registerNewTeam("money").apply { addEntry(languages("sidebar.running.money.display")) }
    // ステータス情報を設定
    open fun money(value: Int) { if(moneyTeam.suffix != "${value}円") { moneyTeam.suffix = "${value}円" } }

    // 現在のステータつ情報を表示するためのチーム
    private val dayTeam = scoreboard.registerNewTeam("day").apply { addEntry(languages("sidebar.running.day.display")) }
    // ステータス情報を設定
    fun day(value: Int) { if(dayTeam.suffix != "${value}日") { dayTeam.suffix = "${value}日" } }

    // サイドバーに表示するオブジェクト
    open val objective = scoreboard.registerNewObjective("running", Criteria.DUMMY, languages("sidebar.title")).apply {
        // スロットをサイドバーに
        displaySlot = DisplaySlot.SIDEBAR
        // サイドバーの一覧に下に表示するかっこいいやつ(//////<-これ)
        getScore(languages("sidebar.bottom")).apply { score = 0 }
        getScore("").apply { score = 1 }
        // 待機プレイヤー数
        getScore(languages("sidebar.running.money.display")).apply { score = 2 }
        // マージン
        getScore(" ").apply { score = 3 }
        // 役職
        getScore(languages("sidebar.global.role.display")).apply { score = 4 }
        // マージン
        getScore("  ").apply { score = 5 }
        // 待機プレイヤー数
        getScore(languages("sidebar.running.players.display")).apply { score = 6 }
        // マージン
        getScore("   ").apply { score = 7 }
        // 待機プレイヤー数
        getScore(languages("sidebar.running.day.display")).apply { score = 8 }
        // 参加プレイヤー数を設定
        playersEst(WereWolf3.PLAYERS.size)
        // 待機時間を設定
        // ステータスを待機中に変更
        role(player.role)
        money(300)
        day(-1)
    }
}