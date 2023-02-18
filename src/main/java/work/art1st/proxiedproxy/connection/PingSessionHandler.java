package work.art1st.proxiedproxy.connection;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.velocitypowered.proxy.VelocityServer;
import com.velocitypowered.proxy.connection.MinecraftConnection;
import com.velocitypowered.proxy.connection.MinecraftSessionHandler;
import com.velocitypowered.proxy.protocol.StateRegistry;
import com.velocitypowered.proxy.protocol.packet.Handshake;
import com.velocitypowered.proxy.protocol.packet.StatusRequest;
import com.velocitypowered.proxy.protocol.packet.StatusResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/* Originally com.velocitypowered.proxy.server.PingSessionHandler */
public class PingSessionHandler implements MinecraftSessionHandler {
    private final CompletableFuture<ServerPing> result;
    private final RegisteredServer server;
    private final MinecraftConnection connection;
    private final ProtocolVersion version;
    private final String vHost;
    private boolean completed = false;

    /* Use vHost here */
    PingSessionHandler(CompletableFuture<ServerPing> result, RegisteredServer server, MinecraftConnection connection, ProtocolVersion version, String vHost) {
        this.result = result;
        this.server = server;
        this.connection = connection;
        this.version = version;
        this.vHost = vHost;
    }

    public void activated() {
        Handshake handshake = new Handshake();
        handshake.setNextStatus(1);
        handshake.setServerAddress(vHost);
        handshake.setPort(this.server.getServerInfo().getAddress().getPort());
        handshake.setProtocolVersion(this.version);
        this.connection.delayedWrite(handshake);
        this.connection.setState(StateRegistry.STATUS);
        this.connection.delayedWrite(StatusRequest.INSTANCE);
        this.connection.flush();
    }

    public boolean handle(StatusResponse packet) {
        this.completed = true;
        this.connection.close(true);
        ServerPing ping = (ServerPing) VelocityServer.getPingGsonInstance(this.version).fromJson(packet.getStatus(), ServerPing.class);
        this.result.complete(ping);
        return true;
    }

    public void disconnected() {
        if (!this.completed) {
            this.result.completeExceptionally(new IOException("Unexpectedly disconnected from remote server"));
        }

    }

    public void exception(Throwable throwable) {
        this.completed = true;
        this.result.completeExceptionally(throwable);
    }

}
