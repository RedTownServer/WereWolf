package dev.mr3n.werewolf3.commands

import dev.mr3n.werewolf3.GameTerminator
import dev.mr3n.werewolf3.roles.Role
import dev.mr3n.werewolf3.utils.languages
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

object EndCommand: CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) { return false }
        GameTerminator.end(Role.Team.VILLAGER, languages("force_end"))
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): MutableList<String>? {
        return null
    }

}