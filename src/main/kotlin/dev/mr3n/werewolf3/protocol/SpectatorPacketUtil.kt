package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode
import com.comphenix.protocol.wrappers.EnumWrappers.PlayerInfoAction
import com.comphenix.protocol.wrappers.PlayerInfoData
import dev.mr3n.werewolf3.WereWolf3

object SpectatorPacketUtil {

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
    }
}
