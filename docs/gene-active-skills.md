# 三种基因与主动技能扩展

本次沿用 Forge 1.20.1、Java 17、五区域十槽和通用 gene_module；正式定义共四个，旧再生细胞不变。

## 定义与属性

AdditionalGenes.register 集中注册 skeletal_retina、blast_skeleton、ender_synapse。

| ID（均为 yixiangshouronglu） | 区域 / 容量 | 分类 | α / β / γ |
|---|---|---|---|
| skeletal_retina | VISION / 3 | mutant, undead | attributeslib:arrow_damage MULTIPLY_BASE 0.10 / 0.15 / 0.25 |
| blast_skeleton | SKELETAL / 3 | mutant | IS_EXPLOSION 伤害剩余 0.75 / 0.50 / 0.25 |
| ender_synapse | NERVOUS / 5 | mutant | attributeslib:dodge_chance ADDITION 0.02 / 0.04 / 0.06 |

属性修饰器使用 GeneDefinition.effectId(attribute ID) 稳定 UUID。GeneEffects.refresh 核对数值和操作类型，仅更改对应 UUID；分类使用原有并集去重。箭矢伤害由神化属性自身的实体生成监听处理，ACC 不重复乘伤。

GeneDefinition.Effect.explosionReduction 为可选扩展入口，默认0。GeneDamageEvents 在服务端 LivingHurtEvent LOWEST 中仅处理真实 IS_EXPLOSION 标签，各基因按剩余伤害乘法组合，原版护甲、抗性、吸收生命仍在之后执行。

## 配方

正式文件位于 data/yixiangshouronglu/recipes/{skeletal_retina,blast_skeleton,ender_synapse}.json。三者均为 gene_synthesis、SYNTHESIS、shapeless=true、time=200、ALPHA 输出一件。

- 骸骨视网膜：mutantmonsters:mutant_skeleton 提取液4、骨头6、wendigo_eye 1、antlers:wendigo 提取液1。
- 爆裂骨骼：mutantmonsters:mutant_creeper 提取液4、火药8、烈焰粉2、皮革4。
- 末影突触：mutantmonsters:mutant_enderman 提取液4、末影珍珠2、末影之眼1、#yixiangshouronglu:meats 1。

提取液 Ingredient 使用 forge:partial_nbt，EntityId 位于物品根 NBT，额外 NBT 不影响匹配。meats 引用现有 raw_meats，并添加五种熟肉及原版生熟鱼；不含鱼桶、腐肉和加工食品。无新增强化材料配方，GeneModuleItem.upgrade 仍支持 α→β→γ。

GeneTime.seconds 将 tick 除以20后移除末尾零，合成台和 JEI 共用；内部保存和制作时间仍为 tick。空配方右侧不绘制提示。

## 主动技能接口

GeneSkills.register(Skill) 注册 ID、提供基因、名称翻译键和服务端执行入口。执行返回本地化状态后缀，成功为 ok，只有成功才写入冷却。未来技能需提供自身执行逻辑；公共验证、请求节流、技能选择和保存可复用。

当前 ender_teleport 由有效安装的 ender_synapse 提供。单一可重绑按键默认 G；一个可用技能直接请求，多个时打开统一分页选择界面。客户端不提交位置、容量、冷却或结果。

GeneNetwork 协议版本2：原 Snapshot/Action 保留，新增 C2S SkillRequest（仅技能 ID）及 S2C SkillResult。服务端通过连接取得玩家，所有操作在服务器线程执行；仅向本人同步。登录、重生、换维度复用 Capability 快照，不按 tick 广播。

## 配置与保存

服务器世界 serverconfig/yixiangshouronglu-server.toml：

```toml
[geneSkills]
teleportRange = 32
teleportCooldownTicks = 100
```

范围1–128格，冷却1–72000 tick，品质不改变它们。Runtime.SkillCooldowns 以技能完整 ID 保存主世界 gameTime 的 readyAt；Runtime.SkillRequestAfter 保存4 tick请求节流。失败不更新成功冷却，卸载、品质替换、Clone、换维度及重启均保留；停服不按墙钟推进。

## 瞬移安全

射线从服务端真实眼睛和视角发出，逐格检查已加载状态后进行原版方块碰撞，遇未加载区块或未命中直接失败。命中点附近水平2、垂直3格搜索支撑面的碰撞形状；侧面禁止搜索同高度墙后，允许障碍顶部。

候选落点检查完整站立碰撞箱、实体阻挡、支撑、建筑高度、世界边界、配置距离及已加载区块，拒绝液体、火、熔岩、仙人掌、岩浆块、细雪等危险位置。无安全位置时不移动。

服务器拒绝无基因、死亡、旁观、睡眠、骑乘、载客、鞘翅飞行、换维度和工作容器交互状态。成功使用原版玩家连接传送同步，重置 fallDistance，再保存冷却；起终点使用原版末影粒子与音效。

所有基因效果行由 GeneDescriptions 统一本地化并应用金色斜体，不新增模型或注册物品，不恢复共鸣或 TAA。

实际测试、构建与未验证项见 build/ender-validation/report.md。
