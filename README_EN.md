# IpacGrowthControl
A simple plant growth control plugin for Minecraft.

https://modrinth.com/project/ipacgrowthcontrol

This is a simple plugin for controlling plants such as sugar cane, cactus, bamboo, and vines.  
It allows players to use shears to quickly toggle the growth ability of a single plant, which is useful for builds that need to keep plants at a fixed height, temporarily pause natural growth, or make machine maintenance easier.

### Usage

Hold shears and right-click a plant to toggle its growth ability.
- Upward-growing plants: sugar cane, cactus, bamboo, kelp, and twisting vines.
- Downward-growing plants: vine, weeping vines, and cave vines.

The plugin automatically finds the anchor block of that plant and stores its state.  
When the player is in Survival or Adventure mode, each toggle consumes 1 point of shears durability.  
Some plants can also play a shearing sound when toggled, and this list can be adjusted in the config.  
Right-click again with shears to re-enable growth.  
The stored state is automatically cleared when the block is broken, replaced, exploded, or changed by other events.  

### Commands

- `/pgc` - Help information
- `/pgc reload` - Reload the plugin
- `/pgc chunk` - View stopped-growth plants in the current chunk
- `/pgc clear` - Re-enable stopped-growth plants in the current chunk

### Permissions

```yaml
permissions:

  IpacGrowthControl.reload:
    description: 'Reload the plugin'
    default: op

  IpacGrowthControl.chunk:
    description: 'View stopped-growth plants in the current chunk'
    default: op

  IpacGrowthControl.clear:
    description: 'Re-enable stopped-growth plants in the current chunk'
    default: op
```

### Configuration File
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
  disable-growth: '§bIpacEL §f> §bPlant growth stopped'
  enable-growth: '§bIpacEL §f> §aPlant growth restored'

```
