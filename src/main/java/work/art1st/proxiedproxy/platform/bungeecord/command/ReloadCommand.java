package work.art1st.proxiedproxy.platform.bungeecord.command;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import work.art1st.proxiedproxy.PPlugin;

public class ReloadCommand extends Command {
    public ReloadCommand() {
        super(PPlugin.PERMISSION_STRING_RELOAD, PPlugin.PERMISSION_STRING_RELOAD, PPlugin.COMMAND_ALIASES);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TextComponent component;
        if (args.length == 1 && args[0].equals("reload")) {
            boolean success = PPlugin.initialize();
            if (success) {
                component = new TextComponent("Proxied Proxy configurations reloaded.");
                component.setColor(ChatColor.GREEN);
            } else {
                component = new TextComponent("Invalid configuration file!");
                component.setColor(ChatColor.RED);
            }
        } else {
            component = new TextComponent("Unknown command!");
            component.setColor(ChatColor.RED);
        }
        sender.sendMessage(component);
    }
}
