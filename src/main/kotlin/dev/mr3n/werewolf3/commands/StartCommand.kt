package dev.mr3n.werewolf3.commands

import dev.mr3n.werewolf3.GameTerminator
import dev.mr3n.werewolf3.WereWolf3
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

object StartCommand: CommandExecutor, TabCompleter {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if(sender !is Player) { return false }
        try {
            WereWolf3.start(sender.location.clone())
        } catch(e: Exception) {
            e.printStackTrace()
            sender.sendTitle("${ChatColor.RED}${ChatColor.BOLD}ERROR HAS OCCURRED", "Please resend command that /start", 0, 100, 20)
            GameTerminator.run()
        }
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