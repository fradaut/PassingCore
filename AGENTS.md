# AGENTS.md

本文件提供給 Codex 與其他自動化代理人使用。請在修改本專案前先閱讀，並優先遵守這裡的專案慣例。

## 專案概觀

PassingCore 是一個 Paper Minecraft 插件，目標版本為 Minecraft/Paper `26.1.2`。目前插件提供單一取得物品指令 `/passgive`，並搭配 `resourcepack/` 內的材質包實作自訂物品外觀。

主要產物：

- 插件 jar：`build/libs/PassingCore-1.0-SNAPSHOT.jar`
- 材質包 zip：`build/libs/PassingCore-resourcepack.zip`
- GitHub repo：`git@github.com:fradaut/PassingCore.git`

## 技術環境

- Java toolchain：Java `25`
- Build tool：Gradle wrapper，請使用 `./gradlew`
- Paper API：`io.papermc.paper:paper-api:26.1.2.build.+`
- 插件主類別：`tw.sac.passingcore.PassingCorePlugin`
- Plugin metadata：`src/main/resources/plugin.yml`

建置指令：

```bash
env GRADLE_USER_HOME=/Users/fradaut/IdeaProjects/PassingCore/.gradle-user-home ./gradlew build
```

如果在沙盒內執行 Gradle 遇到 socket 或 lock 權限問題，需要用 escalated 權限重跑同一個命令。

## 目錄與職責

- `src/main/java/tw/sac/passingcore/PassingCorePlugin.java`：插件啟動入口，只做註冊與 wiring，避免塞業務邏輯。
- `src/main/java/tw/sac/passingcore/command/PassGiveCommand.java`：`/passgive` 指令解析、玩家查找、tab complete。
- `src/main/java/tw/sac/passingcore/item/CustomItemRegistry.java`：所有插件自訂物品的登記處。
- `src/main/java/tw/sac/passingcore/item/CustomItemDefinition.java`：單一自訂物品的建立、辨識、舊 id 相容與 meta 套用。
- `src/main/java/tw/sac/passingcore/item/CustomItemListener.java`：阻止自訂物品使用、放置、消耗，以及防止被 craft/anvil/smithing/grindstone 產生結果。
- `Resource/`：原始圖片來源。目前 `Resource/p_coin.png` 是 P Coin 圖片來源。
- `resourcepack/`：Minecraft resource pack 原始內容。

## 指令規格

只保留一個取得插件物品的指令：

```text
/passgive <item> [amount]
/passgive <player> <item> [amount]
```

規則：

- 玩家輸入 `/passgive <item>` 時給自己。
- 玩家或 console 輸入 `/passgive <player> <item>` 時給指定在線玩家。
- 數量 `[amount]` 可省略，預設為 `1`，目前允許 `1-64`。
- console 不允許省略玩家。
- 不要重新新增 `/hello`、`/passingitem` 或 `/passitem`。
- 新增插件物品時，應透過 `CustomItemRegistry` 登記，並自然支援 `/passgive`，不要新增一個物品一個指令。

目前已登記物品：

- `p_coin`
- aliases：`pcoin`、`p-coin`、`pc`
- legacy id：`passing_core_token`
- `fly_potion`
- aliases：`flypotion`、`fly-potion`、`flight_potion`
- consume action：喝下後給予 3 分鐘飛行能力，並以 action bar 顯示 `飛行時間00:00` 倒數。

## 新增自訂物品流程

新增插件物品時，請照這個順序做：

1. 在 `resourcepack/assets/passingcore/textures/item/` 放入小寫 snake_case 的 PNG，例如 `new_item.png`。
2. 新增 `resourcepack/assets/passingcore/models/item/new_item.json`，並讓 `layer0` 指向 `passingcore:item/new_item`。
3. 新增 `resourcepack/assets/passingcore/items/new_item.json`，並讓 model 指向 `passingcore:item/new_item`。
4. 在 `CustomItemRegistry.createDefault(...)` 內新增 `CustomItemDefinition.Builder`。
5. 確認 id、item model、texture 檔名都使用同一個小寫 snake_case id。
6. 執行 `./gradlew build`，確認 jar 與 resource pack zip 都有更新。

如果物品曾改名，請用 `legacyIds(...)` 保留舊 item model / PDC tag 相容，避免玩家既有物品破圖或無法被辨識。

## 材質包規則

- `pack.png` 是 Minecraft resource pack 封面固定檔名，不要改名。
- `Resource/p_coin.png` 會在 Gradle `syncResourceImages` task 中同步為：
  - `resourcepack/pack.png`
  - `resourcepack/assets/passingcore/textures/item/p_coin.png`
- `Resource/fly_potion.png` 會同步為 `resourcepack/assets/passingcore/textures/item/fly_potion.png`。
- `Resource/icon_fly_potion.png` 會同步為 `resourcepack/assets/passingcore/textures/ui/icon_fly_potion.png`。
- 飛行藥水倒數 UI 使用 `resourcepack/assets/passingcore/font/hud.json` 定義自訂 glyph，插件以 action bar 顯示圖示與倒數。純插件加材質包無法真正改寫 vanilla hotbar 版面；若要更精準的位置需 client mod。
- `resourcepack/assets/passingcore/items/*.json` 是新版 item model definition。
- `resourcepack/assets/passingcore/models/item/*.json` 是實際 item model。
- 不要把 `.DS_Store` 放進材質包 zip；`resourcePack` task 已排除 `**/.DS_Store`。

## 編碼與風格

- Java 程式碼維持簡潔、單一職責；不要把新邏輯塞回 `PassingCorePlugin`。
- 使用 Adventure `Component` 來設定名稱與訊息。
- 自訂物品名稱請同時設定 `itemName(...)` 與 `displayName(...)`，讓遊戲顯示與舊物品升級更穩。
- 自訂物品必須有 PDC tag 與 `item_model`，辨識時兩者都要檢查。
- 自訂物品預設應無功能、不可使用、不可合成；若未來要新增有功能物品，請先調整 listener 設計，不要直接繞過現有保護。

## Git 與提交注意事項

- 不要提交 `build/`、`.gradle/`、`.gradle-user-home/`、`.idea/`、`.DS_Store`。
- 可以提交 Gradle wrapper：`gradlew`、`gradlew.bat`、`gradle/wrapper/*`。
- 修改後至少執行 `./gradlew build`。
- 推送目標 remote：`origin git@github.com:fradaut/PassingCore.git`。
- 若使用 SSH 推送失敗，先確認 `ssh -T git@github.com` 是否成功。

## 驗證清單

每次修改插件行為後，至少確認：

- `./gradlew build` 成功。
- `build/libs/PassingCore-1.0-SNAPSHOT.jar` 已產出。
- `build/libs/PassingCore-resourcepack.zip` 已產出。
- `unzip -p build/libs/PassingCore-1.0-SNAPSHOT.jar plugin.yml` 內指令與權限符合預期。
- 若改材質包，確認 zip 內路徑符合 Minecraft resource location 規則，小寫且不要有空白。
