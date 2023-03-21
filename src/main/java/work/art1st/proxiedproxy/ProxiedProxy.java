package work.art1st.proxiedproxy;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.config.PlayerInfoForwarding;
import com.velocitypowered.proxy.config.VelocityConfiguration;
import com.velocitypowered.proxy.connection.client.LoginInboundConnection;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;
import work.art1st.proxiedproxy.command.NewServerCommand;
import work.art1st.proxiedproxy.command.ReloadCommand;
import work.art1st.proxiedproxy.config.EntryConfig;
import work.art1st.proxiedproxy.config.ProxyConfig;
import work.art1st.proxiedproxy.config.TrustedEntry;
import work.art1st.proxiedproxy.connection.ServerListPingHandler;
import work.art1st.proxiedproxy.forwarding.ForwardingParser;
import work.art1st.proxiedproxy.forwarding.ForwardingPluginChannel;
import work.art1st.proxiedproxy.forwarding.VerificationType;
import work.art1st.proxiedproxy.util.RSAUtil;
import work.art1st.proxiedproxy.util.ReflectUtil;
import work.art1st.proxiedproxy.util.VelocityInjector;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Plugin(
        id = "proxied-proxy",
        name = "Proxied Proxy",
        version = BuildConstants.VERSION,
        authors = {"__ART1st__"},
        url = ProxiedProxy.URL
)
public class ProxiedProxy {

    public static final String URL = "https://mc.sjtu.cn/wiki";
    @Getter
    private final ProxyServer proxy;
    @Getter
    private final Logger logger;
    @Getter
    private final Path dataDirectory;
    private final ForwardingPluginChannel channel = new ForwardingPluginChannel();
    @Getter
    private final EntryConfig entryConfig = new EntryConfig();
    @Getter
    private final ProxyConfig proxyConfig = new ProxyConfig();
    private boolean verbose;
    @Getter
    private Role role;

    @Inject
    public ProxiedProxy(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws IOException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        copyResources();
        proxy.getCommandManager().register("proxiedproxy", new ReloadCommand(this), "prox");
        new VelocityInjector().inject();
        entryConfig.serverCommandAlias = "server";
        initialize();
    }

    public boolean initialize() {
        boolean success = false;
        try {
            success = _initialize();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (!success) {
                role = Role.DISABLED;
                logger.warn("Plugin now disabled.");
            } else {
                logger.info("Configurations loaded.");
            }
        }
        return success;
    }

    private boolean _initialize() throws IOException {
        FileConfig configFile = FileConfig.of(dataDirectory.resolve("config.toml").toFile());
        configFile.load();

        role = Role.valueOf(configFile.getOrElse("role", "PROXY").toUpperCase());
        verbose = configFile.getOrElse("verbose", false);
        switch (role) {
            case ENTRY:
                /* Check forwarding mode */
                if (proxy.getConfiguration() instanceof VelocityConfiguration) {
                    if (!((VelocityConfiguration) proxy.getConfiguration()).getPlayerInfoForwardingMode().equals(PlayerInfoForwarding.LEGACY) &&
                            !((VelocityConfiguration) proxy.getConfiguration()).getPlayerInfoForwardingMode().equals(PlayerInfoForwarding.BUNGEEGUARD)) {
                        logger.error("You have to set forwarding mode to legacy or bungeeguard.");
                        return false;
                    }
                } else {
                    throw new RuntimeException();
                }
                /* Replace /server with your specific command, /hub as default. */
                CommandManager commandManager = proxy.getCommandManager();
                String newServerCommandAlias = configFile.getOrElse("entry.server-command-alias", "hub");
                debugOutput("Replacing /" + entryConfig.serverCommandAlias + " with /" + newServerCommandAlias);
                if (!newServerCommandAlias.equals(entryConfig.serverCommandAlias)) {
                    commandManager.unregister(entryConfig.serverCommandAlias);
                    entryConfig.serverCommandAlias = newServerCommandAlias;
                    commandManager.register(entryConfig.serverCommandAlias, new NewServerCommand(proxy, newServerCommandAlias));
                }
                /* Set verification type */
                entryConfig.verificationType = VerificationType.valueOf(configFile.getOrElse("entry.verification-type", "RSA").toUpperCase());
                entryConfig.entryId = configFile.get("entry.entry-id");
                if (entryConfig.entryId == null) {
                    logger.error("Invalid entry-id.");
                    return false;
                }
                TrustedEntry self = null;
                switch (entryConfig.verificationType) {
                    case RSA:
                        File publicKeyFile = dataDirectory.resolve("PublicKey.pem").toFile();
                        File privateKeyFile = dataDirectory.resolve("PrivateKey.pem").toFile();
                        KeyPair keyPair;
                        if (publicKeyFile.exists() && privateKeyFile.exists()) {
                            try {
                                keyPair = RSAUtil.readKeyPair(publicKeyFile, privateKeyFile);
                            } catch (NoSuchAlgorithmException e) {
                                throw new RuntimeException(e);
                            } catch (InvalidKeySpecException e) {
                                logger.error("Invalid RSA keypair.");
                                return false;
                            }
                        } else {
                            keyPair = RSAUtil.genKeyPair();
                            RSAUtil.saveKeyPair(keyPair, publicKeyFile, privateKeyFile);
                        }
                        entryConfig.publicKey = keyPair.getPublic();
                        entryConfig.privateKey = keyPair.getPrivate();
                        String publicKeyString = new String(Files.readAllBytes(publicKeyFile.toPath()));
                        self = new TrustedEntry(entryConfig.entryId, publicKeyString, VerificationType.RSA);
                        break;
                    case KEY:
                        entryConfig.key = configFile.get("entry.key.key");
                        if (entryConfig.key == null) {
                            logger.error("Invalid entry verification key.");
                            return false;
                        }
                        self = new TrustedEntry(entryConfig.entryId, entryConfig.key, VerificationType.KEY);
                        break;
                }
                FileWriter fileWriter = new FileWriter(dataDirectory.resolve("entry.json").toFile(), false);
                fileWriter.write(new Gson().toJson(self));
                fileWriter.flush();
                fileWriter.close();
                /* Ping forward settings */
                if (configFile.getOrElse("entry.pass-through-ping-vhost", true)) {
                    try {
                        Field serverListPingHandlerField = ReflectUtil.handleAccessible(proxy.getClass().getDeclaredField("serverListPingHandler"));
                        if (entryConfig.velocityServerListPingHandler == null) {
                            entryConfig.velocityServerListPingHandler = (com.velocitypowered.proxy.connection.util.ServerListPingHandler) serverListPingHandlerField.get(proxy);
                        }
                        serverListPingHandlerField.set(proxy, new ServerListPingHandler((VelocityServer) proxy));
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        logger.warn("Failed to replace serverListPingHandler.");
                    }
                } else {
                    if (entryConfig.velocityServerListPingHandler != null) {
                        try {
                            Field serverListPingHandlerField = ReflectUtil.handleAccessible(proxy.getClass().getDeclaredField("serverListPingHandler"));
                            serverListPingHandlerField.set(proxy, entryConfig.velocityServerListPingHandler);
                        } catch (NoSuchFieldException | IllegalAccessException e) {
                            /* Should be impossible */
                            throw new RuntimeException(e);
                        }
                    }
                }
                break;
            case PROXY:
                proxyConfig.allowClientConnection = configFile.getOrElse("proxy.allow-client-connection", true);
                try {
                    File trustedEntriesListFile = dataDirectory.resolve("TrustedEntries.json").toFile();
                    JsonReader reader = new JsonReader(new FileReader(trustedEntriesListFile));
                    List<TrustedEntry> trustedEntriesList = new Gson().fromJson(reader, new TypeToken<List<TrustedEntry>>() {
                    }.getType());
                    if (trustedEntriesList == null) {
                        trustedEntriesList = new ArrayList<>();
                    }
                    proxyConfig.trustedEntries.clear();
                    for (TrustedEntry entry :
                            trustedEntriesList) {
                        proxyConfig.trustedEntries.put(entry.getId(), entry);
                    }
                } catch (FileNotFoundException e) {
                    logger.warn("No TrustedEntries.json found.");
                    logger.warn("Please consult " + URL);
                    copyResourceFile("TrustedEntries.json");
                }
                if (proxyConfig.trustedEntries.size() == 0) {
                    logger.warn("There are no entries trusted.");
                }
                break;
        }

        configFile.close();
        return true;
    }

    /* Triggered on PROXY server. */
    @Subscribe
    public void onPreLoginEvent(PreLoginEvent event) throws NoSuchFieldException, IllegalAccessException {
        if (role.equals(Role.PROXY)) {
            debugOutput("PreLogin");
            if (event.getConnection() instanceof LoginInboundConnection) {
                /* getVirtualHost() extracts vhost from the address field of a handshake packet.
                 * Therefore, by comparing result of getVirtualHost() and original address field,
                 * we can determine whether the inbound connection is from an upstream entry. */
                String cleanedAddress = event.getConnection().getVirtualHost().map(InetSocketAddress::getHostString).map(str -> str.toLowerCase(Locale.ROOT)).orElse("");
                LoginInboundConnection loginInboundConnection = (LoginInboundConnection) event.getConnection();
                /* origAddress is the original address field from handshake packet.
                 * Velocity removes the '.' at the end if using SRV record. */
                String origAddress = ForwardingParser.getServerAddressFromConnection(loginInboundConnection).toLowerCase(Locale.ROOT);
                if (!origAddress.isEmpty() && origAddress.charAt(origAddress.length() - 1) == '.') {
                    origAddress = origAddress.substring(0, origAddress.length() - 1);
                }
                debugOutput("origAddress:");
                debugOutput(origAddress);
                if (!cleanedAddress.equals(origAddress) && !origAddress.endsWith("\0fml\0") && !origAddress.endsWith("\0fml2\0")) {
                    /* Incoming connection is from an upstream entry.
                     * We send a LoginPluginMessage requesting for player info forwarding. */
                    loginInboundConnection.sendLoginPluginMessage(channel, ForwardingPluginChannel.FORWARDING_REQUEST.getBytes(StandardCharsets.UTF_8), bytes -> {
                        /* Async callback function that stores forwarded player info into cache
                         * Seems that the callback is always invoked before the next event(GameProfileRequestEvent). (Is it guaranteed?) */
                        if (bytes == null) {
                            loginInboundConnection.disconnect(Component.text("You are connecting from an untrusted entry."));
                            return;
                        }
                        String response = new String(bytes, StandardCharsets.UTF_8);
                        debugOutput("Received LoginPluginMessage response:");
                        debugOutput(response);
                        ForwardingParser parser = new Gson().fromJson(response, ForwardingParser.class);
                        if (!parser.isTrusted(proxyConfig.trustedEntries)) {
                            loginInboundConnection.disconnect(Component.text("You are connecting from an untrusted entry."));
                            return;
                        }
                        proxyConfig.profileCache.put(parser.getProfile().getName(), parser);
                    });
                    /* Set to offline mode to disable encryption,
                     * since velocity does not support encrypted connection to downstream server. */
                    event.setResult(PreLoginEvent.PreLoginComponentResult.forceOfflineMode());
                } else {
                    if (!proxyConfig.allowClientConnection) {
                        loginInboundConnection.disconnect(Component.text("Connection refused: Please connect to an entry server."));
                    }
                }
            } else {
                throw new RuntimeException();
            }
        }
    }

    /* Triggered on PROXY server. */
    @Subscribe
    public void onGameProfileRequest(GameProfileRequestEvent event) {
        if (role.equals(Role.PROXY)) {
            debugOutput("GameProfileRequest");
            ForwardingParser forwarded = proxyConfig.profileCache.getIfPresent(event.getUsername());
            if (forwarded != null) {
                event.setGameProfile(forwarded.getProfile());
                proxyConfig.profileCache.invalidate(event.getUsername());
                try {
                    forwarded.setConnectionRemoteAddress((LoginInboundConnection) event.getConnection());
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /* Triggered on ENTRY server. */
    @Subscribe
    public void onServerLoginPluginMessage(ServerLoginPluginMessageEvent event) {
        if (role.equals(Role.ENTRY)) {
            debugOutput("ServerLoginPluginMessage");
            if (event.getIdentifier().getId().equals(channel.getId())) {
                String content = new String(event.getContents());
                /* Only respond to specific incoming request.
                 * Guarantees compatibility with Bungeecord style forwarding. */
                if (content.equals(ForwardingPluginChannel.FORWARDING_REQUEST)) {
                    try {
                        ForwardingParser parser = null;
                        switch (entryConfig.verificationType) {
                            case RSA:
                                parser = new ForwardingParser(event.getConnection(), entryConfig.entryId, entryConfig.privateKey);
                                break;
                            case KEY:
                                parser = new ForwardingParser(event.getConnection(), entryConfig.entryId, entryConfig.key);
                        }
                        String response = new Gson().toJson(parser);
                        event.setResult(ServerLoginPluginMessageEvent.ResponseResult.reply(response.getBytes(StandardCharsets.UTF_8)));
                    } catch (InvalidKeyException e) {
                        ((LoginInboundConnection) event.getConnection()).disconnect(Component.text("Entry internal error: InvalidKeyException"));
                    }
                }
            }
        }
    }

    public void debugOutput(String s) {
        if (verbose) {
            logger.info(s);
        }
    }

    private void copyResources() throws IOException {
        copyResourceFile("config.toml");
    }

    private void copyResourceFile(String filename) throws IOException {
        File file = dataDirectory.resolve(filename).toFile();
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            file.createNewFile();
            InputStream is = this.getClass().getResourceAsStream("/" + filename);
            FileOutputStream fos = new FileOutputStream(dataDirectory.resolve(filename).toString());
            byte[] b = new byte[1024];
            int length;
            while ((length = is.read(b)) > 0) {
                fos.write(b, 0, length);
            }
            is.close();
            fos.close();
        }
    }

    public enum Role {
        ENTRY,
        PROXY,
        DISABLED
    }
}
