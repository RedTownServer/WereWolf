package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.utility.MinecraftReflection
import com.comphenix.protocol.wrappers.WrappedChatComponent
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerJoinEvent
import java.util.*

object TeamPacketUtil {
    private val TEAMS = mutableMapOf<Player, MutableMap<ChatColor, MutableList<String>>>()

    /**
     * 色用のチームを作成するパケットです。
     */
    fun createTeamColorPacket(player: Player,color: ChatColor): PacketContainer {
        // チーム作成のためのパケットを生成
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)
        // チーム名を設定。この場合は色の名前にしています。
        packet.strings.write(0,"$color")
        // 操作内容は0、つまりチームの新規作成
        packet.integers.write(0,0)
        // チームの情報格納用のos
        val internalStructure = packet.optionalStructures.read(0).get()
        // チームの表示名を設定。個々の場合は色の名前
        internalStructure.chatComponents.write(0, WrappedChatComponent.fromText("$color"))
        // 当たり判定やネームタグの表示/非表示を設定。
        internalStructure.integers.write(0,0x01)
        internalStructure.strings.write(0,"always")
        internalStructure.strings.write(1,"always")
        // チームの色を設定
        internalStructure.getEnumModifier(ChatColor::class.java, MinecraftReflection.getMinecraftClass("EnumChatFormat")).write(0,color)
        // 作成した情報をパケットに収納
        packet.optionalStructures.write(0, Optional.of(internalStructure))
        // チームのプレイヤー一覧を格納
        packet.getSpecificModifier(Collection::class.java).write(0,TEAMS[player]?.get(color)?:listOf<String>())
        return packet
    }

    /**
     * チームのメンバーを設定できる関数です。
     */
    fun set(player: Player,color: ChatColor,players: List<Player>) {
        val colours = TEAMS[player]?: mutableMapOf()
        colours[color] = players.map { it.name }.toMutableList()
        TEAMS[player] = colours
    }

    /**
     * チームからプレイヤーを削除数パケットです。
     */
    fun remove(player: Player, color: ChatColor, entities: List<String>) {
        val teams = TEAMS[player]?.get(color)?: return
        teams.removeAll(entities)
        TEAMS[player]?.put(color, teams)
        // パケットを作成
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)
        // チーム名を指定
        packet.strings.write(0,"$color")
        // 操作を４，つまりプレイヤーの削除に設定
        packet.integers.write(0,4)
        // 削除するプレイヤーを格納
        packet.getSpecificModifier(Collection::class.java).write(0,entities)
        // パケットを送信
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, packet)
    }

    /**
     * チームを削除するパケットです。
     */
    fun removeTeam(player: Player, color: ChatColor) {
        // チームがもとから存在しない場合はreturn
        if(TEAMS[player]?.contains(color)!=true) { return }
        // パケットを作成
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.SCOREBOARD_TEAM)
        // 削除するチーム名を指定
        packet.strings.write(0,"$color")
        // 操作を１，つまりチームの削除に設定
        packet.integers.write(0,1)
        // パケットを送信
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, packet)
    }

    /**
     * プレイヤー全員にチームを作成するパケットを送信する
     */
    fun sendAll() {
        TEAMS.forEach { (player, map) -> map.forEach { (color, _) -> WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player,createTeamColorPacket(player,color)) } }
    }

    init {
        // プレイヤー参加時にチームを作成するパケットを送信するt
        WereWolf3.INSTANCE.registerEvent<PlayerJoinEvent> { event ->
            TEAMS[event.player]?.forEach { (color, _) -> WereWolf3.PROTOCOL_MANAGER.sendServerPacket(event.player, createTeamColorPacket(event.player,color)) }
        }
    }
}
