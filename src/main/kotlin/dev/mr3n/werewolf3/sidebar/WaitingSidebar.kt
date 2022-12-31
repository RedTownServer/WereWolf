package dev.mr3n.werewolf3.sidebar

import dev.mr3n.werewolf3.utils.languages
import org.bukkit.Bukkit
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot

/**
 * 待機中に表示するサイドバー
 */
class WaitingSidebar: ISideBar {
    // サイドバーのスコアボード
    override val scoreboard = checkNotNull(Bukkit.getScoreboardManager()?.newScoreboard)

    // 待機中のプレイヤー数を表示するためのチーム
    private val playersTeam = scoreboard.registerNewTeam("players").apply { addEntry(languages("sidebar.waiting.players.display")) }
    // プレイヤー人数を設定
    fun players(value: Int) { if(playersTeam.suffix!="${value}人") { playersTeam.suffix = "${value}人" } }

    // 待機時間を表示するためのチーム
    private val timeTeam = scoreboard.registerNewTeam("time").apply { addEntry(languages("sidebar.waiting.time.display")) }
    // 待機時間を設定: 秒
    fun time(value: String) { if(timeTeam.suffix!=value) { timeTeam.suffix = value } }

    // 現在のステータつ情報を表示するためのチーム
    private val statusTeam = scoreboard.registerNewTeam("status").apply { addEntry(languages("sidebar.global.status.display")) }
    // ステータス情報を設定
    fun status(value: String) { if(statusTeam.suffix!=value){ statusTeam.suffix = value } }

    // サイドバーに表示するオブジェクト
    val objective = scoreboard.registerNewObjective("waiting", Criteria.DUMMY, languages("sidebar.title")).apply {
        // スロットをサイドバーに
        displaySlot = DisplaySlot.SIDEBAR
        // サイドバーの一覧に下に表示するかっこいいやつ(//////<-これ)
        getScore(languages("sidebar.bottom")).apply { score = 0 }
        // マージン
        getScore(" ").apply { score = 1 }
        // ステータス
        getScore(languages("sidebar.global.status.display")).apply { score = 2 }
        // マージン
        getScore("  ").apply { score = 3 }
        // 待機プレイヤー数
        getScore(languages("sidebar.waiting.players.display")).apply { score = 4 }
        // マージン
        getScore("   ").apply { score = 5 }
        // 待機時間
        getScore(languages("sidebar.waiting.time.display")).apply { score = 6 }
        // 待機プレイヤー数を設定
        players(Bukkit.getOnlinePlayers().size)
        // 待機時間を設定
        time(languages("sidebar.waiting.time.soon"))
        // ステータスを待機中に変更
        status(languages("sidebar.global.status.waiting"))
    }
}