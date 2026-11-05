# 传奇萝卜王与金币商店：初版实现及验证报告

本文记录初版单槽售卖实现和当时测试结果。2026-11-05 的背包售卖改造、当前行为及测试结果见 [商店 GUI 修改报告](carrot-king-shop-gui.md)。

## 使用方式

实体ID为 `yixiangshouronglu:legendary_carrot_king`，刷怪蛋ID为 `yixiangshouronglu:legendary_carrot_king_spawn_egg`，已加入ACC创造标签。支持 `/summon yixiangshouronglu:legendary_carrot_king`，不自然生成。20生命，不主动攻击或反击，不主动游荡，不自然消失，可用命名牌改名，无战利品掉落。

提供的64×64皮肤原样复制为 `assets/yixiangshouronglu/textures/entity/legendary_carrot_king.png`，SHA256为 `bf73fcf1d5d7aa2f1688e957fbbe64827d1052336875a29880d36883d3d5f93d`。渲染器使用 `ModelLayers.PLAYER_SLIM` 和 `PlayerModel(..., true)`，保留皮肤外层。

每名玩家初始金币0。第一次右键即在服务端记录已欢迎，展示精确欢迎语；关闭欢迎页也保留记录，后续与任意传奇萝卜王交互直接进入买分页。进入商店按钮在当前菜单中切换页面，客户端不能自行越过欢迎状态发起交易。

欢迎语：欢迎来到异想收容录！我是你的向导“传奇萝卜王”，你可以在我这里买卖物资。

## 交易表及实际物品依据

| 方向 | 物品 | 金币 |
|---|---|---:|
| 卖 | minecraft:iron_ingot | 每个1 |
| 卖 | minecraft:gold_ingot | 每个2 |
| 卖 | minecraft:diamond | 每个5 |
| 卖 | minecraft:netherite_ingot | 每个20 |
| 买 | minecraft:potion，Potion=mutantmonsters:chemical_x | 每瓶2 |
| 买 | mutantmore:compound_z | 每个20 |

MutantMonsters-v8.0.8-1.20.1-Forge.jar的ModRegistry注册 `mutantmonsters:chemical_x` 药水类型；其gunpowder_spice进度明确匹配 `minecraft:potion` 与该药水类型。商品用PotionUtils.setPotion构造，不依赖不存在的独立chemical_x物品。Mutant More依赖JAR的ItemInit注册 `compound_z`。这些注册及实际ItemStack已在Forge GameTest中核对。缺失可选商品时显示不可用，不添加硬依赖。

每次买1个，库存无限。卖分页输入槽接受物品供报价，不支持的物品禁用出售；受支持物品按整组数量结算，按物品ID识别。出售槽属于当前玩家菜单，互不共享；切换分页保留槽内物品。关闭时调用原版clearContainer，退回背包或在无法容纳时掉落。

360×270界面复用GeneScreen的缩放和面板样式，使用代码绘制暗色科技风面板、青色边框、金币占位图标，无新增GUI贴图。买卖标签位于左侧，商品和出售槽位于右侧，金币位于右上角，玩家背包在下部，状态提示在底部。商品图标提供真实Tooltip，禁用购买按钮也可悬停查看金币不足或未注册原因。

## 数据、网络与服务端结算

独立玩家Capability ID为 `yixiangshouronglu:shop`，保存Version=1、Coins、Welcomed、Revision。金币使用非负long，拒绝零或负数增减，拒绝余额溢出。Clone复制金币及欢迎状态；登录、重生、跨维度同步。没有修改基因数据字段或基因网络协议。

服务端API位于ShopService：coins、canPay、add、pay。add/pay只接受ServerPlayer并同步所属玩家。独立网络通道为 `yixiangshouronglu:shop`，协议1；C2S只提交菜单ID、菜单版本、金币版本、动作及商品索引。动作0进入商店、1买分页、2卖分页、3购买、4出售。

所有请求通过enqueueWork在服务端主线程执行，核对玩家归属、菜单ID、NPC实体ID和UUID、存活、同维度、8格距离、版本和空鼠标状态。出售槽、分页和金币变化更新菜单版本；成功交易递增金币revision。客户端不能提交余额、售价、数量或结果物品NBT。旧成功交易请求不能再次结算。

购买先检查商品、余额及主背包/快捷栏36格完整容量，按同物品同NBT合并并尊重堆叠上限；空间不足不扣金币且不掉落购买物品。出售先校验物品及总价。统一事务先保存余额和库存快照，再结算；返回失败或抛出异常时恢复快照。仅在结算完成后同步槽位与状态，且只发给所属玩家。

## 真实验证结果

| 检查 | 结果 |
|---|---|
| Java编译、普通单元测试和完整build | 通过；44项单元测试，0失败，包含新增12项商店测试 |
| 金币初始值、一次性欢迎、玩家独立性 | 单元测试通过 |
| NBT保存读取保留金币、欢迎状态和revision | 单元测试及Forge GameTest通过；真实退出重登尚未手工测试 |
| 四种出售价格、数量1–64计算 | 单元测试通过；实际服务端4钻石售得20金币通过 |
| 购买扣款、旧请求重放、溢出、失败回滚 | 单元测试通过；实际菜单购买药水和出售重放拒绝通过 |
| 两种商品实际注册与普通突变药水NBT | Forge GameTest通过 |
| 满背包拒绝购买且金币保持不变 | 真实Inventory及实际菜单GameTest通过 |
| NPC创建并加入世界、20生命、不自然消失 | Forge GameTest通过；刷怪蛋注册及模型资源检查通过，未手工使用刷怪蛋 |
| 首次交互欢迎、继续按钮动作、第二次直达买页 | 实际ServerPlayer菜单GameTest通过 |
| 欢迎页拒绝购买 | Forge GameTest通过 |
| NPC移除或距离过远使菜单无效 | Forge GameTest通过 |
| 关闭菜单退还未售物品 | 实际ServerPlayer关闭菜单退回7个钻石通过；背包满时掉落分支沿用原版规则，未单独实测 |
| 无图形Forge GameTest服务端 | 3/3通过，完整加载世界并正常保存退出 |
| 普通专服runServer | 使用临时依赖后越过缺失依赖检查，因run/eula.txt为false停止，未完成普通服务器运行验证 |
| 客户端runClient | 使用临时依赖后完成模组、资源与渲染器初始化；随后渲染线程发生glfw.dll原生EXCEPTION_ACCESS_VIOLATION，未完成GUI人工交互 |
| 皮肤原图一致与slim模型 | PNG逐字节一致，64×64，slim绑定静态检查通过；游戏内视觉结果未确认 |
| 单人客户端／真实局域网 | 手工交互和LAN未完成；内存Netty连接的服务端玩家测试不能替代真实LAN测试 |
| 死亡、跨维度、真实重登后的数据 | 保存与复制、同步事件已实现；上述实际生命周期仍需完整客户端环境验证 |

执行命令：`.\gradlew.bat compileJava --offline`、`.\gradlew.bat test build --offline`。临时依赖和测试代码全部位于build/acc-shop-validation，正式build.gradle及运行实例mods目录未改动。

额外执行：`.\gradlew.bat -I build/acc-shop-validation/init.gradle runClient --offline`、同配置的runServer、runGameTestServer。GameTest模板命名和mock连接问题在测试夹具中修正后，最终3项全通过。实际GameTest源码位于 `build/acc-shop-validation/src/com/carrot123/yixiangshouronglu/shopvalidation/ShopRuntimeTests.java`，不属于发布sourceSet。

原生客户端崩溃报告：`D:/ModIMade/ACC/run/hs_err_pid8172.log`。最终GameTest日志：`D:/ModIMade/ACC/build/acc-shop-validation/gametest-run/logs/latest.log`。

## 完整新增／修改文件清单

- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/entity/LegendaryCarrotKingEntity.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/LegendaryCarrotKingRenderer.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/CarrotKingShopScreen.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/ShopClientPackets.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/menu/CarrotKingShopMenu.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/network/ShopNetwork.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/PlayerShopData.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/ShopCapability.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/ShopCatalog.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/ShopTransactions.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/ShopInventory.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/ShopService.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/AnomalousContainmentChronicles.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/registry/ModEntities.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/registry/ModItems.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/registry/ModMenus.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/registry/ModCreativeTabs.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/ClientScreens.java`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/textures/entity/legendary_carrot_king.png`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/models/item/legendary_carrot_king_spawn_egg.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/loot_tables/entities/legendary_carrot_king.json`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/zh_cn.json`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/en_us.json`
- `D:/ModIMade/ACC/src/test/java/com/carrot123/yixiangshouronglu/shop/ShopTest.java`
- `D:/ModIMade/ACC/docs/carrot-king-shop.md`
