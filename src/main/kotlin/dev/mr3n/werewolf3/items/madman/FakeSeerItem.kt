package dev.mr3n.werewolf3.items.madman

import dev.mr3n.werewolf3.items.IShopItem
import dev.mr3n.werewolf3.items.seer.SeerItem
import dev.mr3n.werewolf3.utils.languages

object FakeSeerItem: IShopItem.ShopItem(SeerItem.material) {
    override val id: String = "fake_seer"

    override val displayName: String = languages("item.$id.name")

    override val price: Int = 300
}