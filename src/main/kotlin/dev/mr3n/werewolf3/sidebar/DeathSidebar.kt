package dev.mr3n.werewolf3.sidebar

import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.utils.languages
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot

/**
 * 待機中に表示するサイドバー
 */
class DeathSidebar(player: Player): RunningSidebar(player) {
    override fun money(value: Int) { }

    // 待機中のプレイヤー数を表示するためのチーム
    private val playersTeam = scoreboard.registerNewTeam("players").apply { addEntry(languages("sidebar.dead.players.display")) }
    fun players(value: Int)  { if(playersTeam.suffix!="${value}人"){ playersTeam.suffix = "${value}人" } }

    // サイドバーに表示するオブジェクト
    override val objective = scoreboard.registerNewObjective("death", Criteria.DUMMY, languages("sidebar.title")).apply {
        // スロットをサイドバーに
        displaySlot = DisplaySlot.SIDEBAR
        // サイドバーの一覧に下に表示するかっこいいやつ(//////<-これ)
        getScore(languages("sidebar.bottom")).apply { score = 0 }
        // マージン
        getScore(" ").apply { score = 1 }
        // 役職
        getScore(languages("sidebar.global.role.display")).apply { score = 2 }
        // マージン
        getScore("  ").apply { score = 3 }
        // 待機プレイヤー数
        getScore(languages("sidebar.running.players.display")).apply { score = 4 }
        // マージン
        getScore("   ").apply { score = 5 }
        // 待機プレイヤー数
        getScore(languages("sidebar.dead.players.display")).apply { score = 6 }
        // マージン
        getScore("    ").apply { score = 7 }
        // 待機プレイヤー数
        getScore(languages("sidebar.running.day.display")).apply { score = 8 }
        // 参加プレイヤー数を設定
        players(WereWolf3.PLAYERS.size)
        // 待機時間を設定
        players(WereWolf3.PLAYERS.count { it.gameMode != GameMode.SPECTATOR })
    }
}