package dev.mr3n.werewolf3.items

import dev.moru3.minepie.Executor.Companion.runTaskTimer
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object GlowInk: IShopItem.ShopItem(Material.GLOW_INK_SAC) {
    override val id: String = "glow_ink"

    override val displayName: String = languages("item.${id}.name")

    override val price: Int = 300

    private val GLOWING_TIME: Long = constant("glowing_time")

    private val GLOW_TITLE_TEXT = titleText("item.${id}.title.glowing")

    private var glowing = 0L

    init {
        WereWolf3.INSTANCE.registerEvent<PlayerInteractEvent> { event ->
            val player = event.player
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            // main handじゃない場合はreturn
            if(event.hand!=EquipmentSlot.HAND) { return@registerEvent }
            // 右クリックしていない場合はreturn
            if(event.action!=Action.RIGHT_CLICK_AIR&&event.action!=Action.RIGHT_CLICK_BLOCK) { return@registerEvent }
            val item = player.inventory.itemInMainHand
            // ピカピカインクを持っていない場合はreturn
            if(!isSimilar(item)) { return@registerEvent }
            item.amount--
            player.playSound(player, Sound.ENTITY_GLOW_SQUID_SQUIRT, 2f, 1f)
            glowing = GLOWING_TIME
        }
        WereWolf3.INSTANCE.runTaskTimer(0L,20L) {
            if(glowing > 0) {
                glowing -= 20
                WereWolf3.PLAYERS.forEach { player ->
                    player.playSound(player,Sound.ENTITY_BEE_STING,2F,0F)
                    if(player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
                        player.sendTitle(GLOW_TITLE_TEXT, messages("invisible"), 0, 5, 30)
                        player.removePotionEffect(PotionEffectType.INVISIBILITY)
                    } else {
                        player.sendTitle(GLOW_TITLE_TEXT, messages("glowing"), 0, 5, 30)
                        player.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 40, 0, false, false))
                    }
                }
            }
        }
    }
}