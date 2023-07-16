package work.art1st.proxiedproxy.platform.bungeecord;

import lombok.SneakyThrows;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import org.slf4j.LoggerFactory;
import work.art1st.proxiedproxy.PPlugin;
import work.art1st.proxiedproxy.platform.bungeecord.command.ReloadCommand;
import work.art1st.proxiedproxy.platform.common.PlatformPluginInstance;

public class BungeeMain extends Plugin implements PlatformPluginInstance, Listener {
    @Override
    public boolean configCheck() {
        return true;
    }

    @SneakyThrows
    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerCommand(this, new ReloadCommand());
        getProxy().getPluginManager().registerListener(this, new BungeeEventListener());//注册监听器
        new BungeeInjector().inject();
        PPlugin.construct(this, getDataFolder().toPath(), LoggerFactory.getLogger(getLogger().getClass()));
        PPlugin.initialize();
    }

}
