package work.art1st.proxiedproxy.command;

import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import work.art1st.proxiedproxy.ProxiedProxy;

/**
 * Command /reload
 */
public class ReloadCommand implements SimpleCommand {

    private final ProxiedProxy plugin;

    public ReloadCommand(ProxiedProxy plugin) {
        this.plugin = plugin;
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1 && args[0].equals("reload")) {
            boolean success = plugin.initialize();
            if (invocation.source() instanceof Player) {
                if (success) {
                    invocation.source().sendMessage(Component.text("Proxied Proxy configurations reloaded.", NamedTextColor.GRAY));
                } else {
                    invocation.source().sendMessage(Component.text("Invalid configuration file!", NamedTextColor.RED));
                }
            }
        } else {
            invocation.source().sendMessage(Component.text("Unknown command!", NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission("proxiedproxy.command.reload");
    }
}
