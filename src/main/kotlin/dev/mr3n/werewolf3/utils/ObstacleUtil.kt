package dev.mr3n.werewolf3.utils

import org.bukkit.Bukkit
import org.bukkit.Location

/**
 * プレイヤーの視線上に障害物があるかどうかを確認します。ある場合はtrue
 */
fun Location.hasObstacleInPath(end: Location, max: Double = Bukkit.getServer().viewDistance.toDouble()): Boolean {
    // 障害物がない場合はnullが返ってくるため!=nullで比較。障害物がある場合はtrue
    val start = this.clone()
    val distance = start.distance(end)
    val direction = end.toVector().subtract(start.toVector()).normalize()
    val now = start.clone().add(direction)
    while(start.distance(now).let { it < distance && it < max }) {
        if(now.block.type.isOccluding) { return true }
        now.add(direction)
    }
    return false
}