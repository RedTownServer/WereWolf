package dev.mr3n.werewolf3.events

import dev.mr3n.werewolf3.protocol.DeadBody
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class WereWolf3DeadBodyClickEvent(player: Player, val deadBody: DeadBody): PlayerEvent(player), Cancellable {
    override fun getHandlers(): HandlerList {
        return HANDLERS
    }

    private var isCancelled = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(cancel: Boolean) {
        isCancelled = cancel
    }

    companion object {
        private val HANDLERS = HandlerList()

        @JvmStatic
        fun getHandlerList(): HandlerList {
            return HANDLERS
        }
    }
}