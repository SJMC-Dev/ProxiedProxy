package work.art1st.proxiedproxy.platform.bungeecord;

import lombok.NonNull;
import lombok.SneakyThrows;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import org.slf4j.LoggerFactory;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.bungeecord.command.ReloadCommand;
import work.art1st.proxiedproxy.platform.bungeecord.event.BPreLoginEvent;
import work.art1st.proxiedproxy.platform.common.PlatformPluginInstance;

public class BungeeMain extends Plugin implements PlatformPluginInstance, Listener {

    private BungeeAudiences adventure;
    @Override
    public boolean configCheck() {
        return true;
    }

    @SneakyThrows
    @Override
    public void onEnable() {
        this.adventure = BungeeAudiences.create(this);
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
        new BungeeInjector().inject();
        PPlugin.construct(this, getDataFolder().toPath(), LoggerFactory.getLogger(getLogger().getClass()));
        PPlugin.initialize();
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
         PPlugin.getEventHandler().handlePreLoginEvent(new BPreLoginEvent(event));
    }

    public @NonNull BungeeAudiences adventure() {
        if(this.adventure == null) {
            throw new IllegalStateException("Cannot retrieve audience provider while plugin is not enabled");
        }
        return this.adventure;
    }

    @Override
    public void onDisable() {
        if(this.adventure != null) {
            this.adventure.close();
            this.adventure = null;
        }
    }

}
