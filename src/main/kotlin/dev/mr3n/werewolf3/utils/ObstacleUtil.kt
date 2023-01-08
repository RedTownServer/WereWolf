package dev.mr3n.werewolf3.utils

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.util.Vector

fun getPlayerSightHeight(player: Player): Location {
    val start = player.location.clone()
    if(player.isSneaking){
        // スニークしているときの視線の高さ
        start.add(.0,1.281,.0)
    } else if(player.isSwimming) {
        // 泳いでいるときの視線の高さ
        start.add(.0,0.393,.0)
    } else {
        // 普通に立っているときの視線の高さ
        start.add(.0,1.625,.0)
    }
    return start
}

fun Player.hasObstacleInSightPath(target: Player, max: Double = Bukkit.getServer().viewDistance.toDouble() * 16): Boolean {
    val player = this
    val start = getPlayerSightHeight(player)
    val head = target.location.clone()
    if(player.isSwimming) {
        // 泳いでいる場合は高さの計算がだるいので一個の座標のみで計算して返す。
        return start.hasObstacleInPath(head.add(.0,0.393,.0))
    } else if(player.isSneaking) {
        head.add(.0,1.281,.0)
    } else {
        head.add(.0,1.625,.0)
    }
    val foot = target.location.clone()
    return start.hasObstacleInPath(head, max) && start.hasObstacleInPath(foot, max)
}

fun Player.hasObstacleInSightPath(target: Location, max: Double = Bukkit.getServer().viewDistance.toDouble() * 16): Boolean {
    val player = this
    return getPlayerSightHeight(player).hasObstacleInPath(target, max)
}

/**
 * プレイヤーの視線上に障害物があるかどうかを確認します。ある場合はtrue
 */
fun Location.hasObstacleInPath(end: Location, max: Double = Bukkit.getServer().viewDistance.toDouble() * 16): Boolean {
    // 障害物がない場合はnullが返ってくるため!=nullで比較。障害物がある場合はtrue
    val start = this.clone()
    val distance = start.distance(end)
    // 二点間の距離がmaxを超過していた場合はfalseを返す
    if(distance > max) { return false }
    val direction = end.toVector().subtract(start.toVector()).normalize().multiply(0.1)
    val now = start.clone().add(direction)
    while(start.distance(now).let { it < distance && it < max }) {
        if(now.block.type.isOccluding) { return true }
        now.add(direction)
    }
    return false
}