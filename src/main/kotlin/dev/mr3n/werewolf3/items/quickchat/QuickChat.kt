package dev.mr3n.werewolf3.items.quickchat

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.roles.Role
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.*

abstract class QuickChat(id: String, material: Material, val coolDownMs: Long): IShopItem.ShopItem(id, material) {
    override val price: Int = Int.MAX_VALUE
    override val roles: List<Role> = listOf()
    private val lastUse = mutableMapOf<UUID, Long>()
    override fun buy(player: Player): Boolean = false
    val messages = WereWolf3.LANGUAGES.config()?.getStringList("item.${id}.messages")?: listOf()
    fun message(player: String): String = messages.randomOrNull()?.replace("%player%", player)?:"THIS IS BUG. PLEASE REPORT TO ADMIN."

    init {
        WereWolf3.INSTANCE.registerEvent<PlayerInteractAtEntityEvent> { event ->
            val player = event.player
            val item = player.inventory.itemInMainHand
            if(event.hand!=EquipmentSlot.HAND) { return@registerEvent }
            if(!isSimilar(item)) { return@registerEvent }
            event.isCancelled = true
            val target = event.rightClicked
            if(target !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(target)) { return@registerEvent }
            // クールダウンチェック
            if(System.currentTimeMillis() - (lastUse[player.uniqueId]?:0) <= coolDownMs) { return@registerEvent }
            player.chat(message(target.name))
            lastUse[player.uniqueId] = System.currentTimeMillis()
        }
        WereWolf3.INSTANCE.registerEvent<EntityDamageByEntityEvent> { event ->
            val player = event.damager
            val target = event.entity
            if(player !is Player) { return@registerEvent }
            if(target !is Player) { return@registerEvent }
            val item = player.inventory.itemInMainHand
            if(!isSimilar(item)) { return@registerEvent }
            event.isCancelled = true
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(target)) { return@registerEvent }
            // クールダウンチェック
            if(System.currentTimeMillis() - (lastUse[player.uniqueId]?:0) <= coolDownMs) { return@registerEvent }
            player.chat(message(target.name))
            lastUse[player.uniqueId] = System.currentTimeMillis()
            event.isCancelled = true
        }
    }
}