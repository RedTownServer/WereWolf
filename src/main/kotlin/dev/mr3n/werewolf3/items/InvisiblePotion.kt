package dev.mr3n.werewolf3.items

import com.comphenix.protocol.wrappers.EnumWrappers
import dev.moru3.minepie.Executor.Companion.runTaskLater
import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.protocol.InvisiblePacketUtil
import dev.mr3n.werewolf3.utils.languages
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.inventory.meta.PotionMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType

object InvisiblePotion: IShopItem.ShopItem("invisible_potion", Material.POTION) {
    private val TIME: Int = constant("time")

    override val description: String = languages("item.$id.description", "%time%" to TIME / 20)

    override fun onSetItemMeta(itemMeta: ItemMeta) {
        if(itemMeta !is PotionMeta) { return }
        itemMeta.color = Color.AQUA
        itemMeta.itemFlags.add(ItemFlag.HIDE_POTION_EFFECTS)
    }

    init {
        WereWolf3.INSTANCE.registerEvent<EntityPotionEffectEvent> { event ->
            val player = event.entity
            if(player !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            when(event.action) {
                EntityPotionEffectEvent.Action.ADDED -> {
                    if(event.newEffect?.type!=PotionEffectType.INVISIBILITY) { return@registerEvent }
                    WereWolf3.INSTANCE.runTaskLater(1L) {
                        WereWolf3.PLAYERS.forEach { sendTo ->
                            InvisiblePacketUtil.add(sendTo, player, 10, EnumWrappers.ItemSlot.HEAD, EnumWrappers.ItemSlot.CHEST, EnumWrappers.ItemSlot.LEGS, EnumWrappers.ItemSlot.FEET)
                        }
                    }
                }
                EntityPotionEffectEvent.Action.REMOVED -> {
                    if(event.oldEffect?.type!=PotionEffectType.INVISIBILITY) { return@registerEvent }
                    // イベント発生直後はエフェクトが残っている判定なので1tick後に帽子を復元するパケットを送信
                    WereWolf3.INSTANCE.runTaskLater(1L) {
                        WereWolf3.PLAYERS.forEach { sendTo -> InvisiblePacketUtil.remove(sendTo, player, 10) }
                    }
                }
                EntityPotionEffectEvent.Action.CLEARED -> {
                    // イベント発生直後はエフェクトが残っている判定なので1tick後に帽子を復元するパケットを送信
                    WereWolf3.INSTANCE.runTaskLater(1L) {
                        WereWolf3.PLAYERS.forEach { sendTo -> InvisiblePacketUtil.remove(sendTo, player, 10) }
                    }
                }
                else -> {}
            }
        }
        WereWolf3.INSTANCE.registerEvent<PlayerItemConsumeEvent> { event ->
            val player = event.player
            val item = event.item
            if(!isSimilar(item)) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(player)) { return@registerEvent }
            player.addPotionEffect(PotionEffect(PotionEffectType.INVISIBILITY, TIME, 200, false, false, true))
            player.removePotionEffect(PotionEffectType.GLOWING)
            player.inventory.itemInMainHand.amount--
        }
    }
}