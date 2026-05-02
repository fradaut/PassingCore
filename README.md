# PassingCore

PassingCore 是為 **245 passing 伺服器** 準備的 Paper Minecraft 插件，用來集中管理伺服器自訂物品與配套材質包。

目前支援 Minecraft/Paper `26.1.2`，並搭配 Java `25` 建置。

## 目前功能

- 提供單一指令 `/passgive` 取得插件自訂物品。
- 目前已加入自訂物品 `P Coin`，物品 id 為 `p_coin`。
- 目前已加入自訂藥水 `飛行藥水`，物品 id 為 `fly_potion`。
- 一般自訂物品透過插件標記與材質包顯示，不可使用、不可合成，也不會有額外功能。
- `飛行藥水` 可飲用，喝下後獲得 3 分鐘飛行能力，並在物品欄上方顯示倒數。
- 建置時會同時產出插件 jar 與材質包 zip。

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

## 材質包

材質包原始內容位於 `resourcepack/`。

`Resource/p_coin.png` 是目前 `P Coin` 的來源圖片，Gradle 建置時會同步為：

- `resourcepack/pack.png`
- `resourcepack/assets/passingcore/textures/item/p_coin.png`

`Resource/fly_potion.png` 與 `Resource/icon_fly_potion.png` 會同步為飛行藥水物品材質與倒數 UI 圖示。

伺服器若使用自動發送材質包，更新 `PassingCore-resourcepack.zip` 後也記得同步更新 `resource-pack-sha1`。
