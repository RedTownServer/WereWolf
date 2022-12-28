package dev.mr3n.werewolf3

import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.sidebar.ISideBar.Companion.sidebar
import dev.mr3n.werewolf3.sidebar.RunningSidebar
import dev.mr3n.werewolf3.utils.languages
import org.bukkit.Bukkit
import org.bukkit.Sound
import org.bukkit.SoundCategory
import org.bukkit.boss.BarColor

enum class Time(val barColor: BarColor) {
    DAY(BarColor.YELLOW),
    NIGHT(BarColor.PURPLE);

    fun lowercase() = this.toString().lowercase()

    val displayName: String
        get() = languages("time.${lowercase()}.name")
    val emoji: String
        get() = languages("time.${lowercase()}.emoji")

    /**
     * 時間帯の説明
     */
    val description: String
        get() = languages("time.${lowercase()}.description", "%day%" to Constants.MAX_DAYS - WereWolf3.DAY)


    /**
     * 朝/夜の変更の処理
     */
    operator fun invoke() {
        when(this) {
            DAY -> { morning() }
            NIGHT -> { night() }
        }
    }

    val title: String
        get() = languages("title.time.title", "%emoji%" to DAY.emoji, "%time%" to displayName, "%day%" to WereWolf3.DAY)

    /**
     * 次の時間帯を変えします。
     */
    fun next(): Time {
        return when(this) {
            DAY -> { NIGHT }
            NIGHT -> { DAY }
        }
    }

    companion object {
        /**
         * 朝になったときの処理
         */
        fun morning() {
            // ゲームが実行中ではない場合return
            if(!WereWolf3.running) { return }
            if(WereWolf3.DAY>=Constants.MAX_DAYS) {
                GameTerminator.end(Role.Faction.VILLAGER, languages("title.win.reason.time_up"))
                return
            }
            // 残り時間を朝の時間に設定(20はtick)
            WereWolf3.TIME_LEFT = Constants.DAY_TIME * 20
            WereWolf3.TIME_LENGTH = WereWolf3.TIME_LEFT
            // 日付を1追加する
            WereWolf3.DAY++

            WereWolf3.PLAYERS.forEach { player ->
                // プレイヤーに朝になった旨を伝える。
                player.sendTitle(WereWolf3.TIME.title, WereWolf3.TIME.description, 0, 100, 20)
                // TODO レコードを流す
                // ぴろりーんの効果音
                player.world.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
                // ボスバーの色を朝の色に変更
                WereWolf3.BOSSBAR.color = WereWolf3.TIME.barColor
                // ワールドの時間帯を朝に変更
                player.world.time = 8000
                val sidebar = player.sidebar
                if(sidebar is RunningSidebar) {
                    sidebar.day(Constants.MAX_DAYS - WereWolf3.DAY)
                }
            }
        }

        /**
         * 夜になったときの処理
         */
        fun night() {
            // ゲームが実行中ではない場合return
            if(!WereWolf3.running) { return }
            // 残り時間を夜の時間に設定(20はtick)
            WereWolf3.TIME_LEFT = Constants.NIGHT_TIME * 20
            WereWolf3.TIME_LENGTH = WereWolf3.TIME_LEFT

            WereWolf3.PLAYERS.forEach { player ->
                // プレイヤーに夜になった旨を伝える。
                player.sendTitle(WereWolf3.TIME.title, WereWolf3.TIME.description, 0, 100, 20)
                // ボスバーを夜の色に変更
                WereWolf3.BOSSBAR.color = WereWolf3.TIME.barColor
                // すべての効果音を停止(BGM停止用)
                player.stopAllSounds()
                // 狼の遠吠えを再生
                player.world.playSound(player, Sound.ENTITY_WOLF_HOWL, SoundCategory.MASTER, 1f, 1f)
                // ワールドの時間帯を夜に変更
                player.world.time = 16000
            }
        }
    }
}