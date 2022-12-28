package dev.mr3n.werewolf3.utils

import org.bukkit.scoreboard.Scoreboard

@Deprecated("archived.")
fun Scoreboard.getOrNew(name: String) = this.getTeam(name)?:this.registerNewTeam(name)