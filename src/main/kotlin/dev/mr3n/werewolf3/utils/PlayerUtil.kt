package dev.mr3n.werewolf3.utils

import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.roles.Role
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.persistence.PersistentDataType

fun Player.damageTo(target: Player, damage: Double) {
    target.health = maxOf(.0, target.health - damage)
}

var Player.gameId: String?
    get() = this.persistentDataContainer.get(Keys.GAME_ID, PersistentDataType.STRING)
    set(value) {
        if(value==null) { this.persistentDataContainer.remove(Keys.GAME_ID) } else { this.persistentDataContainer.set(Keys.GAME_ID, PersistentDataType.STRING, value) }
    }

var Player.role: Role?
    get() = this.persistentDataContainer.get(Keys.ROLE, Role.RoleTagType)
    set(value) {
        if(value==null) {
            val role = this.role
            if(role!=null) {
                val list = Role.ROLES[role]?.toMutableList()?: mutableListOf()
                list.remove(this.uniqueId)
                Role.ROLES[role] = list
            }
            this.persistentDataContainer.remove(Keys.ROLE)
        } else {
            val role = this.role
            if(role!=null) {
                val old = Role.ROLES[role]?.toMutableList()?: mutableListOf()
                old.remove(this.uniqueId)
                Role.ROLES[role] = old
            }
            val new = Role.ROLES[value]?.toMutableList()?: mutableListOf()
            new.add(this.uniqueId)
            Role.ROLES[value] = new
            this.persistentDataContainer.set(Keys.ROLE, Role.RoleTagType, value)
        }
    }

var Player.co: Role?
    get() = this.persistentDataContainer.get(Keys.CO, Role.RoleTagType)
    set(value) {
        if(value==null) { this.persistentDataContainer.remove(Keys.CO) } else { this.persistentDataContainer.set(Keys.CO, Role.RoleTagType, value) }
    }

var Player.money: Int
    get() = this.persistentDataContainer.get(Keys.MONEY, PersistentDataType.INTEGER)?:0
    set(value) {
        this.persistentDataContainer.set(Keys.MONEY, PersistentDataType.INTEGER, value)
    }