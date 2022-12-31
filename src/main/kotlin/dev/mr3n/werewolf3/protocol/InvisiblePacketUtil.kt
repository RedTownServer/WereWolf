package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers
import com.comphenix.protocol.wrappers.Pair
import dev.mr3n.werewolf3.WereWolf3
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType

object InvisiblePacketUtil {
    fun sendEmptySlotPacket(sendTo: Player, player: Player) {
        if(sendTo==player) { return }
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT)
        packet.integers.write(0, player.entityId)
        val air = ItemStack(Material.AIR)
        val stacks = mapOf(EnumWrappers.ItemSlot.HEAD to air, EnumWrappers.ItemSlot.CHEST to ItemStack(Material.AIR), EnumWrappers.ItemSlot.LEGS to ItemStack(Material.AIR), EnumWrappers.ItemSlot.FEET to ItemStack(Material.AIR))
            .map { Pair(it.key,it.value) }
        packet.slotStackPairLists.write(0, stacks)
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(sendTo, packet)
    }

    fun sendResetSlotPacket(sendTo: Player, player: Player) {
        if(sendTo==player) { return }
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT)
        packet.integers.write(0, player.entityId)
        val stacks = mapOf(
            EnumWrappers.ItemSlot.HEAD to player.inventory.helmet
        ).map { Pair(it.key,it.value) }
        packet.slotStackPairLists.write(0, stacks)
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(sendTo, packet)
    }

    init {
        WereWolf3.PROTOCOL_MANAGER.addPacketListener(object: PacketAdapter(WereWolf3.INSTANCE, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            override fun onPacketSending(event: PacketEvent) {
                val packet = event.packet.deepClone()
                val entityId = packet.integers.read(0)
                val entity = WereWolf3.PLAYER_BY_ENTITY_ID[entityId]?:return
                if(entity.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                    val slots = packet.slotStackPairLists.read(0)
                        .associate { it.first to it.second }
                        .map { (slot, item) ->
                            when(slot) {
                                EnumWrappers.ItemSlot.HEAD, EnumWrappers.ItemSlot.CHEST, EnumWrappers.ItemSlot.LEGS, EnumWrappers.ItemSlot.FEET -> { slot to ItemStack(Material.AIR) }
                                else -> { slot to item }
                            }
                        }.map { Pair(it.first,it.second) }
                    packet.slotStackPairLists.write(0,slots)
                    event.packet = packet
                }
            }
        })
    }
}