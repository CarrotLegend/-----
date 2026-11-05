# 传奇萝卜王商店 GUI 与背包售卖改造

日期：2026-11-05。Minecraft 1.20.1、Forge 47.4.28、Java toolchain 17，实际运行 JVM 17.0.12；包名 com.carrot123.yixiangshouronglu。

## 当前行为

保留360×270窗口、GeneScreen面板和缩放、玩家背包与快捷栏坐标、左侧按钮位置以及右上角金币显示。商店标题为“商店界面／Shop”；欢迎页仍使用原NPC标题及原欢迎语。左侧按钮为“购买／售卖”，选中页有金色标记。

售卖卡片126×46，坐标(88,46)、(222,46)、(88,98)、(222,98)，依次铁锭、金锭、钻石、下界合金锭。普通左键卖一个，Shift+左键卖背包中全部对应物品；悬停显示单价、拥有数量和操作说明。选中卡片为金色边框，悬停使用青色边框与背景反馈。购买和售卖共用drawCard绘制；购买仍为两行商品与原购买按钮。

删除默认底部说明、旧单槽报价、整组出售按钮及相关专用语言键；状态为ok时完全跳过底部渲染，真实成功/失败结果继续显示。没有新增GUI贴图，没有修改GeneScreen或其他界面。

## 售卖槽与存档

删除原菜单私有SimpleContainer、售卖Slot、saleStack/saleValue接口及clearContainer关闭退物分支。菜单只含玩家36槽：菜单0–26映射主背包9–35，菜单27–35映射快捷栏0–8；主背包与快捷栏可以双向Shift移动，欢迎页继续禁止库存操作。

旧输入槽从未序列化到存档，正常关闭旧版菜单/退出旧版服务器时已由原规则退还；新版无需迁移，也不会创建或清空存档内的隐藏容器。不支持热替换正在运行的旧版菜单。金币、欢迎状态和revision持久化、死亡复制、登录和跨维度同步代码均未修改；当前没有独立交易记录存档。

## 服务器交易与同步

ShopCatalog.SELL_PRODUCTS是唯一有序售卖目录，价格分别1、2、5、20；服务器构造快照发送四种图标与价格，客户端不硬编码售价。购买仍为minecraft:potion，Potion=mutantmonsters:chemical_x，2金币，以及mutantmore:compound_z，20金币。物品来源沿用初版已核对的注册对象，本次GameTest再次核对实际药水NBT及复合物Z注册。

独立shop协议从1升为2，基因网络不变。请求字段不变：窗口ID、菜单revision、钱包revision、动作、商品索引。动作0进入、1购买页、2售卖页、3购买，4售卖一个、5售卖全部。旧客户端被握手版本检查拒绝。

请求在服务端主线程执行，重新检查玩家当前containerMenu身份、所有者、窗口ID、NPC类型/UUID/存活/同维度/8格距离、页面、版本及空鼠标状态。服务器只接受售卖白名单索引，数量由0–35背包槽的真实物品决定，按注册ID匹配。自定义名称或NBT不影响资格；剩余栈复制并保留原NBT。护甲、副手、Curios、其他容器、鼠标物品不参加统计。

ShopInventory.remove只生成副本计划；单卖扣1，批量扣所有匹配数量。收入使用long与multiplyExact，钱包检查余额溢出后复用ShopTransactions.commit，一次结算背包和钱包。异常或失败恢复完整36槽快照及钱包状态。成功后通过原版槽位同步和所属玩家状态包刷新。没有向其他玩家广播完整背包。

菜单观察36槽内容、分页及钱包版本，任一变化递增菜单revision；每次请求处理前再次观察，未等下一tick的背包变化也会拒绝旧请求。成功结算递增钱包revision，因此重复请求无法再次获利。背包空时返回“背包中没有该物品”。

## 验证记录

- 静态检查：JSON能解析、无重复键；中英文商店键及%s占位符一致；git diff --check通过。旧单槽UI引用已删除。
- compileJava、test、build通过；最终45项JUnit测试，0失败、0错误。存在API弃用警告，没有编译错误。发行JAR中语言与标题已核对，不包含GameTest类。
- Forge GameTest：4/4通过，真实世界、真实ServerPlayer和背包，使用内存Netty连接，不等同真实客户端或局域网。
- 覆盖四种材料单卖、跨主背包/快捷栏批量、剩余物品自定义名称保留、护甲/副手排除、鼠标非空拒绝、空背包拒绝、非法索引、背包变化后旧请求拒绝、旧成功请求重放、余额溢出零消耗、关闭不吞物或复制、36槽结构及双向快捷移动。
- 回归首次欢迎、欢迎页拒绝交易、继续与再次交互直达购买页、余额不足、满背包不扣款、药水购买与NBT、金币/欢迎/revision序列化往返；事务失败和异常回滚由现有单元测试覆盖。
- 普通runServer本次重试仍因run/eula.txt=false停止；Gradle任务退出成功不表示服务器已启动。未代用户接受EULA。
- 客户端本次runClient完成14个模组、声音、纹理及渲染初始化，10:10:33记录Stopping!，随后Gradle正常退出；窗口关闭前未能进入商店。没有本次新的JVM崩溃报告，不能将昨日GLFW崩溃当成本次结果。GUI人工视觉、缩小界面命中、真实联机、死亡/重登/跨维度持久化均未完成，不能由上述GameTest代替。客户端日志：build/acc-shop-validation/client-gui-update.log。

执行命令：

```powershell
.\gradlew.bat compileJava test build --offline
.\gradlew.bat -I build/acc-shop-validation/init.gradle runGameTestServer --offline
.\gradlew.bat -I build/acc-shop-validation/init.gradle runServer --offline
.\gradlew.bat -I build/acc-shop-validation/init.gradle runClient --offline
```

GameTest已保存至src/gametest，不加入正式发布sourceSet；临时init.gradle接入该源码和资源及既有本机依赖，不改变正式build.gradle，不将第三方依赖加入发行包。服务端测试日志：build/acc-shop-validation/gametest-gui-update.log；普通专服日志：build/acc-shop-validation/server-gui-update.log。

## 本次全部源码、资源及文档路径

- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/CarrotKingShopScreen.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/ShopClientPackets.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/menu/CarrotKingShopMenu.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/network/ShopNetwork.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/ShopCatalog.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/shop/ShopInventory.java`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/zh_cn.json`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/en_us.json`
- `D:/ModIMade/ACC/src/test/java/com/carrot123/yixiangshouronglu/shop/ShopTest.java`
- `D:/ModIMade/ACC/src/gametest/java/com/carrot123/yixiangshouronglu/shopvalidation/ShopRuntimeTests.java`
- `D:/ModIMade/ACC/src/gametest/resources/data/yixiangshouronglu/structures/shop_empty.nbt`
- `D:/ModIMade/ACC/docs/carrot-king-shop.md`
- `D:/ModIMade/ACC/docs/carrot-king-shop-gui.md`

## 交付产物

- 模组：`D:/ModIMade/ACC/build/libs/yixiangshouronglu-1.0.0.jar`
- 本次修改源码、资源、测试及运行配置：`D:/ModIMade/ACC/build/acc-shop-gui-changes.zip`
- 独立单元测试报告：`D:/ModIMade/ACC/build/reports/tests/test/index.html`

临时运行配置修改路径：`D:/ModIMade/ACC/build/acc-shop-validation/init.gradle`。其测试源码入口从build目录改为src/gametest，依赖JAR仍来自本机既有临时Maven目录；ZIP不包含第三方JAR。使用普通test/build不需要该init脚本。未修改NPC实体、皮肤、AI、PlayerShopData、ShopCapability、ShopService、ShopTransactions或任何基因系统代码。
