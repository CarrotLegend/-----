# 炽焰神经、凋亡肋骨与元素生物类

2026-11-05；Minecraft 1.20.1、Forge 47.4.28、Java 17（实际运行17.0.12）、official 1.20.1映射、JEI15.59.0.212。包名com.carrot123.yixiangshouronglu。

## 新增内容

| 基因 | 正式ID | 强度/区域/容量 | 分类 | 三品质效果 |
|---|---|---|---|---|
| 炽焰神经 / Blazing Nerves | yixiangshouronglu:blazing_nerves | TETH/神经/8 | 元素生物、突变 | 烈焰领域三个品质相同 |
| 凋亡肋骨 / Withered Ribs | yixiangshouronglu:withered_ribs | TETH/骨骼/6 | 突变、亡灵 | 护甲3/5/7，近战凋零概率5%/10%/15% |

工作区不存在这两个旧基因及凋亡骨髓，故没有替换旧ID或遗留小火球技能。现有8个基因和存档结构保留，本次正式基因增加至10种。

烈焰领域技能ID为yixiangshouronglu:blazing_domain，复用GeneSkills和原R键/选择界面，持续600 tick、每20 tick一次基础8火焰伤害，完整持续30次，冷却从成功发动起2400 tick。首次伤害在发动20 tick后，最后一次在600 tick。失败不进入冷却。

逻辑范围为玩家当前包围盒inflate(10,5,10)，跟随移动，沿用实装HeatUtils的AABB扩展算法，并非另设计的球形距离判定。仅查询同维度现有实体，不主动加载区块。排除自己、死亡/移除对象、盔甲架及旁观者；其他玩家遵守服务器PVP与队伍友伤。多个施法者各自每20 tick最多结算一次同目标。

伤害使用独立动态伤害类型yixiangshouronglu:blazing_domain，加入minecraft:is_fire，源归属玩家，非PLAYER_ATTACK。调用原版hurt，不点燃实体/方块，不设置火焰，不爆炸，不写入灼烧Capability；正常护甲、药水、免疫及外部伤害事件仍可调整最终生命损失。当前AttributesLib会对玩家归属伤害应用随机暴击，因此基础8不等于任何外部属性条件下固定最终8。测试夹具将测试玩家的外部暴击属性中和，验证ACC的基础数值；没有修改正式玩家属性或其他模组代码。

凋亡肋骨用稳定effectId和现有reconcile刷新属性，不逐tick重建修饰符。凋零接入GeneMeleeEvents现有成功伤害队列，事件类型限定PLAYER_ATTACK且直接来源为玩家，取消/零伤害不处理；每个有效事件仅判定一次。凋零Ⅰ持续60 tick，原版addEffect处理免疫和较高等级保留。领域、箭矢等不进入该队列。

## 六条正式配方

两条初始配方均四槽无序、600 tick（30秒），带forge:mod_loaded mutantmore条件：

- blazing_nerves：4个containment_extract（EntityId=mutantmore:mutant_blaze）、烈焰棒4、烈焰粉8、#yixiangshouronglu:meats 2，输出α×1。
- withered_ribs：4个containment_extract（EntityId=mutantmore:mutant_wither_skeleton）、骨头8、煤炭4、下界之星1，输出α×1。

两种基因各有alpha_to_beta与beta_to_gamma升级JSON，放在gene_upgrades目录，沿用现有材料和200 tick（10秒），没有特殊升级材料。20条正式升级配方覆盖当前10种基因。原有配方逐字节校验未改动。

GeneRecipe继续验证输入堆叠数量、提取液部分NBT、基因ID与品质；输出升级只修改Gene.Quality。JEI原插件动态读取真实配方与Tag候选，模块子类型和整数秒时间机制未改动；未新增硬编码展示列表或转移功能。

## 元素分类与Tooltip

分类ID yixiangshouronglu:elemental。Category增加可选非属性描述列表，保留现有四参数构造入口；现有突变和亡灵加成不变。元素分类凑齐4个不同有效基因后，对明确魔法来源乘0.15，5个也只应用一次，回到3个立即失效。

当前正式版本只有炽焰神经贡献元素计数，正常安装一个不会激活奖励，也不添加凑数基因。测试中的acc_test命名空间定义仅存在GameTest进程，不打包发布。

标签data/yixiangshouronglu/tags/damage_type/magic.json默认只含minecraft:magic、minecraft:indirect_magic。外部伤害由数据包明确加入，不把非物理/绕过护甲自动当魔法。火焰、近战、箭矢、爆炸、摔落、饥饿和虚空不会仅因元素分类而被减免。原爆炸减伤分支保留。

所有新增显示文字使用中英文lang。分类效果仍一类一行，附4基因条件，并由GeneDescriptions统一金色斜体；Tooltip不新增注册ID。

## 原Heat视觉复用依据

实际JAR：D:/my gameeeeee/mc/.minecraft/versions/ACC/mods/mutantmore-1.20.1-early_access-2.0.0.jar（文件版本2.0.0，内部metadata1.0.0）。javap核对了：

- com.alexander.mutantmore.entities.MutantBlaze：heatRange/heatRangeY从配置取值；实装与本机配置均10/5。
- com.alexander.mutantmore.util.HeatUtils：createHeatParticles每tick四轮，按整数高度层和相同概率生成；角度均匀，角速度0.001+随机0.05；createHeatRange为包围盒inflate。
- com.alexander.mutantmore.particles.AdvancedParticleOption：网络格式为粒子ID字符串和10个float。
- com.alexander.mutantmore.particles.HeatParticle：v0范围、v1角度、v2角速度、v3跟随实体ID、v4跟随开关，v5–7偏移；v8–9补0。
- assets/mutantmore/particles/heat.json直接引用mutantmore:heat_0至heat_8九张原纹理。

BlazingDomainVisuals通过已注册ParticleType的fromNetwork构造原选项，再由原提供器创建真实HeatParticle；没有复制图像、重新画法阵或直接依赖Mutant More Java类。所有原粒子动画、寿命、透明度和材质由原实现执行。仅复现生成参数，不调用performHeating/heatNearbyMobs/突变烈焰人执行逻辑。

客户端仅持有自己创建的Particle引用，结束/卸载/断线/维度退出时移除本领域粒子，不清除其他来源粒子。真实视觉相似度尚未人工验收。

## 网络、生命周期与可选依赖

GeneNetwork协议从3升为4，新增客户端DomainState包编号4，保留原0–3消息和动作编号。包仅含拥有者UUID、实体ID、维度、会话起点及剩余tick（0–600）。开始/结束发给拥有者与追踪者，开始追踪补发当前剩余时间；不每tick发粒子或完整基因NBT。旧协议客户端需与服务器一同更新。

活动领域仅保存在服务端临时UUID映射；死亡、登出、切维度、卸载来源、无效玩家和服务器停止时清理。重登/重启不恢复活动领域，现有Runtime.SkillCooldowns仍持久化并随原克隆/登录逻辑恢复。没有模块NBT版本或存档迁移。

Mutant More继续可选：缺失时两个初始配方被条件禁用，已有模块仍可识别；缺失heat粒子时技能返回本地化失败原因且不扣冷却。没有新增硬依赖。

## 实际测试结果

- 编译、单元测试及构建通过；最新工作区旧单元测试已被删除，本次未恢复，当前实际运行新增2项单元测试，0失败。不能沿用先前任务46项的数字。
- 有Mutant More的Forge GameTest最终12/12通过：其中4项新领域/分类/近战/合成测试，原8项商店和基因/提取测试回归。
- 无Mutant More隔离GameTest 1/1通过：完整服务端世界加载、两个初始配方禁用、技能发动失败、无活动领域且冷却仍0。
- 遍历10种正式基因的20条升级配方、完整NBT继承和数量；新基因无序初始合成、提取液错误ID拒绝、准确耗材和安装区域/容量；Tooltip字段与效果样式。
- 领域30次基础8伤害、同tick重复调用不重复伤害、移动离开目标、火焰免疫、不点燃、近战效果排除、PVP禁用、多人状态独立、卸载/死亡/登出/维度事件清理、冷却NBT保存恢复。
- 三品质护甲及100次固定种子逐次概率核对；取消/零伤害/箭矢/领域拒绝凋零触发，免疫凋零生物和更高级凋零处理。
- 元素计数1–5及回退至3；20直接/间接魔法变3，其他已测伤害保持20；视觉包编解码。
- 首轮领域断言受外部暴击影响失败，使用本机jdb断点确认首个伤害8，并核对AttributesLib的apothCriticalStrike事件；中和仅测试夹具的暴击属性后非调试测试通过。未通过钳制正式伤害或关闭游戏暴击绕过问题。临时VS Code调试配置已移除，Java调试会话已退出。
- 客户端加载15个模组（含Mutant More及JEI），完成粒子与JEI图集初始化，已用窗口观察确认进入主菜单，随后正常退出，未创建/打开存档。普通runServer在隔离目录重试，因eula=false在世界启动前停止；Gradle任务退出成功不计为专服运行通过。不创建人工测试世界；真实粒子外观、R键操作、JEI列表/重载、实际多人网络、真实重登/死亡跨维度流程未人工验收，不能由GameTest替代。

## 本次全部新增和修改文件

- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/AnomalousContainmentChronicles.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/ElementalGenes.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/BlazingDomain.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneCategories.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneDamageEvents.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneEffects.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneMeleeEvents.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/network/GeneNetwork.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/BlazingDomainVisuals.java`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/zh_cn.json`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/en_us.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/blazing_nerves.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/blazing_nerves_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/blazing_nerves_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/withered_ribs.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/withered_ribs_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/withered_ribs_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/damage_type/blazing_domain.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/tags/damage_type/magic.json`
- `D:/ModIMade/ACC/src/main/resources/data/minecraft/tags/damage_type/is_fire.json`
- `D:/ModIMade/ACC/src/test/java/com/carrot123/yixiangshouronglu/gene/ElementalGenesTest.java`
- `D:/ModIMade/ACC/src/gametest/java/com/carrot123/yixiangshouronglu/gene/ElementalRuntimeTests.java`
- `D:/ModIMade/ACC/src/gametest/java/com/carrot123/yixiangshouronglu/gene/MissingMutantMoreRuntimeTests.java`
- `D:/ModIMade/ACC/src/gametest/java/com/carrot123/yixiangshouronglu/genevalidation/GeneUpgradeRuntimeTests.java`
- `D:/ModIMade/ACC/src/gametest/resources/data/acc_missing/structures/shop_empty.nbt`
- `D:/ModIMade/ACC/docs/elemental-genes.md`

## Java文件职责

| 文件 | 本次变更 |
|---|---|
| AnomalousContainmentChronicles | 注册ElementalGenes |
| ElementalGenes | 两个正式定义、元素分类、烈焰领域技能及护甲刷新 |
| BlazingDomain | 领域状态、目标筛选、定时火焰伤害、追踪同步及生命周期 |
| BlazingDomainVisuals | 原Heat粒子参数构造、客户端生成与精确清理 |
| GeneCategories | ELEMENTAL ID及可选非属性描述；保留旧构造入口 |
| GeneDamageEvents | 明确魔法Tag与元素减伤，保留爆炸减伤 |
| GeneEffects | 新护甲与卸载状态刷新入口 |
| GeneMeleeEvents | 凋亡肋骨加入原近战队列和概率判定 |
| GeneNetwork | 协议4及编号4领域视觉快照 |
| ElementalGenesTest | 数值、计数与激活边界单元测试 |
| ElementalRuntimeTests | 四项有Mutant More的服务端综合GameTest |
| MissingMutantMoreRuntimeTests | 缺少可选模组时的失败与配方条件GameTest |
| GeneUpgradeRuntimeTests | 正式定义筛选与10种基因/20配方覆盖断言 |

额外的acc_missing结构模板仅供缺失模组的隔离测试。语言文件新增两种基因、技能、元素分类、效果、失败提示及死亡信息；既有显示键保持。新damage_type和两个damage_type Tag提供领域火焰身份及严格魔法匹配。

## 命令与产物

```powershell
.\gradlew.bat -I build/acc-gene-validation/init.gradle test build runGameTestServer --offline
.\gradlew.bat -I build/acc-elemental-validation/no-mutantmore.gradle runGameTestServer --offline
.\gradlew.bat -I build/acc-gene-validation/init.gradle test build runServer --offline
.\gradlew.bat -I build/acc-gene-validation/init.gradle runClient --offline
```

- 发行JAR：`D:/ModIMade/ACC/build/libs/yixiangshouronglu-1.0.0.jar`
- 本次修改源码/资源/测试/报告ZIP：`D:/ModIMade/ACC/build/acc-elemental-genes-changes.zip`
- 有模组验证日志：`D:/ModIMade/ACC/build/elemental-validation.log`
- 无Mutant More验证日志：`D:/ModIMade/ACC/build/elemental-no-mutantmore.log`
- 构建/普通专服日志：`D:/ModIMade/ACC/build/elemental-build-server.log`
- 客户端日志：`D:/ModIMade/ACC/build/elemental-client.log`

临时运行配置build/acc-gene-validation/init.gradle沿用既有本机依赖与src/gametest sourceSet，正式build.gradle未改。新建的build/acc-elemental-validation/no-mutantmore.gradle专用于禁用Mutant More并选择acc_missing测试命名空间。ZIP不包含第三方模组JAR。

发行JAR校验：包含20条升级配方和本次2条初始配方，不含GameTest类或acc_missing测试模板。没有修改Mutant More JAR、AI、旧基因数值或收容室规则。旧单元测试删除、其他工作区资源修改均原样保留。

JAR SHA256：`80abc47bac0f174252c6ab278819b14b3c9df5dec10e1175e66e19d314aac5ed`
