package dev.mr3n.werewolf3.utils

import org.bukkit.Location

fun Location.hasObstacleInPath(pos2: Location): Boolean {
    return this.world?.rayTraceBlocks(this,pos2.toVector().subtract(this.toVector()),128.0) != null
}