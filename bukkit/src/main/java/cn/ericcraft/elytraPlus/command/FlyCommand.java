package cn.ericcraft.elytraPlus.command;

import cn.ericcraft.elytraPlus.config.Messages;
import cn.ericcraft.elytraPlus.manager.FlightManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class FlyCommand implements CommandExecutor {
    private final FlightManager flights;
    private final Messages messages;

    public FlyCommand(FlightManager flights, Messages messages) {
        this.flights = flights;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) messages.send(sender, "only-player");
        else if (args.length != 0) messages.send(sender, "usage");
        else flights.toggle((Player) sender);
        return true;
    }
}
