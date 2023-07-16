package work.art1st.proxiedproxy;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lombok.Getter;
import org.slf4j.Logger;
import work.art1st.proxiedproxy.config.EntryConfig;
import work.art1st.proxiedproxy.config.ProxyConfig;
import work.art1st.proxiedproxy.config.TrustedEntry;
import work.art1st.proxiedproxy.config.VerificationType;
import work.art1st.proxiedproxy.platform.common.PlatformPluginInstance;
import work.art1st.proxiedproxy.platform.velocity.VelocityMain;
import work.art1st.proxiedproxy.union.SkinServiceBackendVerifier;
import work.art1st.proxiedproxy.util.RSAUtil;

import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

public class PPlugin {
    public static final String WIKI_URL = "https://wiki.mualliance.ltd/%E8%81%94%E5%90%88%E5%A4%A7%E5%8E%85";
    public static final String PERMISSION_STRING_RELOAD = "proxiedproxy.command.reload";
    public static final String COMMAND_PREFIX = "proxiedproxy";
    public static final String[] COMMAND_ALIASES = {"prox"};
    @Getter
    private static EntryConfig entryConfig;
    @Getter
    private static ProxyConfig proxyConfig;
    private static boolean verbose;
    @Getter
    private static Role role = Role.NOT_INITIALIZED;
    @Getter
    private static PlatformPluginInstance instance;
    @Getter
    private static Logger logger;
    private static FileConfig configFile;
    @Getter
    private static Path dataDirectory;
    @Getter
    private static final EventHandler eventHandler = new EventHandler();
    @Getter
    private static final Gson gson = new Gson();

    public static void construct(PlatformPluginInstance pluginInstance, Path instanceDataDirectory, Logger instanceLogger) {
        instance = pluginInstance;
        dataDirectory = instanceDataDirectory;
        logger = instanceLogger;
    }
    /* TODO: Uninitialize */
    public static boolean initialize() {
        boolean success = false;
        try {
            copyResourceFile("config.toml", "config.toml", false);
            copyResourceFile("config.toml", "config-template.toml", true);
            configFile = FileConfig.of(dataDirectory.resolve("config.toml").toFile());
            success = doInitialize();
        } catch (IOException e) {
            logger.warn("IOException detected. Maybe the RSA key files are broken, or something wrong while reading config file.");
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

    private static boolean doInitialize() throws IOException {
        entryConfig = new EntryConfig();
        proxyConfig = new ProxyConfig();
        configFile.load();

        Role configRole = Role.valueOf(configFile.getOrElse("role", "DISABLED").toUpperCase());
        if (!role.equals(Role.NOT_INITIALIZED)) {
            if (!role.equals(configRole)) {
                logger.warn("If you wish to switch role, please restart the proxy.");
                return false;
            }
        }
        role = configRole;
        verbose = configFile.getOrElse("verbose", false);
        switch (role) {
            case ENTRY:
                /* You must use Velocity */
                if (!(instance instanceof VelocityMain)) {
                    logger.error("You must use velocity for an ENTRY server.");
                    return false;
                }
                VelocityMain vInstance = (VelocityMain) instance;
                if (!vInstance.configCheck()) {
                    return false;
                }
                /* Replace /server with your specific command, /hub as default. */
                /* Set verification type */
                entryConfig.verificationType = VerificationType.valueOf(configFile.getOrElse("entry.verification-type", "RSA").toUpperCase());
                entryConfig.entryId = configFile.get("entry.entry-id");
                entryConfig.serverCommandAlias = configFile.getOrElse("entry.server-command-alias", "hub");
                entryConfig.passThroughPingVhost = configFile.getOrElse("entry.pass-through-ping-vhost", true);
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
                fileWriter.write(PPlugin.getGson().toJson(self));
                fileWriter.flush();
                fileWriter.close();
                /* Ping forward settings */
                vInstance.replacePingForward();
                vInstance.replaceServerCommand();
                break;
            case PROXY:
                proxyConfig.allowClientConnection = configFile.getOrElse("proxy.allow-client-connection", true);
                try {
                    proxyConfig.skinServiceBackendVerifier = new SkinServiceBackendVerifier(
                            configFile.getOrElse("proxy.skin-service-backend.allowed", new ArrayList<>()),
                            configFile.getOrElse("proxy.skin-service-backend.blocked", new ArrayList<>()),
                            configFile.getOrElse("proxy.skin-service-backend.union-query-api", SkinServiceBackendVerifier.DEFAULT_UNION_QUERY_API)
                    );
                } catch (MalformedURLException e) {
                    logger.error("Bad union-query-api.");
                    return false;
                }
                try {
                    File trustedEntriesListFile = dataDirectory.resolve("TrustedEntries.json").toFile();
                    JsonReader reader = new JsonReader(new FileReader(trustedEntriesListFile));
                    List<TrustedEntry> trustedEntriesList = PPlugin.getGson().fromJson(reader, new TypeToken<List<TrustedEntry>>() {
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
                    logger.warn("Please consult " + WIKI_URL);
                    copyResourceFile("TrustedEntries.json", "TrustedEntries.json", false);
                }
                if (proxyConfig.trustedEntries.size() == 0) {
                    logger.warn("There are no entries trusted.");
                }
                break;
        }
        configFile.close();
        return true;
    }

    public static void debugOutput(String s) {
        if (verbose) {
            logger.info(s);
        }
    }

    public enum Role {
        ENTRY,
        PROXY,
        DISABLED,
        NOT_INITIALIZED
    }

    private static void copyResourceFile(String filename, String destination, boolean replace) throws IOException {
        File file = PPlugin.getDataDirectory().resolve(destination).toFile();
        if (!replace && file.exists()) {
            return;
        }
        file.delete();
        file.getParentFile().mkdirs();
        file.createNewFile();
        InputStream is = PPlugin.class.getResourceAsStream("/" + filename);
        FileOutputStream fos = new FileOutputStream(PPlugin.getDataDirectory().resolve(destination).toString());
        byte[] b = new byte[1024];
        int length;
        while ((length = is.read(b)) > 0) {
            fos.write(b, 0, length);
        }
        is.close();
        fos.close();
    }
}
