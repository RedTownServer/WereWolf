package dev.mr3n.werewolf3.items.wolf

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

object AssassinSword: IShopItem.ShopItem(Material.STONE_AXE) {
    override val id: String = "assassin_sword"

    override val displayName: String = languages("item.$id.name")

    override val price: Int = 300

    private val WOLF_AXE_TITLE_TEXT = titleText("item.$id.title.wolf_axe")

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
                player.sendTitle(WOLF_AXE_TITLE_TEXT,messages("used"),0,30,0)
                world.playSound(player, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 2f, 0.4f)
                world.playSound(player, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 2f, 2f)
                player.damageTo(target, 1000000.0)
                item.amount--
                CHARGES[player] = 0
            }
        }
    }
}