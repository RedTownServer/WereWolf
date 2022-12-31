package dev.mr3n.werewolf3.items.doctor

import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.utils.asPrefixed
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector

object HealthCharger: IShopItem.ShopItem(Material.REDSTONE_ORE) {
    override val id: String = "health_charger"

    override val price: Int = 300

    private val HEAL_AMOUNT: Double = constant("heal_amount")

    private val DISTANCE: Double = constant("distance")

    private val CHARGER_TITLE_TEXT = titleText("item.$id.title.charger")

    val CHARGERS = mutableListOf<Location>()

    override fun onEnd() { CHARGERS.forEach { it.block.type = Material.AIR } }

    init {
        WereWolf3.INSTANCE.runTaskTimer(0L, 35L) {
            CHARGERS.forEach { chargerLoc ->
                WereWolf3.PLAYERS.filter { it.location.distance(chargerLoc) <= DISTANCE }.forEach { player ->
                    player.sendTitle(CHARGER_TITLE_TEXT, messages("healing"), 0, 1, 20)
                    player.health = minOf(player.healthScale, player.health + HEAL_AMOUNT)
                    player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 2f)
                }
            }
        }
        WereWolf3.INSTANCE.registerEvent<PlayerInteractEvent> { event ->
            if(event.hand!=EquipmentSlot.HAND) { return@registerEvent }
            if(event.action!=Action.RIGHT_CLICK_BLOCK) { return@registerEvent }
            val player = event.player
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            val item = player.inventory.itemInMainHand
            if(!isSimilar(item)) { return@registerEvent }
            val clickedBlock = event.clickedBlock?:return@registerEvent
            val face = event.blockFace
            val placedLocation = clickedBlock.location.clone().add(Vector(face.modX,face.modY,face.modZ))
            event.isCancelled = true
            val placeable = (-1..1).all { x -> (-1..1).all { z -> (0..2).all { y ->
                placedLocation.clone().add(Vector(x,y,z)).block.isEmpty
            } } }
            if(placeable) {
                item.amount--
                placedLocation.block.type = Material.REDSTONE_ORE
                CHARGERS.add(placedLocation)
                player.world.playSound(player,Sound.BLOCK_STONE_PLACE, 1f, 1f)
                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f)
            } else {
                player.sendMessage(messages("cant_placeable").asPrefixed())
                player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f)
            }
        }
    }
}