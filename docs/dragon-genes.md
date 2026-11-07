# 四种基因、龙类分类与龙息弹

2026-11-06。Forge 1.20.1／47.4.28、Java 17，JEI 15.59.0.212。

| 注册 ID | 区域／容量／分类 | α、β、γ 效果 |
|---|---|---|
| yixiangshouronglu:wither_core | 心肺／6／亡灵 | 攻击 +2／4／6，生命偷取 +10／20／30%，过量治疗 +5／10／15% |
| yixiangshouronglu:ender_dragon_brain | 神经／8／龙类 | 最大生命 +15／30／45，龙息弹技能 |
| yixiangshouronglu:berserker_tendons | 肌肉／5／突变 | 攻击 +3／6／9，有效受伤后以 13／36／39% 概率获得力量Ⅱ |
| yixiangshouronglu:void_carapace | 骨骼／4／突变 | 护甲 +3／5／7，免疫漂浮 |

四种基因均为 TETH。固定属性用现有刷新机制与稳定 UUID 计算。实装 ApothicAttributes 1.3.7 的 attributeslib:life_steal、attributeslib:overheal 均使用 ADDITION 小数比例，治疗由该模组已有事件结算。狂暴肌腱在服务端 tick 末复核 LivingDamageEvent，每个事件最多一次判定，明确排除 ACC 冰封骨髓同次攻击的额外冻伤。已有同级或更强力量时保留原效果。默认力量Ⅱ 200 tick，由 geneSkills.berserkerStrengthDurationTicks 配置。虚空甲壳在刷新时清除漂浮，并通过已有药水适用事件阻止新漂浮。

龙类 yixiangshouronglu:dragon 安装一个生效，由分类统一增加最大生命 40 与攻击 8。三个原有分类门槛均为四个。Tooltip 与改造舱所选模块的分类状态共用分类门槛，使用中英文语言资源。每类奖励依旧合并为一行金色斜体描述。

## 配方

| 配方路径 | 提取液 ×4 的 EntityId | 其余材料 |
|---|---|---|
| wither_core | minecraft:wither | 下界之星 ×1、凋灵骷髅头颅 ×3、灵魂沙 ×4 |
| ender_dragon_brain | minecraft:ender_dragon | 龙息 ×8、末影水晶 ×1、龙首 ×1 |
| berserker_tendons | mutantmore:mutant_hoglin | 生猪排 ×8、下界岩 ×4、金锭 ×1 |
| void_carapace | mutantmore:mutant_shulker | 潜影壳 ×4、骨头 ×8、紫颂果 ×4 |

四条初始配方为四槽无序，600 tick，输出 α ×1。后两条配方需要 Mutant More。两个实体 ID 已从实装 mutantmore-1.20.1-early_access-2.0.0.jar 核对。终末龙脑按确认选择严格接受原版末影龙提取液，当前常规生存收容流程缺少该材料来源。

八条升级配方各 200 tick。α→β 使用对应模块、下界合金锭 ×4、下界之星 ×1、肉类标签 ×16。β→γ 使用对应模块、下界合金锭 ×16、龙息 ×4、灾变末影合金块 ×1。品质与其他 NBT 继承由现有 GeneRecipe 处理。发行 JAR 包含 28 条升级配方。JEI 从现有 RecipeManager 读取真实配方，使用原有提取液及基因 NBT 子类型、数量和整数秒显示逻辑。

## 龙息弹

复用现有 R 键技能选择，技能 ID 为 yixiangshouronglu:dragon_breath。成功发射后冷却默认 100 tick，可用 geneSkills.dragonBreathCooldownTicks 调整。

gene_dragon_fireball 使用原版 DragonFireball 飞行、轨迹和渲染。服务端有效命中由间接魔法伤害处理，直击基础 20 点，弹体为直接来源，玩家为伤害归属。碰撞只生成一朵专用 gene_dragon_breath_cloud。云复用原版 AreaEffectCloud 粒子、20 tick 等待、600 tick 活跃时间及 3 到约 7 格的半径变化。每隔 20 tick 在水平圆形范围内结算基础 8 点，完整生命周期设计为 30 次。专用云使用空药水和独立伤害结算。实体与云通过原版实体同步处理视觉。命中遵守 PVP 和队伍友伤，施法者免于自身弹体和云伤害。两种实体在所有者失效时清理，且为临时实体。最终生命损失仍受原版和模组伤害事件影响。

## 实际验证

- 用户要求停止测试前，编译通过，2 项现有单元测试通过。最后一轮 Forge GameTest 为 15/16 通过，覆盖新配方、升级、属性和状态效果。
- 龙息用例最后一轮通过实体碰撞、20 点直击和归属断言，在云的脉冲断言失败。已核对 Forge 1.20.1 的 ServerLevel.tickNonPassenger 字节码，确认由该入口递增实体 tickCount。原夹具直接调用云的 tick，导致年龄不前进。夹具已改用 ServerLevel.tickNonPassenger，修正后依用户要求没有重跑。云脉冲、之后的方块碰撞、PVP 与冷却断言，以及客户端和普通专服启动均未完成验证。
- 停止测试后执行 assemble 并成功打包，发行 JAR 包含新弹体、龙息云和所有配方，测试类位于 GameTest source set。

## 文件路径

- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/AnomalousContainmentChronicles.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/DragonGenes.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneCategories.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneEffects.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneMeleeEvents.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/gene/GeneSkillConfig.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/entity/GeneDragonFireball.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/entity/GeneDragonBreathCloud.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/registry/ModEntities.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/ClientScreens.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/GeneClientPackets.java`
- `D:/ModIMade/ACC/src/main/java/com/carrot123/yixiangshouronglu/client/GeneModificationScreen.java`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/en_us.json`
- `D:/ModIMade/ACC/src/main/resources/assets/yixiangshouronglu/lang/zh_cn.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/wither_core.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/wither_core_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/wither_core_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/ender_dragon_brain.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/ender_dragon_brain_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/ender_dragon_brain_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/berserker_tendons.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/berserker_tendons_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/berserker_tendons_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/void_carapace.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/void_carapace_alpha_to_beta.json`
- `D:/ModIMade/ACC/src/main/resources/data/yixiangshouronglu/recipes/gene_upgrades/void_carapace_beta_to_gamma.json`
- `D:/ModIMade/ACC/src/gametest/java/com/carrot123/yixiangshouronglu/gene/DragonGeneRuntimeTests.java`
- `D:/ModIMade/ACC/src/gametest/java/com/carrot123/yixiangshouronglu/genevalidation/GeneUpgradeRuntimeTests.java`
- `D:/ModIMade/ACC/docs/dragon-genes.md`

发行 JAR SHA256 为 `84dee079e12c2f430b1073856fb8b5c8f9eee50305f727d100270e2f73012baf`。
