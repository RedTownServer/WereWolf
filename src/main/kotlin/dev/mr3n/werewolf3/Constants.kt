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
    val START_MONEY: Int
        get() = constant("game.start_money")
    val MAX_DAYS: Int
        get() = constant("game.max_days")
    val END_TIME: Time
        get() = try { Time.valueOf(constant("end_time")) } catch(_: Exception) { Time.DAY }
    val DEAD_BODY_PRIZE: Int
        get() = constant("game.dead_body_prize")
    val CONVERSATION_DISTANCE: Double
        get() = constant("game.conversation_distance")
}