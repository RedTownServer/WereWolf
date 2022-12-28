package dev.mr3n.werewolf3.utils

import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataType

val ItemMeta.container: PersistentDataContainer
    get() = this.persistentDataContainer


inline fun <reified T : Any, Z> ItemStack.getContainerValue(key: NamespacedKey, pdt: PersistentDataType<Z,T>): T? {
    return this.itemMeta?.container?.get(key, pdt)
}

inline fun <reified T : Any, Z> ItemStack.setContainerValue(key: NamespacedKey, pdt: PersistentDataType<Z,T>, value: T) {
    this.itemMeta = this.itemMeta?.also { meta -> meta.container.set(key,pdt,value) }
}