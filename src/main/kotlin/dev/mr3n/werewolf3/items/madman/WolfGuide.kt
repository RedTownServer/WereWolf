package dev.mr3n.werewolf3.items.madman

import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.protocol.GlowPacketUtil
import dev.mr3n.werewolf3.protocol.TeamPacketUtil
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.role
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot

object WolfGuide: IShopItem.ShopItem(Material.BOOK) {
    override val id: String = "wolf_guide"

    override val displayName: String = languages("item.${id}.name")

    override val price: Int = 300

    private val GUIDE_TITLE_TEXT = titleText("item.${id}.title.guide")

    private val SEARCH_TIME = constant<Long>("search_time")

    init {
        WereWolf3.INSTANCE.registerEvent<PlayerInteractEvent> { event ->
            val player = event.player
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            // main handじゃない場合はreturn
            if(event.hand!=EquipmentSlot.HAND) { return@registerEvent }
            // 右クリックしていない場合はreturn
            if(event.action!=Action.RIGHT_CLICK_AIR&&event.action!=Action.RIGHT_CLICK_BLOCK) { return@registerEvent }
            val item = player.inventory.itemInMainHand
            // 人狼ガイドを持っていない場合はreturn
            if(!isSimilar(item)) { return@registerEvent }
            player.sendTitle(GUIDE_TITLE_TEXT, messages("searching"), 10, SEARCH_TIME.toInt() + 10, 0)
            player.playSound(player, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f)
            item.amount--
            WereWolf3.INSTANCE.runTaskLater(SEARCH_TIME) {
                val wolf = WereWolf3.PLAYERS.filter { it.role == Role.WOLF }.randomOrNull()
                if(wolf==null) {
                    player.sendTitle(GUIDE_TITLE_TEXT, messages("wolf_not_found"), 0, 100, 0)
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                } else {
                    player.sendTitle(GUIDE_TITLE_TEXT, messages("searched", "%player%" to wolf.name), 0, 100, 0)
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f)
                    TeamPacketUtil.add(player, ChatColor.DARK_RED, listOf(wolf))
                    GlowPacketUtil.add(player, wolf)
                }
            }
        }
    }
}