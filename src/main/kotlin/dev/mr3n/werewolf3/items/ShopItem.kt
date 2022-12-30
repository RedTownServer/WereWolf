package dev.mr3n.werewolf3.items

import dev.moru3.minepie.item.EasyItem
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.getContainerValue
import dev.mr3n.werewolf3.utils.languages
import dev.mr3n.werewolf3.utils.setContainerValue
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
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

    abstract class ShopItem(val material: Material): IShopItem {

        override val roles: List<Role> = constants<String>("roles").map{Role.valueOf(it)}
        override val displayName: String
            get() = languages("item.${id}.name")
        override val description: String
            get() = languages("item.${id}.description")
        override val itemStack: ItemStack
            get() = EasyItem(material, displayName, description.split("\n")).also { item ->
                item.setContainerValue(Keys.ITEM_ID, PersistentDataType.STRING, id)
            }

        fun messages(key: String, vararg values: Pair<String, Any>): String {
            return languages("item.${id}.messages.${key}", *values)
        }

        protected inline fun <reified T> constant(key: String): T {
            return dev.mr3n.werewolf3.utils.constant("items.${id}.${key}")
        }

        protected inline fun <reified T> constants(key: String): List<T> {
            return dev.mr3n.werewolf3.utils.constants("items.${id}.${key}")
        }

        override fun isSimilar(itemStack: ItemStack): Boolean {
            return itemStack.getContainerValue(Keys.ITEM_ID, PersistentDataType.STRING) == id
        }
    }
}