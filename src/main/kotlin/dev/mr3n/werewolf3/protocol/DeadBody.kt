package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.wrappers.*
import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Constants
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.events.WereWolf3DeadBodyClickEvent
import dev.mr3n.werewolf3.utils.*
import org.bukkit.*
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.experimental.or

class DeadBody(val player: Player) {
    var wasFound = false
        private set

    val role = player.role

    val time = System.currentTimeMillis()

    private val co = player.co

    /**
     * 死体が発見された際に呼び出す関数です。
     */
    fun found(player: Player) {
        if(wasFound) { return }
        wasFound = true
        // 死体が発見された際に推定プレイヤー数を一つ減らす。
        WereWolf3.PLAYERS_EST--
        player.money += Constants.DEAD_BODY_PRIZE
        WereWolf3.PLAYERS.forEach { player2 ->
            player2.sendMessage(languages("messages.found_dead_body", "%player%" to name).asPrefixed())
        }
        player.playSound(player, Sound.UI_BUTTON_CLICK, 1f, 1f)
        if(co!=null) {
            this.player.setPlayerListName("${co.color}${ChatColor.STRIKETHROUGH}[${co.displayName}Co]${this.player.name}")
        } else {
            this.player.setPlayerListName("${ChatColor.STRIKETHROUGH}${this.player.name}")
        }
    }

    val name = player.name

    val will = player.will?:languages("none")

    // お願いだからかぶらないでね...いやまじで。
    private val entityId = (0..Int.MAX_VALUE).random()

    // これはかぶらんやろ！
    private val uniqueId = UUID.randomUUID()

    private val playerUniqueId = player.uniqueId

    val location = player.location.clone()

    private val armorStand = player.world.spawn(location.clone().subtract(0.0,0.5,0.0), ArmorStand::class.java)

    private val gameProfile = WrappedGameProfile(uniqueId, player.name)

    private val showedPlayers = mutableListOf<Player>()

    private val helmet = player.inventory.helmet?.clone()

    private val chestPlate = player.inventory.chestplate?.clone()

    private val leggings = player.inventory.leggings?.clone()

    private val boots = player.inventory.boots?.clone()

    private val mainHand = player.inventory.itemInMainHand.clone()

    private val offHand = player.inventory.itemInOffHand.clone()

    fun onClick(player: Player) {
        val event = WereWolf3DeadBodyClickEvent(player, this)
        Bukkit.getPluginManager().callEvent(event)
        if(event.isCancelled) { return }
        found(player)
    }

    init {
        DEAD_BODY_BY_UUID[playerUniqueId]?.destroy()
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
        DEAD_BODY_BY_UUID[playerUniqueId] = this
    }

    fun hide(players1: List<Player>) {
        val players = players1.filter { showedPlayers.contains(it) }
        if(players.isEmpty()) { return }
        sendMetadataPacket(0.toByte().or(0x20),players)
        showedPlayers.removeAll(players)
    }

    fun show(players1: List<Player>) {
        val players = players1.filterNot { showedPlayers.contains(it) }
        if(players.isEmpty()) { return }
        sendMetadataPacket(0,players)
        showedPlayers.addAll(players)
    }

    private fun sendMetadataPacket(byte: Byte, players: List<Player>) {
        val entityMetadata = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_METADATA)
        entityMetadata.integers.writeSafely(0, entityId)
        val dataWatcher = WrappedDataWatcher()
        dataWatcher.entity
        dataWatcher.setObject(0, BYTE_SERIALIZER, byte)
        entityMetadata.dataValueCollectionModifier.writeSafely(0, dataWatcher.watchableObjects.map { WrappedDataValue(it.watcherObject.index, it.watcherObject.serializer, it.rawValue) })
        players.forEach { p -> WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,entityMetadata) }
        showedPlayers.addAll(players)
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

        // 装備をつける
        val setEquipment = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_EQUIPMENT)
        setEquipment.integers.writeSafely(0, entityId)
        val equipments = listOf(
            Pair(EnumWrappers.ItemSlot.HEAD, helmet),
            Pair(EnumWrappers.ItemSlot.CHEST, chestPlate),
            Pair(EnumWrappers.ItemSlot.LEGS, leggings),
            Pair(EnumWrappers.ItemSlot.FEET, boots),
            Pair(EnumWrappers.ItemSlot.MAINHAND, mainHand),
            Pair(EnumWrappers.ItemSlot.OFFHAND, offHand)
        )
        setEquipment.slotStackPairLists.writeSafely(0, equipments)

        players.forEach { p ->
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,playerInfo)
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,namedEntitySpawn)
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,entityMetadata)
            WereWolf3.PROTOCOL_MANAGER.sendServerPacket(p,setEquipment)
        }
        show(players)
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
        ARMOR_STANDS.remove(armorStand.entityId)
        armorStand.remove()
        // 一覧からも削除
        DEAD_BODIES.remove(this)
        DEAD_BODY_BY_UUID.remove(playerUniqueId)
    }

    companion object {
        private const val ENTITY_TYPE = "DEAD_BODY_MARKER"

        // 現在存在している死体の一覧です。
        val DEAD_BODIES = CopyOnWriteArrayList<DeadBody>()

        private val BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType)

        private val ARMOR_STANDS = mutableMapOf<Int, DeadBody>()

        val DEAD_BODY_BY_UUID = mutableMapOf<UUID, DeadBody>()

        init {
            WereWolf3.INSTANCE.runTaskTimer(10,10) {
                DEAD_BODIES.forEach { deadBody ->
                    val location = deadBody.location.clone()
                    location.world?.spawnParticle(Particle.REDSTONE, location.add(0.0,0.5,0.0),10,0.0, 0.0, 0.0, Particle.DustOptions(if(deadBody.wasFound) Color.AQUA else Color.RED, 1f))
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