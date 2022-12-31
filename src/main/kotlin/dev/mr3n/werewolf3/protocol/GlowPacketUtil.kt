package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.wrappers.WrappedDataValue
import com.comphenix.protocol.wrappers.WrappedDataWatcher
import dev.mr3n.werewolf3.WereWolf3
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import kotlin.experimental.or

object GlowPacketUtil {
    private val BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType)

    private val PLAYERS = mutableMapOf<UUID, MutableList<Int>>()

    private val ENTITY_MAPPING = mutableMapOf<Int, Entity>()

    /**
     * 発行するプレイヤーを追加します。
     */
    fun add(player: Player, entity: Player) {
        // PLAYERSから発行するプレイヤー一覧を取得
        val entities = PLAYERS[player.uniqueId]?: mutableListOf()
        // エンティティのIDマッピングに発行させるentityを追加
        ENTITY_MAPPING[entity.entityId] = entity
        // 発光するプレイヤー一覧にentityを追加
        entities.add(entity.entityId)
        // entityを追加した一覧を保存
        PLAYERS[player.uniqueId] = entities
        // 発行するパケットを作成して送信
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player,createMetadataPacket(entity))
    }

    /**
     * entityの発光を削除する関数です。
     */
    fun remove(player: Player, entity: Entity) {
        // 発光するエンティティ一覧からプレイヤーを削除
        PLAYERS[player.uniqueId]?.remove(entity.entityId)
        // 削除するパケットを送信
        WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, createMetadataResetPacket(entity))
    }

    fun removeAll(player: Player) {
        val entities = PLAYERS[player.uniqueId]?:return
        entities.map { ENTITY_MAPPING[it] }.filterNotNull().forEach { entity -> remove(player, entity) }
    }

    /**
     * エンティティが発行していないというメタデータが保存されたパケットを作成
     */
    fun createMetadataResetPacket(entity: Entity): PacketContainer {
        // メタデータのパケットを作成
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA)
        // 編集対象のエンティティを指定
        packet.integers.write(0, entity.entityId)
        // 発光情報を書き込んでいない素のdwを生成
        val dataWatcher = WrappedDataWatcher.getEntityWatcher(entity)
        // それをパケットに書き込む
        val wrappedDataValueList = dataWatcher.watchableObjects.map { WrappedDataValue(it.watcherObject.index, it.watcherObject.serializer, it.rawValue) }
        packet.dataValueCollectionModifier.write(0, wrappedDataValueList)
        return packet
    }

    /**
     * エンティティが発行しているというメタデータが保存されたパケットを作成
     */
    fun createMetadataPacket(entity: Entity): PacketContainer {
        // メタデータのパケットを作成
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA)
        // 編集対象のエンティティを指定
        packet.integers.write(0, entity.entityId)
        // 発光情報が保存されているdwを取得
        val dataWatcher = createDataWatcher(entity)
        // そのdwをパケットに書き込む
        val wrappedDataValueList = dataWatcher.watchableObjects.map { WrappedDataValue(it.watcherObject.index, it.watcherObject.serializer, it.rawValue) }
        packet.dataValueCollectionModifier.write(0, wrappedDataValueList)
        return packet
    }

    /**
     * エンティティの発光情報を保存したdwを生成
     */
    fun createDataWatcher(entity: Entity): WrappedDataWatcher {
        val dataWatcher = WrappedDataWatcher.getEntityWatcher(entity).deepClone()
        dataWatcher.entity = entity
        dataWatcher.setObject(0, BYTE_SERIALIZER, dataWatcher.getByte(0).or(0x40))
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
                // エンティティが発行している必要がある場合
                if(PLAYERS[player.uniqueId]?.contains(entityId)==true) {
                    // entityを取得
                    val entity = ENTITY_MAPPING[entityId]?:return
                    // entityの発光情報が格納されているdwを取得
                    val dataWatcher = createDataWatcher(entity)
                    // dwに発行しているという情報を書き込む
                    val wrappedDataValueList = dataWatcher.watchableObjects.map { WrappedDataValue(it.watcherObject.index, it.watcherObject.serializer, it.rawValue) }
                    // そのdwをパケットに書き込む
                    packet.dataValueCollectionModifier.write(0, wrappedDataValueList)
                    // 編集したパケットを送信する
                    event.packet = packet
                }
            }
        })
    }
}