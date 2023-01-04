package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.mr3n.werewolf3.WereWolf3
import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import kotlin.experimental.or

object MetadataPacketUtil {
    private val BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType)

    private val GLOWING_PLAYERS = mutableMapOf<UUID, MutableList<Int>>()

    private val INVISIBLE_PLAYERS = mutableMapOf<UUID, MutableList<Int>>()

    private val ENTITY_MAPPING = mutableMapOf<Int, Entity>()

    /**
     * 発行するプレイヤーを追加します。
     */
    fun addToGlowing(player: Player, entity: Player) {
        // PLAYERSから発行するプレイヤー一覧を取得
        val entities = GLOWING_PLAYERS[player.uniqueId]?: mutableListOf()
        // エンティティのIDマッピングに発行させるentityを追加
        ENTITY_MAPPING[entity.entityId] = entity
        // 発光するプレイヤー一覧にentityを追加
        entities.add(entity.entityId)
        // entityを追加した一覧を保存
        GLOWING_PLAYERS[player.uniqueId] = entities
        // 発行するパケットを作成して送信
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player,createMetadataPacket(player, entity))
    }

    /**
     * entityの発光を削除する関数です。
     */
    fun removeFromGlowing(player: Player, entity: Entity) {
        // 発光するエンティティ一覧からプレイヤーを削除
        GLOWING_PLAYERS[player.uniqueId]?.remove(entity.entityId)
        // 削除するパケットを送信
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, createMetadataPacket(player, entity))
    }

    fun removeAllGlowing(player: Player) {
        val entities = GLOWING_PLAYERS[player.uniqueId]?:return
        entities.mapNotNull { ENTITY_MAPPING[it] }.forEach { entity -> removeFromGlowing(player, entity) }
    }

    fun addToInvisible(player: Player, entity: Player) {
        if(player==entity) { return }
        // PLAYERSから発行するプレイヤー一覧を取得
        val entities = INVISIBLE_PLAYERS[player.uniqueId]?: mutableListOf()
        if(entities.contains(entity.entityId)) { return }
        // エンティティのIDマッピングに発行させるentityを追加
        ENTITY_MAPPING[entity.entityId] = entity
        // 発光するプレイヤー一覧にentityを追加
        entities.add(entity.entityId)
        // entityを追加した一覧を保存
        INVISIBLE_PLAYERS[player.uniqueId] = entities
        // 発行するパケットを作成して送信
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player,createMetadataPacket(player, entity))
    }

    fun removeFromInvisible(player: Player, entity: Entity) {
        if(player==entity) { return }
        // 発光するエンティティ一覧からプレイヤーを削除
        val entities = INVISIBLE_PLAYERS[player.uniqueId]?: mutableListOf()
        if(!entities.contains(entity.entityId)) { return }
        entities.remove(entity.entityId)
        INVISIBLE_PLAYERS[player.uniqueId] = entities
        // 削除するパケットを送信
        WereWolf3.INSTANCE.runTaskLater(1L) {
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, createMetadataPacket(player, entity))
        }
    }

    fun removeAllInvisible(player: Player) {
        val entities = INVISIBLE_PLAYERS[player.uniqueId]?:return
        entities.mapNotNull { ENTITY_MAPPING[it] }.forEach { entity -> removeFromGlowing(player, entity) }
    }

    fun resetAll(player: Player) {
        GLOWING_PLAYERS.remove(player.uniqueId)
        Bukkit.getOnlinePlayers().forEach { entity ->
            // 発光するエンティティ一覧からプレイヤーを削除
            GLOWING_PLAYERS[player.uniqueId]?.remove(entity.entityId)
            // 削除するパケットを送信
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, createMetadataPacket(player, entity))
        }
    }

    /**
     * エンティティが発行しているというメタデータが保存されたパケットを作成
     */
    private fun createMetadataPacket(player: Player, entity: Entity): PacketContainer {
        // メタデータのパケットを作成
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA)
        // 編集対象のエンティティを指定
        packet.integers.write(0, entity.entityId)
        // 発光情報が保存されているdwを取得
        val dataWatcher = createDataWatcher(player, entity)
        // そのdwをパケットに書き込む
        val wrappedDataValueList = dataWatcher.watchableObjects.map { WrappedDataValue(it.watcherObject.index, it.watcherObject.serializer, it.rawValue) }
        packet.dataValueCollectionModifier.write(0, wrappedDataValueList)
        return packet
    }

    /**
     * エンティティの発光情報を保存したdwを生成
     */
    fun createDataWatcher(player: Player, entity: Entity): WrappedDataWatcher {
        val dataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone()
        dataWatcher.entity = entity
        var byte = dataWatcher.getByte(0)
        if(INVISIBLE_PLAYERS[player.uniqueId]?.contains(entity.entityId)==true) {
            byte = byte.or(0x20)
        }
        if(GLOWING_PLAYERS[player.uniqueId]?.contains(entity.entityId)==true) {
            byte = byte.or(0x40)
        }
        dataWatcher.setObject(0, BYTE_SERIALIZER, byte)
        return dataWatcher
    }

    init {
        WereWolf3.PROTOCOL_MANAGER.addPacketListener(object: PacketAdapter(WereWolf3.INSTANCE,PacketType.Play.Server.ENTITY_METADATA) {
            override fun onPacketSending(event: PacketEvent) {
                // パケットを送信する先
                val player = event.player
                // パケット
                val packet = event.packet.deepClone()
                // エンティティID
                val entityId = packet.integers.read(0)
                // entityを取得
                val entity = ENTITY_MAPPING[entityId]?:return
                // entityの発光情報が格納されているdwを取得
                val dataWatcher = createDataWatcher(player, entity)
                val skinLayers = WrappedDataWatcher.WrappedDataWatcherObject(17, WrappedDataWatcher.Registry.get(Byte::class.javaObjectType))
                dataWatcher.setObject(skinLayers, (0x01 or 0x02 or 0x04 or 0x08 or 0x10 or 0x20 or 0x40).toByte())
                // エンティティが発行している必要がある場合
                // dwに発行しているという情報を書き込む
                val wrappedDataValueList = dataWatcher.watchableObjects.map { WrappedDataValue(it.watcherObject.index, it.watcherObject.serializer, it.rawValue) }
                // そのdwをパケットに書き込む
                packet.dataValueCollectionModifier.write(0, wrappedDataValueList)
                // 編集したパケットを送信する
                event.packet = packet
            }
        })
    }
}