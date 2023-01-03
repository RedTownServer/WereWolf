package dev.mr3n.werewolf3.items

import dev.moru3.minepie.events.EventRegister.Companion.registerEvent
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.utils.damageTo
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.titleText
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.CrossbowMeta
import org.bukkit.inventory.meta.Damageable
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

object OneShotBow: IShopItem.ShopItem("one_shot_crossbow", Material.CROSSBOW) {
    override val displayName: String = languages("item.${id}.name")

    override fun onSetItemMeta(itemMeta: ItemMeta) {
        if(itemMeta !is Damageable) { return }
        itemMeta.isUnbreakable = true
        if(itemMeta !is CrossbowMeta) { return }
        itemMeta.damage = material.maxDurability.toInt()
        itemMeta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE)
        itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS)
        itemMeta.addEnchant(Enchantment.ARROW_INFINITE,1,true)
        itemMeta.setChargedProjectiles(listOf(ItemStack(Material.ARROW)))
    }

    private const val ENTITY_TYPE = "ONE_SHOT_ARROW"

    private val BOW_TITLE_TEXT = titleText("item.${id}.title.bow")

    init {
        WereWolf3.INSTANCE.registerEvent<ProjectileLaunchEvent> { event ->
            val projectile = event.entity
            val shooter = projectile.shooter?:return@registerEvent
            if(shooter !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(shooter)) { return@registerEvent }
            if(!isSimilar(shooter.inventory.itemInMainHand)) { return@registerEvent }
            // 爆発玉識別用のタグを付与
            shooter.inventory.itemInMainHand.amount--
            projectile.persistentDataContainer.set(Keys.ENTITY_TYPE, PersistentDataType.STRING, ENTITY_TYPE)
            shooter.sendTitle(BOW_TITLE_TEXT, messages("used"), 0, 60, 20)

        }
        WereWolf3.INSTANCE.registerEvent<ProjectileHitEvent> { event ->
            val projectile = event.entity
            // あたった一撃の弓じゃない場合はreturn
            if(projectile.persistentDataContainer.get(Keys.ENTITY_TYPE, PersistentDataType.STRING) != ENTITY_TYPE) { return@registerEvent }
            val shooter = projectile.shooter?:return@registerEvent
            if(shooter !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(shooter)) { return@registerEvent }
            val hitEntity = event.hitEntity?:return@registerEvent
            if(hitEntity !is Player) { return@registerEvent }
            if(!WereWolf3.PLAYERS.contains(hitEntity)) { return@registerEvent }
            event.isCancelled = true
            projectile.remove()
            shooter.world.playSound(shooter, Sound.ENTITY_ITEM_BREAK, 1f, 1f)
            shooter.damageTo(hitEntity, -1.0)
        }
    }
}