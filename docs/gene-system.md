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

- `GeneService.upgradeCapacity(ServerPlayer)`：逐级提升I–V并刷新同步。
- `GeneService.unlockExtreme(ServerPlayer)`：解锁标记并刷新同步。
- GUI开关通过事务action4处理，只允许V级且解锁；仍超正常容量或存在未知成本时不能关闭。
- `GeneCapability.get(player)`供服务端读取；扩展Runtime变动后调用`GeneService.refresh(player)`同步。不要绕过事务直接修改安装槽。

极限允许额外4点；1–2轻度、3–4重度。本阶段只有状态计算和展示，没有饥饿/生命值惩罚，也无升级物品、升级配方或无成本管理员命令。

## 网络

通道：`yixiangshouronglu:genes`，协议2。改造C2S仅发送菜单ID、能力修订号、操作、背包槽与身体索引；主动技能C2S仅发送技能ID。安装基因ID、容量、目标坐标、冷却和操作结果不从客户端接受。S2C只向所属玩家发送服务端快照与翻译状态码。登录、重生、换维度、打开设备和修改后同步，不每tick广播。

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

注册分类通过GeneCategories.register。有效安装定义的分类集合做并集，奖励不按模块数量或品质重复叠加。Gene.Categories物品数据只是快照，实际效果及Tooltip以定义为准。

品质数值：α每40tick治疗1、过量治疗+0.05；β每30tick治疗1、+0.10；γ每20tick治疗1、+0.15。本次只有α配方。突变固定+20生命/+4攻击，亡灵固定+5生命/生命恢复II。

神化真实Mod ID是attributeslib，必需版本至少1.3.7；attributeslib:overheal默认0，使用ADDITION，不改基础值。该属性依赖神化本身的物理伤害转吸收生命处理，不把普通治疗溢出自动转成护盾。Placebo由神化自身依赖声明要求。

GeneEffects在安装变更、登录、重生和换维度时核对稳定UUID修饰器。tick只做治疗和药水维护，不每tick添加属性。内部计时及归属记录保存在Runtime，不推进安装Revision或每tick同步。治疗调用heal，满血不调用基础治疗。

GeneRegeneration保留25tick相位：初始45，剩余不超过20时加25。跟踪自有实例和保存快照，外部添加事件会交出所有权。较弱有限外部药水保存在原版HiddenEffect中，卸载时恢复剩余状态；同级有限效果接近到期时，使用不改变相位的自有延长期包裹，原效果保留在隐藏链中。更强、无限和来源不明效果不删除。外部合并后的实例按原版规则自然结束，不强行清空。

GeneDescriptions.effects为所有基因生成独立的本地化Component，统一GOLD和ITALIC。具体效果实现Effect.descriptions(quality)，分类追加自身说明；改造舱与合成台预览悬停复用这些行。每项一行，不在翻译内嵌换行堆叠效果。

配方可选shapeless:true，缺省仍为固定槽。无序时slot表示展示位置，完整回溯分配实际槽位后按映射扣料。UPGRADE仍要求主槽，不允许无序强化。再生细胞配方200tick，消耗4僵尸突变体提取液(EntityId=mutantmonsters:mutant_zombie)、8腐肉、8 #yixiangshouronglu:raw_meats、1金苹果。partial_nbt允许无关附加NBT，拒绝其他实体。生肉标签初始仅有牛、猪、鸡、羊、兔，不含鱼、熟肉和腐肉。
