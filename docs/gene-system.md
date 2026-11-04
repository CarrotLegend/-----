# 异常基因系统开发接口

目标：Forge 1.20.1 / Java 17。当前正式定义包含再生细胞、骸骨视网膜、爆裂骨骼、末影突触及各自α合成配方；排斥仍仅显示状态，不施加惩罚。

## 注册定义

在双方都会执行的模组公共初始化阶段、世界与配方加载前调用 `GeneDefinitions.register`。定义不可变，ID不得重复；运行期间不要动态改写定义。强度与成本独立，成本必须为正整数。`conflicts` 双向检查，只需一方声明即可阻止共存。

```java
GeneDefinitions.register(new GeneDefinition(
    ResourceLocation.fromNamespaceAndPath("example", "nerve_module"),
    "gene.example.nerve_module", "gene.example.nerve_module.description",
    GeneTypes.Strength.ZAYIN, GeneTypes.Region.NERVOUS, 4,
    Set.of(), null
));
```

这是开发示例，不会注册到正式游戏。名称与说明在使用方的中英文语言资源中定义。区域固定为NERVOUS×2、VISION×1、CARDIOPULMONARY×2、MUSCLE×3、SKELETAL×2；全局身体索引为0–9，依次映射上述区域。

## 模块与品质

使用 `GeneModuleItem.create(id, quality)` 创建完整模块，不能只给空白gene_module。`GeneModuleItem.upgrade(original, target)`只允许ALPHA→BETA、BETA→GAMMA；返回全新ItemStack，保留自定义名称、Gene.Extension和其他NBT，仅更新品质。未找到定义、版本不支持或强度数据不符时不会安装。

物品结构为 `Gene:{Version:1,Id:"namespace:id",Strength:"ZAYIN",Quality:"ALPHA",Extension:{}}`。品质与强度、区域和成本独立。未知物品数据不执行任何效果；已安装未知模块原样保留，可卸载，恢复有效定义前禁止新增或替换。

## 配方

配方类型及序列化器：`yixiangshouronglu:gene_synthesis`。四个输入索引0–3，按GUI从左到右、从上到下；未声明槽必须为空。数量1–64，时间为正整数tick。

下面内容仅为文档示例，不在正式资源中注册：

```json
{
  "type": "yixiangshouronglu:gene_synthesis",
  "mode": "SYNTHESIS",
  "time": 200,
  "inputs": [
    {"slot": 0, "ingredient": {"item": "minecraft:iron_ingot"}, "count": 2},
    {"slot": 1, "ingredient": {"item": "minecraft:diamond"}, "count": 1}
  ],
  "result": {"gene": "example:nerve_module", "quality": "ALPHA"}
}
```

强化使用 `mode:"UPGRADE"`，主槽0须为一个有效模块；可在输入中指定 `gene`、`quality`。输出ID必须等于主模块ID，输出品质必须是下一品质，否则不会匹配。输出始终为一个模块。Ingredient支持Forge当前版本的标准物品/标签条件。

机器在输出堵塞时暂停；输入身份、NBT、配方或模式变化时重置进度。同物品同NBT的输入数量增加不单独重置。保存库存、模式、进度和配方上下文，恢复时重新检查；定义或配方不存在则安全停止，不消耗输入。漏斗任意面只能插入0–3、取出4。外部自动化应使用ITEM_HANDLER能力，不直接改写内部库存。

## 玩家服务端数据与操作

Capability ID：`yixiangshouronglu:genes`。`PlayerGenes`包含五个独立容器、Capacity、ExtremeUnlocked、ExtremeEnabled、Revision及Runtime。每个槽保存完整原始ItemStack，容量按当前定义重新求和，不保存used缓存。死亡与末地返回通过Clone完整复制，已安装物品不进入死亡背包掉落。

玩家正常操作通过改造舱菜单；服务器验证距离、方块、菜单ID、身份、空鼠标、选中背包快照与修订号后调用事务接口。不要在客户端直接改写Capability当作安装。

服务端管理接口：

- `GeneService.upgradeCapacity(ServerPlayer, long revision)`：校验改造舱会话、修订号和三种背包材料后逐级提升I–V并刷新同步。
- `GeneService.unlockExtreme(ServerPlayer)`：解锁标记并刷新同步。
- GUI开关通过事务action4处理，只允许V级且解锁；仍超正常容量或存在未知成本时不能关闭。
- `GeneCapability.get(player)`供服务端读取；扩展Runtime变动后调用`GeneService.refresh(player)`同步。不要绕过事务直接修改安装槽。

极限允许额外4点；1–2轻度、3–4重度。本阶段只有状态计算和展示，没有饥饿/生命值惩罚，也无升级物品、升级配方或无成本管理员命令。

## 网络

通道：`yixiangshouronglu:genes`，协议3。改造C2S仅发送菜单ID、能力修订号、操作、背包槽与身体索引；动作5为容量升级，其背包槽与身体索引字段不参与结算。主动技能C2S仅发送技能ID。安装基因ID、容量、目标坐标、冷却和操作结果不从客户端接受。S2C只向所属玩家发送服务端快照、下一阶段容量升级材料及注册状态、翻译状态码。登录、重生、换维度、打开设备和修改后同步，不每tick广播。

合成台使用原版菜单槽位与DataSlot同步库存和32位进度；隐藏的预览槽不允许点击、转移或取出。模式为设备共享状态。改造舱身体槽不是普通Slot，选择后通过安装/卸载/替换按钮进行原子移动。

## 未来效果

`GeneDefinition.Effect`提供installed、removed、restored、refreshed四个生命周期入口。`effectId(purpose)`按基因ID与用途生成稳定UUID，后续修饰器应使用该标识并在removed时清理；restored须幂等。

`GeneDisplay`可描述属性名称/数值、主动和被动说明、冷却tick、条件。display只负责显示，不应用效果，不请求网络。效果定义及显示数据需能在公共逻辑侧安全加载，客户端专用类不能出现在公共签名中。

已实现再生细胞的定时治疗、神化过量治疗属性，以及突变/亡灵分类效果。新增三基因、爆炸减伤、主动技能扩展和服务器配置见 [gene-active-skills.md](gene-active-skills.md)。不添加空监听器或每tick全量同步。

## JEI与资源

可选JEI API：15.59.0.212。两个类别为gene_synthesis、gene_upgrade，均以合成台为催化方块。没有正式配方时类别无需出现可制作条目；无JEI也能加载。

两台设备为单方块普通模型。当前贴图是从本版本Minecraft原版素材复制的正式占位资源：深板岩实验仪器、铁质医疗舱、青色面板和绿色指示灯；模块采用原版回响碎片素材占位。不是最终定制美术，没有缺失路径，也未添加模型库。

## 再生细胞与分类

正式基因ID为 `yixiangshouronglu:regenerative_cells`，ZAYIN、心肺、容量4。分类ID为 `yixiangshouronglu:mutant` 和 `yixiangshouronglu:undead`。GeneDefinition新增categories集合，旧构造函数默认空分类。

注册分类通过GeneCategories.register。全身有效安装定义按基因ID去重后分别统计各分类，至少4个同分类基因才激活一次奖励；双分类同时为两个分类各计1，品质不增加计数。GeneCategories.counts及active共用ACTIVATION_COUNT=4；不足4时基因自己的效果仍生效。Gene.Categories物品数据只是快照，实际效果及Tooltip以定义为准。

品质数值：α每40tick治疗1、过量治疗+0.05；β每30tick治疗1、+0.10；γ每20tick治疗1、+0.15。本次只有α配方。突变固定+20生命/+4攻击，亡灵固定+5生命/生命恢复II。

神化真实Mod ID是attributeslib，必需版本至少1.3.7；attributeslib:overheal默认0，使用ADDITION，不改基础值。该属性依赖神化本身的物理伤害转吸收生命处理，不把普通治疗溢出自动转成护盾。Placebo由神化自身依赖声明要求。

GeneEffects在安装变更、登录、重生和换维度时核对稳定UUID修饰器。tick只做治疗和药水维护，不每tick添加属性。内部计时及归属记录保存在Runtime，不推进安装Revision或每tick同步。治疗调用heal，满血不调用基础治疗。

GeneRegeneration保留25tick相位：初始45，剩余不超过20时加25。跟踪自有实例和保存快照，外部添加事件会交出所有权。较弱有限外部药水保存在原版HiddenEffect中，卸载时恢复剩余状态；同级有限效果接近到期时，使用不改变相位的自有延长期包裹，原效果保留在隐藏链中。更强、无限和来源不明效果不删除。外部合并后的实例按原版规则自然结束，不强行清空。

GeneDescriptions.effects为所有基因生成独立的本地化Component，统一GOLD和ITALIC。具体效果实现Effect.descriptions(quality)，分类追加自身说明和四基因门槛；改造舱与合成台预览悬停复用这些行。每项一行，不在翻译内嵌换行堆叠效果。

配方可选shapeless:true，缺省仍为固定槽。无序时slot表示展示位置，完整回溯分配实际槽位后按映射扣料。UPGRADE仍要求主槽，不允许无序强化。再生细胞配方200tick，消耗4僵尸突变体提取液(EntityId=mutantmonsters:mutant_zombie)、8腐肉、8 #yixiangshouronglu:raw_meats、1金苹果。partial_nbt允许无关附加NBT，拒绝其他实体。生肉标签初始仅有牛、猪、鸡、羊、兔，不含鱼、熟肉和腐肉。

## 巨钳外骨骼与 TETH 基因

`ExtendedGenes`注册`clawdian_exoskeleton`（ZAYIN/骨骼/容量2）、`heart_of_dust`（TETH/心肺/5）、`vine_muscles`（TETH/肌肉/4）和`frozen_bone_marrow`（TETH/骨骼/6）。巨钳外骨骼无分类，后三者同时计入突变与亡灵；同类至少4个有效且不同ID的已安装基因才激活分类奖励。

三品质数值依次为：外骨骼护甲2/4/6；沙尘之心生命5/15/25并免疫原版饥饿、中毒；藤蔓肌束基础移速百分比5/10/15、近战缓慢概率10/20/30（缓慢I，80tick）；冰封骨髓护甲3/5/7、近战额外FREEZE伤害4/8/12，无冰冻减伤。

属性通过现有稳定UUID刷新和移除。沙尘之心在刷新时清除已有指定状态，并在MobEffectEvent.Applicable拒绝新状态。GeneMeleeEvents仅收集有效玩家直接近战的LivingDamageEvent，于服务端tick末在主伤害完成后处理；再次检查取消、存活、维度及已安装基因。额外FREEZE来源仅绕过受伤冷却，保留其他原版类型规则，不属于PLAYER_ATTACK，不递归触发近战效果。队列在tick末和服务器停止时清理。

四条初始配方为四槽无序配方，外骨骼200tick，其余600tick。提取液保留EntityId部分NBT；藤蔓肌束使用现有meats标签。Mutant More配方具有mod_loaded条件。JEI从RecipeManager自动读取；没有新增正式强化配方，三品质效果及现有模块升级接口均可用。

分类描述统一拼接为每类一行，保留本地化分隔符、分类名与4基因门槛提示；GeneDescriptions继续统一应用金色斜体。模块NBT版本及已有ID未改变。当前源码原先没有藤蔓肌束或冰封骨髓定义和旧配方，本次按指定ID加入，不需要删除旧配方。

## 改造舱容量升级

GeneCapacityUpgrades集中维护四阶段材料，均按物品注册ID匹配，各消耗1个，忽略耐久和其他NBT差异。仅扫描背包与快捷栏0–35槽。可选物品未注册时保持阶段不可用，不使用空气或替代品。

| 阶段 | 容量 | 三种材料，各1个 |
|---|---|---|
| I→II | 20→25 | cataclysm:amethyst_crab_shell、yixiangshouronglu:wendigo_eye、mutantmonsters:hulk_hammer |
| II→III | 25→30 | minecraft:nether_star、minecraft:dragon_egg、mutantmore:mutant_wither_skeleton_skull |
| III→IV | 30→35 | cataclysm:abyssal_egg、mythsandlegends:tenebrite_shard、theinkarena:soul_core |
| IV→V | 35→40 | deeperdarker:warden_carapace、cataclysm:witherite_ingot、iceandfire:fire_dragon_heart |

改造舱按钮位于(108,237)，164×14。材料与注册可用状态由服务器快照提供，拥有数量取现有菜单同步的主背包与快捷栏；悬停显示当前及下一容量、三项拥有/需求数量，缺料或缺失物品标红。V级禁用，升级结果同时写入菜单状态和客户端动作栏。

动作5复用原有Action消息。NetworkEvent.Context.enqueueWork将请求放到服务端主线程，网络入口校验菜单ID，菜单与服务再次校验有效会话、归属、方块、距离、空鼠标、基因能力及revision。先验证完整材料表和数量，再统一扣除；升级返回失败或抛出异常时恢复涉及物品栈快照。成功使用原PlayerGenes.upgradeCapacity递增等级和revision，刷新库存、属性及所属玩家快照。旧revision重复请求拒绝，且不能跳级。

继续使用原Capacity及Revision NBT字段与已有Clone、登录、跨维度恢复流程，没有新增持久化字段。网络协议3要求客户端和服务器安装相同新版ACC。极限改造与容量上限不变。

普通JUnit可验证材料表、扣料计划、背包索引边界、旧revision、最高级、事务回滚及预览编解码。真实ItemStack、玩家NBT恢复、菜单距离与方块校验、界面缩放及多人交互仍须在Forge游戏环境中验证；普通JUnit未进行Forge事件类转换，不能用vanilla Bootstrap替代真实加载器启动。
