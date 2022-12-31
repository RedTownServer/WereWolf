package dev.mr3n.werewolf3.sidebar

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