package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction
import com.comphenix.protocol.wrappers.PlayerInfoData
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.event.player.PlayerGameModeChangeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

object SpectatorPacketUtil {

    val spectators = mutableListOf<Int>()

    fun init() {}
    init {
        WereWolf3.PROTOCOL_MANAGER.addPacketListener(object: PacketAdapter(WereWolf3.INSTANCE,ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO) {
            override fun onPacketSending(event: PacketEvent) {
                val player = event.player
                val packet = event.packet.deepClone()
                val actions = packet.playerInfoActions.read(0)
                if(actions.contains(PlayerInfoAction.UPDATE_GAME_MODE)) {
                    val infoData = packet.playerInfoDataLists.read(1).map { infoData ->
                        if(player.uniqueId==infoData.profile.uuid) { return@map infoData }
                        val gameMode = if(infoData.gameMode==NativeGameMode.SPECTATOR) NativeGameMode.CREATIVE else infoData.gameMode
                        return@map PlayerInfoData(infoData.profile, infoData.latency, gameMode, infoData.displayName, infoData.profileKeyData)
                    }
                    packet.playerInfoDataLists.writeSafely(1, infoData)
                    event.packet = packet
                }
            }
        })
        WereWolf3.PROTOCOL_MANAGER.addPacketListener(object: PacketAdapter(WereWolf3.INSTANCE,ListenerPriority.NORMAL, PacketType.Play.Server.ENTITY_DESTROY) {
            override fun onPacketSending(event: PacketEvent) {
                val player = event.player
                if(player.gameMode!=GameMode.SPECTATOR) {
                    val packet = event.packet.deepClone()
                    val entities = packet.intLists.readSafely(0)
                    if (entities.any { id -> spectators.contains(id) }) {
                        val new = entities.toMutableList()
                        new.removeAll(spectators)
                        packet.intLists.writeSafely(0, new)
                        event.packet = packet
                    }
                }
            }
        })
        WereWolf3.INSTANCE.registerEvent<PlayerJoinEvent> { event ->
            val player = event.player
            if(player.gameMode == GameMode.SPECTATOR) { spectators.add(player.entityId) }
        }
        WereWolf3.INSTANCE.registerEvent<PlayerQuitEvent>() { event ->
            val player = event.player
            spectators.remove(player.entityId)
        }
        WereWolf3.INSTANCE.registerEvent<PlayerGameModeChangeEvent>() { event ->
            val player = event.player
            when(event.newGameMode) {
                GameMode.SPECTATOR -> {
                    spectators.add(player.entityId)
                    val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_TELEPORT)
                    packet.integers.writeSafely(0, player.entityId)
                    packet.booleans.writeSafely(0, false)
                    packet.bytes.writeSafely(0,0.toByte()).writeSafely(1,0.toByte())
                    // スペクテイターじゃない人たちにスペクテイターになったプレイヤーの座標を改ざんして送信する
                    Bukkit.getOnlinePlayers().filter { it.gameMode != GameMode.SPECTATOR }.forEach { player2 ->
                        if(player2==player) { return@forEach }
                        val location = player2.location
                        packet.doubles
                            .writeSafely(0, location.x)
                            .writeSafely(1, -1000.0)
                            .writeSafely(2, location.z)
                        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player2, packet.deepClone())
                    }
                }
                else -> {
                    spectators.remove(player.entityId)
                    val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_TELEPORT)
                    packet.booleans.writeSafely(0, false)
                    packet.bytes.writeSafely(0,0.toByte()).writeSafely(1,0.toByte())
                    val location = player.location
                    packet.doubles
                        .writeSafely(0, location.x)
                        .writeSafely(1, -1000.0)
                        .writeSafely(2, location.z)
                    // プレイヤーにスペクテイターの人たちの座標を改ざんして送信する
                    Bukkit.getOnlinePlayers().filter { it.gameMode == GameMode.SPECTATOR }.forEach { player2 ->
                        if(player2==player) { return@forEach }
                        packet.integers.writeSafely(0, player2.entityId)
                        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, packet.deepClone())
                    }
                }
            }
        }
    }
}
