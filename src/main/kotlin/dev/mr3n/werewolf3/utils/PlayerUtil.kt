package dev.mr3n.werewolf3.utils

import dev.mr3n.werewolf3.Constants
import dev.mr3n.werewolf3.Keys
import dev.mr3n.werewolf3.events.WereWolf3DamageEvent
import dev.mr3n.werewolf3.roles.Role
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataType

fun Player.damageTo(target: Player, damage: Double) {
    if(damage == 0.0) { return }
    val event = WereWolf3DamageEvent(target, damage)
    Bukkit.getPluginManager().callEvent(event)
    if(event.isCancelled) { return }
    val health = target.health
    target.health = minOf(target.healthScale, health + 1)
    target.damage(1.0,this)
    if(event.damage < 0) {
        target.health = 0.0
    } else {
        target.health = maxOf(.0, health - event.damage)
    }
}

val Player.isBE: Boolean
    get() = this.name.startsWith(Constants.BE_PREFIX)

var Player.gameId: String?
    get() = this.persistentDataContainer.get(Keys.GAME_ID, PersistentDataType.STRING)
    set(value) {
        if(value==null) { this.persistentDataContainer.remove(Keys.GAME_ID) } else { this.persistentDataContainer.set(Keys.GAME_ID, PersistentDataType.STRING, value) }
    }

var Player.kills: IntArray?
    get() = this.persistentDataContainer.get(Keys.KILLS, PersistentDataType.INTEGER_ARRAY)
    set(value) {
        if(value==null) { this.persistentDataContainer.remove(Keys.KILLS) } else { this.persistentDataContainer.set(Keys.KILLS, PersistentDataType.INTEGER_ARRAY, value) }
    }

fun Player.addKill(target: Player) {
    val kills = this.kills?.toMutableSet()?:mutableSetOf()
    kills.add(target.entityId)
    this.kills = kills.toIntArray()
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

var Player.will: String?
    get() = this.persistentDataContainer.get(Keys.PLAYER_WILL, PersistentDataType.STRING)
    set(value) {
        if(value==null) { this.persistentDataContainer.remove(Keys.PLAYER_WILL) } else { this.persistentDataContainer.set(Keys.PLAYER_WILL, PersistentDataType.STRING, value) }
    }

var Player.money: Int
    get() = this.persistentDataContainer.get(Keys.MONEY, PersistentDataType.INTEGER)?:0
    set(value) {
        this.persistentDataContainer.set(Keys.MONEY, PersistentDataType.INTEGER, value)
    }