package dev.mr3n.werewolf3.events

import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerEvent

class WereWolf3DamageEvent(player: Player, val damage: Double): PlayerEvent(player), Cancellable {
    private var isCancelled: Boolean = false

    override fun isCancelled(): Boolean = isCancelled

    override fun setCancelled(cancel: Boolean) { isCancelled = cancel }

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