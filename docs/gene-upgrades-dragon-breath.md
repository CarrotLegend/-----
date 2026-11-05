# 基因品质升级与幼年末影龙龙息兑换

日期：2026-11-05。Minecraft 1.20.1，Forge 47.4.28，Java toolchain 17（实际运行17.0.12），official 1.20.1映射，JEI 15.59.0.212。包名com.carrot123.yixiangshouronglu。

## 正式配方覆盖

所有基因命名空间均为yixiangshouronglu。每种基因两条升级配方，各耗时200 tick（10秒），输出模块一个。

| 基因 | 注册ID路径 | α→β | β→γ |
|---|---|---|---|
| 再生细胞 | regenerative_cells | 已添加 | 已添加 |
| 骸骨视网膜 | skeletal_retina | 已添加 | 已添加 |
| 爆裂骨骼 | blast_skeleton | 已添加 | 已添加 |
| 末影突触 | ender_synapse | 已添加 | 已添加 |
| 巨钳外骨骼 | clawdian_exoskeleton | 已添加 | 已添加 |
| 沙尘之心 | heart_of_dust | 已添加 | 已添加 |
| 藤蔓肌束 | vine_muscles | 已添加 | 已添加 |
| 冰封骨髓 | frozen_bone_marrow | 已添加 | 已添加 |

配方ID为`yixiangshouronglu:gene_upgrades/<gene>_alpha_to_beta`及`yixiangshouronglu:gene_upgrades/<gene>_beta_to_gamma`。没有新增类型或序列化器，均使用原`yixiangshouronglu:gene_synthesis`序列化器及UPGRADE模式。没有覆盖基础合成配方。未来新增基因仍按当前项目格式添加两条JSON，不引入动态生成器。

| 槽位 | α→β | β→γ |
|---|---|---|
| 0 | 同种α模块×1 | 同种β模块×1 |
| 1 | minecraft:netherite_ingot×4 | minecraft:netherite_ingot×16 |
| 2 | minecraft:nether_star×1 | minecraft:dragon_breath×4 |
| 3 | #yixiangshouronglu:meats×16 | cataclysm:enderite_block×1 |

末影合金块依据：实际依赖curse.maven:lendercataclysm-551586:6906993，JAR内ModBlocks.class和ModItems.class均有enderite_block注册常量，并包含blockstates、物品模型和战利品资源；真实Forge配方加载与匹配测试再次校验该物品。

## 逐项改动与兼容

- GeneModuleItem：仅删除自定义Tooltip中的基因ID行；名称、强度、品质、区域、容量、类别及金色斜体效果保留。升级复制原栈为1个，只写Gene.Quality，取消分类NBT重写；Gene.Id、Strength、Version、Categories、Extension、自定义名称与额外根NBT均原样继承。未改变模块NBT版本、UUID或任何效果数值。
- 16条JSON：明确基因ID、输入品质、四槽材料及数量、目标品质和200 tick。当前全部八种基因均覆盖。
- ChamberSpecialProducts：仅新增幼年末影龙的严格同EntityId特殊产物定义，1个提取液换1个龙息。
- ChamberSpecialProductsTest：补充对应实体、错误实体和空标识的单元测试。
- GeneUpgradeRuntimeTests：服务端遍历全部正式升级配方，测试真实BlockEntity tick和容器库存，另检查Tooltip与特殊产物。

GeneRecipe原有匹配同时检查Ingredient、基因ID、合法定义/强度及品质，并校验每槽数量；UPGRADE要求槽0为当前基因且品质只能.next()。合成台已有四槽，无需改GUI。结算仅在逻辑服务端进行，预检输出可完整容纳，制作完成再次匹配，并按各input.count抽取材料。因此16个材料会实际消耗16个。未修改GeneRecipe、机器结算代码、菜单、网络或安装逻辑。

JEI原实现直接读取RecipeManager的GeneRecipe，按mode进入gene_upgrade类别，使用同一Ingredient的Tag候选并设置数量、同基因/品质NBT和完整输出预览，时间显示为整数10秒。无需修改JEI插件；保持可选依赖。用户自定义名称或额外NBT只能由实际输入继承，JEI展示标准模块预览。

## 龙息兑换流程

仅当收容室SoulEntityId和输入提取液EntityId都为`yixiangshouronglu:young_ender_dragon`时匹配。`minecraft:ender_dragon`、其他Boss和无EntityId不匹配。原版末影龙未新增该配方。

沿用原特殊产物tickExtraction流程及EXTRACT_TIME=20，先检查输入数量和输出容量，完成时消耗1个输入，再存入1个龙息并归零进度。输入耗尽不再产出，输出堵塞不扣料。此分支不扣提取液储量，不改变原提取器消耗规则、归一化规则、产蛋页面或进度条；没有修改收容室BlockEntity或GUI。

## 实际验证

- 静态：原有14条配方逐字节SHA256一致；新增16条JSON的数量、时间、同基因及品质对应检查通过。
- 编译与构建：compileJava、test、build通过，存在API弃用警告；46项单元测试，0失败。
- Forge GameTest：最终8/8通过（本次新增4项，已有商店4项），服务端完整加载世界并保存退出。补充断言确认旧分类NBT保持原样，以及肉类Tag中每种候选物品都能匹配α→β。
- 新增测试遍历8种基因×2级；199 tick不提前扣料或产出，200 tick后各槽准确消耗；保留完整NBT，输入耗尽无重复产出，错误基因/品质/版本及材料少1拒绝，堵塞输出不扣料，解除堵塞可继续合成。
- Tooltip测试覆盖8种基因三品质：ID不显示，其他字段与金色斜体效果保留；模块NBT往返读取通过。不能替代客户端本地化视觉或已安装基因的实际游玩测试。
- 龙息测试：第20 tick消耗1换1，连续两份输入只产两份，输入耗尽不再生成，输出满不消耗，错误提取液/收容室/缺失EntityId拒绝，原产蛋流程仍产蛋。
- 客户端已使用实际JEI 15.59.0.212启动至主菜单。新建独立创造测试世界时出现实验性功能确认，用户已明确取消创建，未进入世界；因此JEI配方列表、轮换动画、配方重载、Tooltip本地化视觉及真实玩家升级/兑换未人工验收。普通专服已在独立开发目录重试，因尚未接受EULA而在世界启动前停止；虽然Gradle任务退出成功，但不计为普通专服运行通过。日志位于build/acc-gene-validation/server-run/logs/latest.log。没有将GameTest或构建成功当作人工游戏验收。

运行命令：

```powershell
.\gradlew.bat -I build/acc-shop-validation/init.gradle compileJava test build runGameTestServer --offline
.\gradlew.bat -I build/acc-gene-validation/init.gradle runGameTestServer --offline
.\gradlew.bat -I build/acc-gene-validation/init.gradle runClient --offline
.\gradlew.bat -I build/acc-gene-validation/init.gradle runServer --offline
```

运行配置仅位于build目录，使用本机已有依赖，未修改正式build.gradle或真实ACC实例mods。带JEI客户端使用本机ACC实例的15.59.0.212 JAR作为临时运行依赖，普通构建和服务端测试不添加JEI运行依赖。

## 本次全部新增及修改文件

- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/item/GeneModuleItem.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/production/ChamberSpecialProducts.java`
- `D:/ModIMade/ACC/src/test/java/com/carrot123/yixiangshouronglu/production/ChamberSpecialProductsTest.java`
- `D:/ModIMade/ACC/src/gametest/java/com/carrot123/yixiangshouronglu/genevalidation/GeneUpgradeRuntimeTests.java`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/blast_skeleton_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/blast_skeleton_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/clawdian_exoskeleton_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/clawdian_exoskeleton_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/ender_synapse_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/ender_synapse_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/frozen_bone_marrow_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/frozen_bone_marrow_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/heart_of_dust_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/heart_of_dust_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/regenerative_cells_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/regenerative_cells_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/skeletal_retina_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/skeletal_retina_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/vine_muscles_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/vine_muscles_beta_to_gamma.json`
- `D:/ModIMade/ACC/docs/gene-upgrades-dragon-breath.md`

## 运行依赖与限制

临时运行配置：`D:/ModIMade/ACC/build/acc-gene-validation/init.gradle`，GameTest沿用已有隔离目录，普通专服使用build/acc-gene-validation/server-run，JEI仅为runClient临时加入。日志：`D:/ModIMade/ACC/build/acc-gene-validation/gametest.log`、`D:/ModIMade/ACC/build/acc-gene-validation/client.log`。

现有Mutant More依赖在测试中报告缺少Dungeons Mobs的可选实体数据和一项外部Tag；不影响本次8项测试，未为此修改其他模组或扩大依赖。未进行真实多人联机、旧生产存档升级、实际装备效果游玩测试；仅模块NBT往返和原数据接口检查不能替代这些项目。

## 产物

- 发布JAR：`D:/ModIMade/ACC/build/libs/yixiangshouronglu-1.0.0.jar`
- 本次修改源码、16条JSON、测试与报告：`D:/ModIMade/ACC/build/acc-gene-upgrades-changes.zip`
- JAR SHA256：`d955893adb18d77d7dfaa7b96bb52644c15b6d5f2d3e8ae99d9491b671fef70b`

发行包已确认包含16条升级JSON，且不包含GameTest测试类。未改动原有14条配方、肉类Tag、基因效果/属性/安装、商店或其他界面。未新增中文硬编码或Java注释。

客户端人工验收暂停原因：新测试世界出现实验性功能确认。computer-use的确认规则要求，未预先明确授权时，第三方“are you sure?”警告需要用户确认；用户随后明确选择取消创建，因此没有点击“是”，仅保留自动测试结果。规则文件：C:/Users/wzx18/.codex/plugins/cache/openai-bundled/computer-use/26.1029.61003/docs/confirmations.md。后续检查时该开发客户端窗口已关闭；没有操作仍在运行的用户单人游戏窗口，也没有打开已有用户存档。
