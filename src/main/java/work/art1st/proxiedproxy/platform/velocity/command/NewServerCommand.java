package work.art1st.proxiedproxy.platform.velocity.command;

import com.google.common.collect.ImmutableList;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.velocitypowered.proxy.command.builtin.CommandMessages;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.kyori.adventure.text.event.HoverEvent.showText;

/**
 * Reimplementation of ServerCommand, with custom name.
 */
public class NewServerCommand implements SimpleCommand {
    public static final int MAX_SERVERS_TO_LIST = 50;
    private final ProxyServer server;

    private final String command;

    public NewServerCommand(ProxyServer server, String command) {
        this.server = server;
        this.command = command;
    }

    static List<RegisteredServer> sortedServerList(ProxyServer proxy) {
        List<RegisteredServer> servers = new ArrayList<>(proxy.getAllServers());
        servers.sort(Comparator.comparing(RegisteredServer::getServerInfo));
        return Collections.unmodifiableList(servers);
    }

    @Override
    public void execute(final SimpleCommand.Invocation invocation) {
        final CommandSource source = invocation.source();
        final String[] args = invocation.arguments();

        if (!(source instanceof Player)) {
            source.sendMessage(CommandMessages.PLAYERS_ONLY);
            return;
        }

        Player player = (Player) source;
        if (args.length == 1) {
            // Trying to connect to a server.
            String serverName = args[0];
            Optional<RegisteredServer> toConnect = server.getServer(serverName);
            if (toConnect.isEmpty()) {
                player.sendMessage(CommandMessages.SERVER_DOES_NOT_EXIST.arguments(Component.text(serverName)));
                return;
            }

            player.createConnectionRequest(toConnect.get()).fireAndForget();
        } else {
            outputServerInformation(player);
        }
    }

    private void outputServerInformation(Player executor) {
        String currentServer = executor.getCurrentServer().map(ServerConnection::getServerInfo)
                .map(ServerInfo::getName).orElse("<unknown>");
        executor.sendMessage(Component.translatable(
                "velocity.command.server-current-server",
                NamedTextColor.YELLOW,
                Component.text(currentServer)));

        List<RegisteredServer> servers = sortedServerList(server);
        if (servers.size() > MAX_SERVERS_TO_LIST) {
            executor.sendMessage(Component.translatable(
                    "velocity.command.server-too-many", NamedTextColor.RED));
            return;
        }

        // Assemble the list of servers as components
        TextComponent.Builder serverListBuilder = Component.text()
                .append(Component.translatable("velocity.command.server-available",
                        NamedTextColor.YELLOW))
                .append(Component.space());
        for (int i = 0; i < servers.size(); i++) {
            RegisteredServer rs = servers.get(i);
            serverListBuilder.append(formatServerComponent(currentServer, rs));
            if (i != servers.size() - 1) {
                serverListBuilder.append(Component.text(", ", NamedTextColor.GRAY));
            }
        }

        executor.sendMessage(serverListBuilder.build());
    }

    private TextComponent formatServerComponent(String currentPlayerServer, RegisteredServer server) {
        ServerInfo serverInfo = server.getServerInfo();
        TextComponent serverTextComponent = Component.text(serverInfo.getName());

        int connectedPlayers = server.getPlayersConnected().size();
        TranslatableComponent playersTextComponent;
        if (connectedPlayers == 1) {
            playersTextComponent = Component.translatable(
                    "velocity.command.server-tooltip-player-online");
        } else {
            playersTextComponent = Component.translatable(
                    "velocity.command.server-tooltip-players-online");
        }
        playersTextComponent = playersTextComponent.arguments(Component.text(connectedPlayers));
        if (serverInfo.getName().equals(currentPlayerServer)) {
            serverTextComponent = serverTextComponent.color(NamedTextColor.GREEN)
                    .hoverEvent(
                            showText(
                                    Component.translatable("velocity.command.server-tooltip-current-server")
                                            .append(Component.newline())
                                            .append(playersTextComponent))
                    );
        } else {
            serverTextComponent = serverTextComponent.color(NamedTextColor.GRAY)
                    /* MODIFICATION IS HERE */
                    .clickEvent(ClickEvent.runCommand("/" + command + " " + serverInfo.getName()))
                    .hoverEvent(
                            showText(
                                    Component.translatable("velocity.command.server-tooltip-offer-connect-server")
                                            .append(Component.newline())
                                            .append(playersTextComponent))
                    );
        }
        return serverTextComponent;
    }

    @Override
    public List<String> suggest(final SimpleCommand.Invocation invocation) {
        final String[] currentArgs = invocation.arguments();
        Stream<String> possibilities = server.getAllServers().stream()
                .map(rs -> rs.getServerInfo().getName());

        if (currentArgs.length == 0) {
            return possibilities.collect(Collectors.toList());
        } else if (currentArgs.length == 1) {
            return possibilities
                    .filter(name -> name.regionMatches(true, 0, currentArgs[0], 0, currentArgs[0].length()))
                    .collect(Collectors.toList());
        } else {
            return ImmutableList.of();
        }
    }
}
