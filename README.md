# ProxiedProxy
这个插件允许你将现有的Velocity接入另一个Velocity。

## 配置文件结构
```
- proxied-proxy
  - config.toml
  - TrustedEntries.json
  - privateKey.pem
  - publicKey.pem
  - entry.json
```

### config.toml
```
# 服务器角色可用选项:
# - PROXY: 接受来自上游ENTRY服务器的连接
# - ENTRY: 将客户端连接代理到PROXY服务器或Minecraft服务器。ENTRY应当设置为legacy/bungeeguard forwarding。
#          下游Minecraft服务器(spigot/paper/...)应做相应设置。它们不支持基于RSA/KEY的验证。
# - ENTRY_AND_PROXY: 同时作为ENTRY和PROXY，需要同时按照两者的要求进行配置
role = "PROXY"

# 以下选项只针对PROXY服务器
[proxy]
# 是否允许客户端直接连接PROXY
allow-client-connection = true
# 定时从远程拉取ENTRY列表，覆盖TrustedEntries.json并自动重载
# 手动维护TrustedEntries.json时请保持false
auto-fetch-entries = false
# 拉取源URL
auto-fetch-url = "https://skin.mualliance.ltd/api/union/network/entry"
# 拉取间隔（分钟）
auto-fetch-interval-minutes = 60

# 以下选项只针对ENTRY服务器
[entry]
# 替换/server指令
server-command-alias = "hub"
# 验证类型可用选项:
# - RSA: 推荐
# - KEY
# 下游PROXY服务器需要把entry.json中的内容添加到他们的TrustedEntries.json数组中。
# entry.json是一个方便传输的文件，直接修改它不会有任何效果。
verification-type = "RSA"
# ENTRY服务器的ID
entry-id = ""

[entry.key]
# 仅verification-type设置为KEY时有效
key = ""
```

### TrustedEntries.json
储存PROXY服务器信任的ENTRY服务器列表。
```
[
  {
    "id": "...",
    "publicKey": "..."
  },
  {
    "id": "...",
    "key": "..."
  }
]
```

### PROXY设置教程
安装本插件，将上游ENTRY的公钥/KEY添加到`TrustedEntries.json`中（注意json格式），使用`/prox reload`重新加载配置文件。

也可以自动同步：在`config.toml`中设置`proxy.auto-fetch-entries = true`，插件会按`proxy.auto-fetch-interval-minutes`（分钟，默认60）的间隔从`proxy.auto-fetch-url`拉取条目列表并覆盖`TrustedEntries.json`。开启后请勿再手工维护该文件；拉取失败或返回空列表时保留原有列表。

## 构建

- 下载源码，下载`velocity.jar`到`libraries`目录。
- 执行`gradle jar`

## See Also
[MUA Proxy Plugin](https://github.com/MagicalSheep/mua-proxy-plugin)：上游ENTRY的frps协同插件

[MUA Frp Daemon](https://github.com/MUAlliance/MUAFrpDaemon)：frpc守护进程，支持ENTRY列表自动同步，支持运行Velocity

[Wiki: 联合大厅](https://wiki.mualliance.ltd/%E8%81%94%E5%90%88%E5%A4%A7%E5%8E%85)
