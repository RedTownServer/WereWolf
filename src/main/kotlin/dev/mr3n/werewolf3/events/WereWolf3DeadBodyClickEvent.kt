package dev.mr3n.werewolf3.events

import dev.mr3n.werewolf3.protocol.DeadBody
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class WereWolf3DeadBodyClickEvent(player: Player, val deadBody: DeadBody): PlayerEvent(player) {
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}