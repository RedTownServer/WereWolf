package dev.mr3n.werewolf3.items.wolf

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.damageTo
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.role
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.block.Action
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object LightningRod: IShopItem.ShopItem(Material.LIGHTNING_ROD) {
    override val id: String = "lightning_rod"

    override val displayName: String = languages("item.${id}.name")

    override val price: Int = 300

    private val BLINDNESS_TIME: Long = constant("blindness_time")

    private val BLINDNESS_TITLE_TEXT = titleText("item.${id}.title.blindness")

    private var blindness = 0L

    init {
        WereWolf3.INSTANCE.registerEvent<PlayerInteractEvent> { event ->
            // main handじゃない場合はreturn
            if(event.hand!=EquipmentSlot.HAND) { return@registerEvent }
            // 右クリックしていない場合はreturn
            if(event.action!=Action.RIGHT_CLICK_AIR&&event.action!=Action.RIGHT_CLICK_BLOCK) { return@registerEvent }
            val player = event.player
            val item = player.inventory.itemInMainHand
            // ピカピカインクを持っていない場合はreturn
            if(!isSimilar(item)) { return@registerEvent }
            item.amount--
            player.playSound(player, Sound.ENTITY_GLOW_SQUID_SQUIRT, 2f, 1f)
            blindness = BLINDNESS_TIME
            WereWolf3.PLAYERS.forEach { player1 ->
                player1.playSound(player1, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1f, 1f)
                if(player1.role==Role.WOLF) {
                    player1.sendTitle(BLINDNESS_TITLE_TEXT, messages("for_wolf", "%sec%" to blindness / 20), 0, 30, 10)
                } else {
                    player1.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 30, 200, false, false, false))
                    player1.sendTitle(BLINDNESS_TITLE_TEXT, messages("blindness", "%sec%" to blindness / 20), 0, 40, 10)
                }
            }
        }
        WereWolf3.INSTANCE.runTaskTimer(0L,20L) {
            if(blindness>0) {
                blindness-=20
                WereWolf3.PLAYERS.forEach { player1 ->
                    if(player1.role==Role.WOLF) {
                        player1.sendTitle(BLINDNESS_TITLE_TEXT, messages("for_wolf", "%sec%" to blindness / 20), 0, 30, 10)
                    } else {
                        player1.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 40, 200, false, false, false))
                        player1.sendTitle(BLINDNESS_TITLE_TEXT, messages("blindness", "%sec%" to blindness / 20), 0, 30, 10)
                    }
                }
            }
        }
    }
}