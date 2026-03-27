# IpacGrowthControl
Minecraft 上的简单植物生长控制插件

https://modrinth.com/project/ipacgrowthcontrol

这是一个针对甘蔗、仙人掌、竹子、藤蔓一类植物的简单控制插件.  
它允许玩家使用剪刀快速切换一株植物的生长能力, 适合需要固定植物高度的建筑、暂时停用自动生长或便于维护机器的场景.

### 使用方法

手持剪刀右键植物, 即可切换该植物的生长能力.
- 向上生长植物: 甘蔗、仙人掌、竹子、海带、缠怨藤.
- 向下生长植物: 藤蔓、垂泪藤、洞穴藤蔓.

插件会自动找到该植物的锚点方块并记录状态.  
玩家处于生存模式或冒险模式时, 每次切换都会消耗 1 点剪刀耐久.  
部分植物可以在切换时播放剪刀声音, 具体列表可在配置中调整.  
再次使用剪刀右键即可恢复生长.  
方块被破坏、放置、爆炸或其他方式改变时, 对应记录会自动清除.  

### 命令

- `/pgc` - 帮助信息
- `/pgc reload` - 重载插件
- `/pgc chunk` - 查看当前区块停止生长的植物
- `/pgc clear` - 恢复当前区块停止生长的植物

### 权限

```yaml
permissions:

  IpacGrowthControl.reload:
    description: '重载插件'
    default: op

  IpacGrowthControl.chunk:
    description: '查看当前区块停止生长的植物'
    default: op

  IpacGrowthControl.clear:
    description: '恢复当前区块停止生长的植物'
    default: op
```

### 配置文件
```yaml

upward-growth-plants:
  - SUGAR_CANE
  - CACTUS
  - BAMBOO
  - KELP
  - KELP_PLANT
  - TWISTING_VINES
  - TWISTING_VINES_PLANT

downward-growth-plants:
  - VINE
  - WEEPING_VINES
  - WEEPING_VINES_PLANT
  - CAVE_VINES
  - CAVE_VINES_PLANT

shear-sound-plants:
  - SUGAR_CANE
  - CACTUS

messages:
  disable-growth: '§bIpacEL §f> §b已停止植物生长'
  enable-growth: '§bIpacEL §f> §a已恢复植物生长'

```
