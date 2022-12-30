package dev.mr3n.werewolf3

import dev.mr3n.werewolf3.utils.constant

/**
 * 定数一覧。
 * 今後スプレッドシート上から変更できるようにするため一元化しています。
 */
object Constants {
    val POINT_FLUSH_SPEED: Int
        get() = constant("general.point_flush_speed")
    val STARTING_TIME: Int
        get() = constant("game.starting_time")
    val DAY_TIME: Int
        get() = constant("game.day_time")
    val NIGHT_TIME: Int
        get() = constant("game.night_time")
    val ADD_MONEY: Int
        get() = constant("game.add_money")
    val MAX_DAYS: Int
        get() = constant("game.max_days")
}