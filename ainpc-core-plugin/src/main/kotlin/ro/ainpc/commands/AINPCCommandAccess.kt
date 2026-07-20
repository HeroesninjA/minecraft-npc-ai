package ro.ainpc.commands

import org.bukkit.command.CommandSender

internal fun hasCreatorAccess(sender: CommandSender): Boolean =
    sender.hasPermission("ainpc.admin") || sender.hasPermission("ainpc.creator")
