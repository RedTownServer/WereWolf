package dev.mr3n.werewolf3.sidebar

import dev.mr3n.werewolf3.Constants
import dev.mr3n.werewolf3.TickTask
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.utils.languages
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Scoreboard

interface ISideBar {
    val scoreboard: Scoreboard
    companion object {
        private val sidebars = mutableMapOf<Player, ISideBar?>()
        var Player.sidebar: ISideBar?
            set(value) {
                this.scoreboard = value?.scoreboard?:throw NullPointerException()
                sidebars[this] = value
            }
            get() = sidebars[this]

    }
}