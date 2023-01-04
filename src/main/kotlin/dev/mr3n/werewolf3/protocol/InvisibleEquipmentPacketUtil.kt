package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers.ItemSlot
import com.comphenix.protocol.wrappers.Pair
import dev.mr3n.werewolf3.WereWolf3
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.PlayerInventory

object InvisibleEquipmentPacketUtil {

    private val INVISIBLE_PLAYERS = mutableMapOf<Player,MutableMap<Int, MutableMap<Short, List<ItemSlot>>>>()
    private val ITEM_SLOT_MAPPING = mapOf<ItemSlot, (PlayerInventory)->ItemStack?>(
        ItemSlot.HEAD to { it.helmet },
        ItemSlot.CHEST to { it.chestplate },
        ItemSlot.LEGS to { it.leggings },
        ItemSlot.FEET to { it.boots },
        ItemSlot.MAINHAND to { it.itemInMainHand },
        ItemSlot.OFFHAND to { it.itemInOffHand },
    )

    fun add(sendTo: Player, player: Player, priority: Short, vararg slots: ItemSlot) {
        if(sendTo==player) { return }
        val players = INVISIBLE_PLAYERS[sendTo]?: mutableMapOf()
        val slotList = players[player.entityId]?: mutableMapOf()
        if(slotList.contains(priority)) { return }
        slotList[priority] = slots.toList()
        players[player.entityId] = slotList
        INVISIBLE_PLAYERS[sendTo] = players
        sendPacket(sendTo, player)
    }

    fun remove(sendTo: Player, player: Player, priority: Short) {
        if(sendTo==player) { return }
        val players = INVISIBLE_PLAYERS[sendTo]?:return
        val slotList = players[player.entityId]?:return
        if(!slotList.contains(priority)) { return }
        slotList.remove(priority)
        players[player.entityId] = slotList
        INVISIBLE_PLAYERS[sendTo] = players
        sendResetPacket(sendTo, player)
    }

    fun sendResetPacket(sendTo: Player, player: Player) {
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT)
        packet.integers.write(0, player.entityId)
        val players = INVISIBLE_PLAYERS[sendTo]?: mutableMapOf()
        val invisibleData = players[player.entityId]?: mutableMapOf()
        val list = invisibleData[invisibleData.keys.minOrNull()?:0]?: listOf()
        packet.slotStackPairLists.writeSafely(0,
            ITEM_SLOT_MAPPING.keys.filterNot { list.contains(it) }
                .map { Pair(it, ITEM_SLOT_MAPPING[it]?.let { it1 -> it1(player.inventory) }?:return@map null) }
                .filterNotNull()
        )
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(sendTo, packet)
    }

    fun sendPacket(sendTo: Player, player: Player) {
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT)
        packet.integers.write(0, player.entityId)
        val players = INVISIBLE_PLAYERS[sendTo]?:return
        val invisibleData = players[player.entityId]?:return
        val firstKey = invisibleData.keys.minOrNull() ?:return
        packet.slotStackPairLists.writeSafely(0, invisibleData[firstKey]?.map { Pair(it, null) })
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(sendTo, packet)
    }

    init {
        WereWolf3.PROTOCOL_MANAGER.addPacketListener(object: PacketAdapter(WereWolf3.INSTANCE, PacketType.Play.Server.ENTITY_EQUIPMENT) {
            override fun onPacketSending(event: PacketEvent) {
                val packet = event.packet.deepClone()
                val players = INVISIBLE_PLAYERS[event.player]?:return
                val invisibleData = players[packet.integers.readSafely(0)]?:return
                val firstKey = invisibleData.keys.minOrNull() ?:return
                val items = packet.slotStackPairLists.readSafely(0).associate { it.first to it.second }.toMutableMap()
                items.putAll(invisibleData[firstKey]?.associate { it to null as ItemStack? }?:mapOf())
                packet.slotStackPairLists.writeSafely(0, items.map { Pair(it.key,it.value) })
                event.packet = packet
            }
        })
    }
}