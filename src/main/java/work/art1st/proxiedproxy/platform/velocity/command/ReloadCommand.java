package work.art1st.proxiedproxy.platform.velocity.command;

import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import work.art1st.proxiedproxy.PPlugin;

/**
 * Command /reload
 */
public class ReloadCommand implements SimpleCommand {


    public ReloadCommand() {
    }

    @Override
    public void execute(Invocation invocation) {
        String[] args = invocation.arguments();
        if (args.length == 1 && args[0].equals("reload")) {
            boolean success = PPlugin.initialize();
            if (success) {
                invocation.source().sendMessage(Component.text("Proxied Proxy configurations reloaded.", NamedTextColor.GREEN));
            } else {
                invocation.source().sendMessage(Component.text("Invalid configuration file!", NamedTextColor.RED));
            }
        } else {
            invocation.source().sendMessage(Component.text("Unknown command!", NamedTextColor.RED));
        }
    }

    @Override
    public boolean hasPermission(final SimpleCommand.Invocation invocation) {
        return invocation.source().hasPermission(PPlugin.PERMISSION_STRING_RELOAD);
    }
}
