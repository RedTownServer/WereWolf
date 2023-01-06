package dev.mr3n.werewolf3.roles

import dev.mr3n.werewolf3.WereWolf3
import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.utils.constants
import dev.mr3n.werewolf3.utils.container
import dev.mr3n.werewolf3.utils.languages
import net.md_5.bungee.api.ChatColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataType
import java.util.*
import kotlin.math.ceil

enum class Role() {
    VILLAGER,
    DOCTOR,
    WOLF,
    MADMAN,
    SEER,
    MEDIUM
    ;

    val config = WereWolf3.CONFIG.config()?:throw NullPointerException()

    // 役職名を小文字で
    fun lowercase() = this.toString().lowercase()

    // 役職の色
    val color = ChatColor.of(config.getString("roles.${this.lowercase()}.color")!!)
    // 役職の陣営 (白陣営/黒陣営)
    val team = Team.valueOf(config.getString("roles.${this.lowercase()}.team")!!)
    // 表示名
    val displayName = config.getString("roles.${this.lowercase()}.name")!!
    // 役職の説明
    val description = config.getString("roles.${this.lowercase()}.description")!!


    // 役職の優先度 高->0,低->∞
    val priority = config.getInt("roles.${this.lowercase()}.priority")
    // 役職の最少人数。minPが優先されます。
    private val min = config.getInt("roles.${this.lowercase()}.min")
    // 役職の人数
    val num = config.getInt("roles.${this.lowercase()}.num")
    // 人数の単位 人/パーセント
    private val unit = Unit.valueOf(config.getString("roles.${this.lowercase()}.unit")!!)
    // 役職を有効にする際の最小参加人数
    private val minP = config.getInt("roles.${this.lowercase()}.min_players")
    val items: List<IShopItem>
        get() = constants<String>("roles.${this.lowercase()}.items").mapNotNull { IShopItem.ShopItem.ITEMS_BY_ID[it] }


    // 役職の人数を参加者数から計算
    fun calc(players: Int): Int {
        // 最小参加人数に達していない場合は0
        if(players <= minP) { return 0 }
        // autoかどうか
        return if(num==-1) {
            //if:autoだった場合
            // autoじゃない役職一覧を取得
            val filters = values().filterNot { it.num == -1 }
            // autoの役職の数
            val count = values().size - filters.size
            // autoの合計役職人数を足す
            val sum = filters.sumOf { it.calc(players) }
            // 足した値をautoの役職の数で割って最小役職人数を適用
            (players - sum)/count
        } else {
            // パーセント/人で人数を決定。
            val base = when(unit) {
                Unit.PERCENT -> { (players / 100.0) * num }
                Unit.PLAYER -> { num.toDouble() }
            }
            // 最小役職人数を適用。
            maxOf(min, ceil(base).toInt())
        }
    }

    // toStringをoverrideするとvalueOfを使えないため。
    fun asString() = "${color}${displayName}"

    // 役職のヘルメットを取得。
    val helmet: ItemStack
        get() = ItemStack(Material.LEATHER_HELMET).also { item ->
            item.itemMeta = (item.itemMeta as LeatherArmorMeta?)?.also { meta ->
                val color = this.color.color
                meta.container.set(HELMET_ROLE_TAG_KEY,RoleTagType,this)
                meta.setColor(Color.fromRGB(color.red,color.green,color.blue))
                meta.isUnbreakable = true
                meta.setDisplayName(languages("item.co_helmet.name", "%color%" to this.color, "%role%" to this.displayName))
            }
        }

    companion object {
        val HELMET_ROLE_TAG_KEY = NamespacedKey(WereWolf3.INSTANCE,"role")
        val ROLES = mutableMapOf<Role, List<UUID>>()
    }

    // プレイヤー数の単位。
    enum class Unit {
        PLAYER,
        PERCENT
    }

    // NBTタグの単位。
    object RoleTagType: PersistentDataType<String, Role> {
        override fun getPrimitiveType(): Class<String> = String::class.java
        override fun getComplexType(): Class<Role> = Role::class.java
        override fun toPrimitive(complex: Role, context: PersistentDataAdapterContext): String = complex.toString()
        override fun fromPrimitive(primitive: String, context: PersistentDataAdapterContext): Role = Role.valueOf(primitive)
    }

    // 陣営一覧。白陣営、黒陣営。
    enum class Team(val displayName: String, val color: ChatColor) {
        WOLF("人狼", ChatColor.DARK_RED),
        VILLAGER("村人", ChatColor.GOLD)
    }
}