package dev.mr3n.werewolf3.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.wrappers.*
import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.Executor.Companion.runTaskTimerAsync
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Constants
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.events.WereWolf3DeadBodyClickEvent
import dev.mr3n.werewolf3.utils.*
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Frog
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.EquipmentSlot
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
        player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
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

    var location = player.location.clone()
        private set

    private val yaw = location.yaw

    private val pitch = location.pitch

    private val frog = player.world.spawn(location.clone(), Frog::class.java)

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
        frog.isInvisible = true
        frog.isInvulnerable = true
        frog.isSilent = true
        frog.ageLock = true
        frog.isCollidable = false
        frog.isAware = false
        frog.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED)?.baseValue = 0.0
        // 死体一覧にしたいを追加
        DEAD_BODIES.add(this)
        FROGS[frog.entityId] = this
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
            .writeSafely(1, location.y + 0.12)
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

    fun sync() {
        if(this.location.x == frog.location.x && this.location.y == frog.location.y && this.location.z == frog.location.z) { return }
        val packet = WereWolf3.PROTOCOL_MANAGER.createPacket(PacketType.Play.Server.ENTITY_TELEPORT)
        packet.integers
            .writeSafely(0, entityId)
        packet.bytes
            .writeSafely(0,((yaw / 360f) * 256f).toInt().toByte())
            .writeSafely(1,((pitch / 360f) * 256f).toInt().toByte())
        packet.doubles
            .writeSafely(0, frog.location.x)
            .writeSafely(1, frog.location.y + 0.12)
            .writeSafely(2, frog.location.z)
        Bukkit.getOnlinePlayers().forEach { player -> WereWolf3.PROTOCOL_MANAGER.sendServerPacket(player, packet) }
        this.location = frog.location
    }

    fun teleport(location: Location) {
        frog.teleport(location.clone())
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
        FROGS.remove(frog.entityId)
        frog.remove()
        // 一覧からも削除
        DEAD_BODIES.remove(this)
        DEAD_BODY_BY_UUID.remove(playerUniqueId)
        CARRYING.filterValues { it == this }.keys.forEach { CARRYING.remove(it) }
    }

    companion object {
        private const val ENTITY_TYPE = "DEAD_BODY_MARKER"

        // 現在存在している死体の一覧です。
        val DEAD_BODIES = CopyOnWriteArrayList<DeadBody>()

        private val BYTE_SERIALIZER = WrappedDataWatcher.Registry.get(Byte::class.javaObjectType)

        private val FROGS = mutableMapOf<Int, DeadBody>()

        val DEAD_BODY_BY_UUID = mutableMapOf<UUID, DeadBody>()

        val CARRYING = mutableMapOf<Player, DeadBody>()

        init {
            /**
             * 死体のクリック判定のところにパーティクルを出す処理
             */
            WereWolf3.INSTANCE.runTaskTimer(10,10) {
                DEAD_BODIES.forEach { deadBody ->
                    val location = deadBody.location.clone()
                    location.world?.spawnParticle(Particle.REDSTONE, location.clone().add(.0, .3, .0),10,0.0, 0.0, 0.0, Particle.DustOptions(if(deadBody.wasFound) Color.AQUA else Color.RED, 1f))
                }
            }
            WereWolf3.INSTANCE.registerEvent<PlayerInteractAtEntityEvent> { event ->
                val player = event.player
                if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
                if(event.hand != EquipmentSlot.HAND) { return@registerEvent }
                val entity = event.rightClicked
                FROGS[entity.entityId]?.onClick(player)
            }

            WereWolf3.INSTANCE.runTaskTimerAsync(1L,1L) {
                DEAD_BODIES.forEach { deadBody ->
                    deadBody.sync()
                }
            }

            /**
             * 死体を運んでいるときにタイトルを表示する
             */
            WereWolf3.INSTANCE.runTaskTimer(20, 20) {
                CARRYING.forEach { (player, _) -> player.sendTitle(titleText("carrying_dead_body.carrying"),languages("carrying_dead_body.carrying.subtitle"), 0, 30, 20) }
            }

            /**
             * 死体を運ぶ処理
             */
            WereWolf3.INSTANCE.registerEvent<PlayerMoveEvent> { event ->
                val player = event.player
                if(player.isSneaking) {
                    // if:スニークしていたら死体をプレイヤーの足元にtp
                    val deadBody = CARRYING[player] ?: return@registerEvent
                    deadBody.teleport(player.location)
                }
            }

            /**
             * スニークを解除したら死体を離す
             */
            WereWolf3.INSTANCE.registerEvent<PlayerToggleSneakEvent> { event ->
                if(!event.isSneaking && CARRYING.containsKey(event.player)) {
                    CARRYING.remove(event.player)
                    event.player.playSound(event.player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 2f, 1f)
                    event.player.sendTitle(titleText("carrying_dead_body.let_go"),languages("carrying_dead_body.let_go.subtitle"), 0, 30, 20)
                }
            }

            /**
             * 死体を運ぶ前処理(クリック部分)
             */
            WereWolf3.INSTANCE.registerEvent<WereWolf3DeadBodyClickEvent>(p = EventPriority.HIGHEST, ic = true) { event ->
                val player = event.player
                // スニークしていなかったらreturn
                if(!player.isSneaking) { return@registerEvent }
                if(player.gameMode==GameMode.SPECTATOR) { return@registerEvent }
                // すでに運搬中の死体だった場合return
                if(CARRYING.values.contains(event.deadBody)) { return@registerEvent }
                event.isCancelled = true
                if(player.location.distance(event.deadBody.location) <= 1) {
                    CARRYING[player] = event.deadBody
                    event.deadBody.teleport(event.player.location)
                    player.playSound(player, Sound.ITEM_ARMOR_EQUIP_LEATHER, 2f, 1f)
                    player.sendTitle(titleText("carrying_dead_body.carrying"),languages("carrying_dead_body.carrying.subtitle"), 0, 30, 20)
                } else {
                    player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 2f, 1f)
                    player.sendMessage(languages("carrying_dead_body.too_far").asPrefixed())
                }
            }
        }
    }
}