package work.art1st.proxiedproxy.platform.common.connection;

import net.kyori.adventure.text.Component;

import java.net.InetSocketAddress;
import java.util.Locale;

public interface PLoginInboundConnection {
    void sendLoginPluginMessage(String message);
    boolean isDirectConnection();
    /** Should only affect the effect of functions like "getRemoteAddress", not the address of the actual connection. */
    void setRemoteAddress(String remoteAddress);
    void disconnect(Component reason);
    default boolean isVHostFromClient(InetSocketAddress address, String vHostValue) {
        String cleanedAddress = address == null ? "" : address.getHostString().toLowerCase(Locale.ROOT);
        String origAddress = vHostValue.toLowerCase(Locale.ROOT);
        if (!origAddress.isEmpty() && origAddress.charAt(origAddress.length() - 1) == '.') {
            origAddress = origAddress.substring(0, origAddress.length() - 1);
        }
        return !(!cleanedAddress.equals(origAddress)
                && !origAddress.endsWith("\0fml\0")
                && !origAddress.endsWith("\0fml2\0")
                && !origAddress.endsWith("\0fml3\0")
                && !origAddress.endsWith("\0fml4\0"));
    }
}
