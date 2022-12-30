package dev.mr3n.werewolf3.items

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.damageTo
import dev.mr3n.werewolf3.utils.languages
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

object GlowInk: IShopItem.ShopItem(Material.GLOW_INK_SAC) {
    override val id: String = "glow_ink"

    override val displayName: String = languages("item.${id}.name")

    override val price: Int = 300

    private val GLOWING_TIME: Long = constant("glowing_time")

    private val GLOW_TITLE_TEXT = titleText("item.${id}.title.glowing")

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
            val task = WereWolf3.INSTANCE.runTaskTimer(0L,20L) {
                WereWolf3.PLAYERS.forEach { player ->
                    player.playSound(player,Sound.ENTITY_BEE_STING,2F,0F)
                    player.sendTitle(GLOW_TITLE_TEXT, messages("glowing"), 0, 5, 30)
                    player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false))
                }
            }
            WereWolf3.INSTANCE.runTaskLater(GLOWING_TIME) {
                task.cancel()
                WereWolf3.PLAYERS.forEach { player ->
                    player.removePotionEffect(PotionEffectType.GLOWING)
                }
            }
        }
    }
}