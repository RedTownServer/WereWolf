package dev.mr3n.werewolf3.items.madman

import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.items.seer.SeerItem
import dev.mr3n.werewolf3.utils.languages

object FakeMediumItem: IShopItem.ShopItem("fake_medium", SeerItem.material) {
    override val displayName: String = languages("item.$id.name")
}