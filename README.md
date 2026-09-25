# PassingCore

PassingCore 是為 **245 passing 伺服器** 準備的 Paper Minecraft 插件，用來集中管理伺服器自訂物品與配套材質包。

目前支援 Minecraft/Paper `26.2` build `129`，並搭配 Java `25` 建置。

## 目前功能

- 提供單一指令 `/passgive` 取得插件自訂物品。
- 目前已加入自訂物品 `P Coin`，物品 id 為 `p_coin`。
- 目前已加入自訂藥水 `飛行藥水`，物品 id 為 `fly_potion`。
- 一般自訂物品透過插件標記與材質包顯示，不可使用、不可合成，也不會有額外功能。
- `飛行藥水` 可飲用，喝下後獲得 3 分鐘飛行能力，並在物品欄上方顯示倒數。
- 建置時會同時產出插件 jar 與材質包 zip。
- 插件會自行提供資源包下載，玩家加入時自動收到最新版資源包。

## 指令

玩家給自己物品：

```text
/passgive <物品> [數量]
```

指定玩家：

```text
/passgive <玩家> <物品> [數量]
```

數量未填時預設為 `1`。Console 使用時必須指定玩家。

範例：

```text
/passgive p_coin
/passgive Steve p_coin
/passgive fly_potion
/passgive Steve fly_potion 3
```

權限：

```text
passingcore.passgive
```

## 建置

```bash
./gradlew build
```

建置完成後會產生：

- `build/libs/PassingCore-1.0-SNAPSHOT.jar`
- `build/libs/PassingCore-resourcepack.zip`

在本機執行 `build` 時，產物也會自動部署到：

- `/Users/fradaut/Run/MinecraftServer/paperMC_26.2#129/plugins/PassingCore-1.0-SNAPSHOT.jar`
- `/Users/fradaut/Run/MinecraftServer/paperMC_26.2#129/plugins/PassingCore/resourcepack.zip`
- `/Users/fradaut/Run/MinecraftServer/paperMC_26.2#129/plugins/PassingCore/config.yml`

若伺服器路徑不同，可用 `-PpassingCoreServerDir=/path/to/server` 覆寫。

## 材質包

材質包原始內容位於 `resourcepack/`。

`Resource/p_coin.png` 是目前 `P Coin` 的來源圖片，Gradle 建置時會同步為：

- `resourcepack/pack.png`
- `resourcepack/assets/passingcore/textures/item/p_coin.png`

`Resource/fly_potion.png` 與 `Resource/icon_fly_potion.png` 會同步為飛行藥水物品材質與倒數 UI 圖示。

資源包會被封裝進插件 jar，插件啟動時會解出到 `plugins/PassingCore/resourcepack.zip`，並自動計算 SHA-1。玩家加入後，插件會要求載入資源包；不需要手動修改 `server.properties` 的 `resource-pack` 或 `resource-pack-sha1`。

內建下載服務使用 TCP `8123`，正式下載網址為 `http://mtdl.sac.tw:8123/PassingCore-resourcepack.zip`。請確認防火牆與路由器允許玩家連線該埠；若日後改用反向代理或 CDN，可在 `plugins/PassingCore/config.yml` 更新 `resource-pack.public-url`。
