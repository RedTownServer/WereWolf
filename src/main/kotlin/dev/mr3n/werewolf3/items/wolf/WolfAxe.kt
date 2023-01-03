package dev.mr3n.werewolf3.items.wolf

import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.utils.damageTo
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerItemHeldEvent

object WolfAxe: IShopItem.ShopItem("wolf_axe",Material.STONE_AXE) {
    override val displayName: String = languages("item.$id.name")

    private val WOLF_AXE_TITLE_TEXT = titleText("item.$id.title.wolf_axe")

    private val SUCCESS_TITLE_TEXT = titleText("item.$id.title.success")

    private val FAILED_TITLE_TEXT = titleText("item.$id.title.failed")

    private val CHARGE: Int = constant("charge")

    private val CHARGES = mutableMapOf<Player, Int>()

    init {
        WereWolf3.INSTANCE.registerEvent<EntityDamageByEntityEvent> { event ->
            val player = event.damager
            val target = event.entity
            if(player !is Player) { return@registerEvent }
            if(target !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(target)) { return@registerEvent }
            val item = player.inventory.itemInMainHand
            if(!isSimilar(item)) { return@registerEvent }
            val charge = CHARGES[player]?:0
            val world = player.world
            if(charge >= CHARGE) {
                item.amount--
                player.sendTitle(SUCCESS_TITLE_TEXT,messages("used"),0,30,0)
                world.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 2f, 0.4f)
                world.playSound(player, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2f, 2f)
                player.damageTo(target, -1.0)
                CHARGES[player] = 0
            } else {
                player.sendTitle(FAILED_TITLE_TEXT,messages("not_enough_charge"),0,30,0)
                world.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 1f)
                CHARGES[player] = 0
                event.isCancelled = true
            }
        }
        WereWolf3.INSTANCE.registerEvent<PlayerItemHeldEvent> { event ->
            CHARGES[event.player] = 0
        }
        WereWolf3.INSTANCE.runTaskTimer(20L,20L) {
            WereWolf3.PLAYERS.forEach { player ->
                val item = player.inventory.itemInMainHand
                if(!isSimilar(item)) { return@forEach }
                val world = player.world
                val charge = CHARGES[player]?:0
                if(charge >= CHARGE) {
                    player.sendTitle(WOLF_AXE_TITLE_TEXT,messages("charged"),0,30,0)
                } else {
                    CHARGES[player] = charge + 20
                    player.sendTitle(WOLF_AXE_TITLE_TEXT,messages("charging"),0,1,20)
                    if(charge + 20 >= CHARGE) {
                        world.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 0f)
                        player.sendTitle(WOLF_AXE_TITLE_TEXT,messages("charged"),0,30,0)
                    } else {
                        world.playSound(player, Sound.BLOCK_BEACON_POWER_SELECT, 1f, 2f)
                    }
                }
            }
        }
    }
}