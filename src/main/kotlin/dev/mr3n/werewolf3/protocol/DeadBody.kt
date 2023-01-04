package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.wrappers.*
import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.events.WereWolf3DeadBodyClickEvent
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Particle
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class DeadBody(val player: Player) {

    private val ENTITY_TYPE = "DEAD_BODY_MARKER"

    val name = player.name

    // お願いだからかぶらないでね...いやまじで。
    val entityId = (0..Int.MAX_VALUE).random()

    // これはかぶらんやろ！
    val uniqueId = UUID.randomUUID()

    val location = player.location.clone()

    val armorStand = player.world.spawn(location.clone().subtract(0.0,0.5,0.0), ArmorStand::class.java)

    val gameProfile = WrappedGameProfile(uniqueId, player.name)

    fun onClick(player: Player) {
        Bukkit.getPluginManager().callEvent(WereWolf3DeadBodyClickEvent(player, this))
    }

    init {
        spawn(Bukkit.getOnlinePlayers().toList())
        armorStand.isInvisible = true
        armorStand.isSmall = true
        armorStand.isInvulnerable = true
        armorStand.isSilent = true
        armorStand.setGravity(false)
        armorStand.setAI(false)
        armorStand.persistentDataContainer.set(Keys.ENTITY_TYPE, PersistentDataType.STRING, ENTITY_TYPE)
        // 死体一覧にしたいを追加
        DEAD_BODIES.add(this)
        ARMOR_STANDS[armorStand.entityId] = this
    }

    fun spawn(players: List<Player>) {
        // エンティティをすぽーんさせるパケット。ここでエンティティーの情報を送信する。
        val namedEntitySpawn = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.NAMED_ENTITY_SPAWN)
        namedEntitySpawn.integers
            .writeSafely(0, entityId)
        namedEntitySpawn.uuiDs
            .writeSafely(0, uniqueId)
        namedEntitySpawn.doubles
            .writeSafely(0, location.x)
            .writeSafely(1, location.y)
            .writeSafely(2, location.z)
        namedEntitySpawn.bytes
            .writeSafely(0, ((location.yaw*256.0f)/360.0f).toInt().toByte())
            .writeSafely(1, ((location.pitch*256.0f)/360.0f).toInt().toByte())
        // プレイヤーの情報を送信するパケット。ここでプレイヤーのスキンや名前などを送信する
        val playerInfo = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.PLAYER_INFO)
        playerInfo.playerInfoActions.writeSafely(0,setOf(EnumWrappers.PlayerInfoAction.ADD_PLAYER))
        val playerSkin = WrappedGameProfile.fromPlayer(player).properties["textures"].first()
        gameProfile.properties.put("textures", WrappedSignedProperty(playerSkin.name, playerSkin.value, playerSkin.signature))
        playerInfo.playerInfoDataLists.writeSafely(1, listOf(PlayerInfoData(gameProfile,player.ping,EnumWrappers.NativeGameMode.ADVENTURE, WrappedChatComponent.fromText(player.displayName),null)))

        // メタデータを送信するパケット。ここでスキンのセカンドレイヤーの情報やプレイヤーのポーズなどを設定する
        val entityMetadata = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA)
        entityMetadata.integers.writeSafely(0, entityId)
        val dataWatcher = WrappedDataWatcher()
        // 参考: https://wiki.vg/Entity_metadata#Player
        val skinLayers = WrappedDataWatcher.WrappedDataWatcherObject(17, WrappedDataWatcher.Registry.get(Byte::class.javaObjectType))
        dataWatcher.setObject(skinLayers, (0x01 or 0x02 or 0x04 or 0x08 or 0x10 or 0x20 or 0x40).toByte())
        // 参考: https://wiki.vg/Entity_metadata#Entity
        val pose = WrappedDataWatcher.WrappedDataWatcherObject(6, WrappedDataWatcher.Registry.get(EnumWrappers.getEntityPoseClass()))
        dataWatcher.setObject(pose,  EnumWrappers.EntityPose.SLEEPING)
        entityMetadata.dataValueCollectionModifier.writeSafely(0, dataWatcher.watchableObjects.map { WrappedDataValue(it.watcherObject.index, it.watcherObject.serializer, it.rawValue) })
        players.forEach { p ->
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,playerInfo)
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,namedEntitySpawn)
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,entityMetadata)
        }
    }

    /**
     * 死体を消します。
     */
    fun destroy() {
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_DESTROY)
        packet.intLists.writeSafely(0, listOf(entityId))
        Bukkit.getOnlinePlayers().forEach { p ->
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,packet)
        }
        armorStand.remove()
        // 一覧からも削除
        DEAD_BODIES.remove(this)
    }

    companion object {
        // 現在存在している死体の一覧です。
        val DEAD_BODIES = CopyOnWriteArrayList<DeadBody>()

        private val ARMOR_STANDS = mutableMapOf<Int, DeadBody>()

        init {
            WereWolf3.INSTANCE.runTaskTimer(10,10) {
                DEAD_BODIES.forEach { deadBody ->
                    val location = deadBody.location.clone()
                    location.world?.spawnParticle(Particle.REDSTONE, location.add(0.0,0.5,0.0),10,0.0, 0.0, 0.0, Particle.DustOptions(Color.RED, 1f))
                }
            }

            WereWolf3.INSTANCE.registerEvent<PlayerInteractAtEntityEvent> { event ->
                val player = event.player
                if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
                val entity = event.rightClicked
                ARMOR_STANDS[entity.entityId]?.onClick(player)
            }
        }
    }
}