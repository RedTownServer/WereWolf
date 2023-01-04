package dev.mr3n.werewolf3.items

import dev.moru3.minepie.item.EasyItem
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.items.doctor.DoctorSword
import dev.mr3n.werewolf3.items.doctor.HealthCharger
import dev.mr3n.werewolf3.items.madman.FakeMediumItem
import dev.mr3n.werewolf3.items.madman.FakeSeerItem
import dev.mr3n.werewolf3.items.madman.WolfGuide
import dev.mr3n.werewolf3.items.medium.MediumItem
import dev.mr3n.werewolf3.items.seer.SeerItem
import dev.mr3n.werewolf3.items.wolf.*
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.*
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataType

/**
 * 人狼で使用するアイテムの情報
 */
interface IShopItem {
    /**
     * アイテムを識別するためのユニークなID
     */
    val id: String

    /**
     * ひとこと
     */
    val comment: String

    /**
     * アイテムの表示名
     */
    val displayName: String

    /**
     * アイテムの説明
     */
    val description: String

    /**
     * アイテム
     */
    val itemStack: ItemStack

    /**
     * このアイテムを所持できる役職
     */
    val roles: List<Role>

    /**
     * アイテムの価格
     */
    val price: Int

    /**
     * アイテムがこのアイテムかどうかを確認
     */
    fun isSimilar(itemStack: ItemStack): Boolean

    fun onEnd()

    fun onSetItemMeta(itemMeta: ItemMeta)

    fun buy(player: Player): Boolean

    abstract class ShopItem(final override val id: String, val material: Material): IShopItem {

        override val price: Int = constant("price")
        override val roles: List<Role> = constants<String>("roles").map{Role.valueOf(it)}
        override val displayName: String = languages("item.${id}.name")
        override val comment: String = languages("item.${id}.comment")
        override val description: String = languages("item.${id}.description").let { "${it}\n\n${languages("item.comment", "%comment%" to comment)}" }
        override val itemStack: ItemStack
            get() = EasyItem(material, displayName, description.split("\n")).also { item ->
                item.itemMeta = item.itemMeta?.also { meta ->
                    meta.container.set(Keys.ITEM_ID, PersistentDataType.STRING, id)
                    ItemFlag.values().forEach { meta.addItemFlags(it) }
                    this.onSetItemMeta(meta)
                }
            }

        fun messages(key: String, vararg values: Pair<String, Any>): String {
            return languages("item.${id}.messages.${key}", *values)
        }

        protected inline fun <reified T> constant(key: String): T {
            return dev.mr3n.werewolf3.utils.constant("items.${id}.${key}")
        }

        protected inline fun <reified T> constants(key: String): List<T> {
            return dev.mr3n.werewolf3.utils.constants<T>("items.${id}.${key}")
        }

        override fun isSimilar(itemStack: ItemStack): Boolean {
            return itemStack.getContainerValue(Keys.ITEM_ID, PersistentDataType.STRING) == id
        }

        override fun onEnd() {}

        override fun buy(player: Player): Boolean {
            return if(player.money >= price) {
                player.money -= price
                player.inventory.addItem(itemStack)
                player.sendMessage(languages("shop.bought", "%item%" to displayName, "%price%" to price).asPrefixed())
                true
            } else {
                player.sendMessage(languages("shop.cant_buy", "%item%" to displayName, "%price%" to price).asPrefixed())
                false
            }
        }

        override fun onSetItemMeta(itemMeta: ItemMeta) {}

        init {
            ITEMS.add(this)
            ITEMS_BY_ID[id] = this
        }

        companion object {
            val ITEMS = mutableListOf<IShopItem>()
            val ITEMS_BY_ID = mutableMapOf<String, IShopItem>()
            val STAN_BALL = StanBall
            val INVISIBLE_POTION = InvisiblePotion
            val HEAL_POTION = HealPotion
            val GLOW_INK = GlowInk
            val ASSASSIN_SWORD = AssassinSword
            val BOMB_BALL = BombBall
            val LIGHTNING_ROD = LightningRod
            val WOLF_AXE = WolfAxe
            val SEER_ITEM = SeerItem
            val MEDIUM_ITEM = MediumItem
            val WOLF_GUIDE = WolfGuide
            val FAKE_SEER_ITEM = FakeSeerItem
            val FAKE_MEDIUM_ITEM = FakeMediumItem
            val HEALTH_CHARGER = HealthCharger
            val DOCTOR_SWORD = DoctorSword
            val ONE_SHOT_BOW = OneShotBow
            val TOTEM = Totem
            val SPEED_POTION = SpeedPotion
            val STONE_SWORD = StoneSword
            val LAST_RESORT = LastResort
            val DEAD_BODY_REMOVER = DeadBodyRemover
        }
    }
}