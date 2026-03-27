package io.github.aplini.IpacGrowthControl;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

import java.util.*;

public final class IpacGrowthControl extends JavaPlugin implements Listener {

    private static final byte LOCKED_FLAG = 1;
    private static final String KEY_PREFIX = "igc_";
    private static final String DEFAULT_DISABLE_MESSAGE = "已禁用该植物的生长";
    private static final String DEFAULT_ENABLE_MESSAGE = "已恢复该植物的生长";

    private Set<Material> upwardGrowthPlants;
    private Set<Material> downwardGrowthPlants;
    private Set<Material> shearSoundPlants;
    private String disableMessage;
    private String enableMessage;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadPlantMaterials();
        getServer().getPluginManager().registerEvents(this, this);

        // 注册命令
        PluginCommand command = Objects.requireNonNull(getCommand("igc"));
        command.setExecutor(this);
        command.setTabCompleter(this);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SHEARS) {
            return;
        }

        Block clickedBlock = event.getClickedBlock();
        if (clickedBlock == null) {
            return;
        }

        GrowthDirection growthDirection = getGrowthDirection(clickedBlock.getType());
        if (growthDirection == null) {
            return;
        }

        Block anchorBlock = getAnchorBlock(clickedBlock, growthDirection);
        boolean locked = hasLock(anchorBlock);
        setLock(anchorBlock, !locked);

        Player player = event.getPlayer();
        if (locked) {
            player.sendMessage(enableMessage);
        } else {
            player.sendMessage(disableMessage);
        }

        if (player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE) {
            player.getInventory().setItemInMainHand(item.damage(1, player));
        }

        if (shearSoundPlants.contains(clickedBlock.getType())) {
            player.playSound(clickedBlock.getLocation(), Sound.ENTITY_SHEEP_SHEAR, 1.0f, 1.0f);
        }

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        GrowthDirection growthDirection = getGrowthDirection(event.getNewState().getType());
        if (growthDirection == null) {
            return;
        }

        Block growthSource = event.getBlock().getRelative(growthDirection.sourceDirection());
        if (!isTrackedPlant(growthSource.getType(), growthDirection)) {
            return;
        }

        if (hasLockedBlockInColumn(growthSource, growthDirection)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        GrowthDirection growthDirection = getGrowthDirection(event.getNewState().getType());
        if (growthDirection == null) {
            return;
        }

        Block growthSource = event.getSource();
        if (!isTrackedPlant(growthSource.getType(), growthDirection)) {
            return;
        }

        if (hasLockedBlockInColumn(growthSource, growthDirection)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        clearLock(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        clearLock(event.getBlockPlaced());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        clearLock(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        clearLock(event.getBlock());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        clearLocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        clearLocks(event.blockList());
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        clearLock(event.getBlock());
    }

    private void reloadPlantMaterials() {
        reloadConfig();
        upwardGrowthPlants = parseMaterials(getConfig().getStringList("upward-growth-plants"), "upward-growth-plants");
        downwardGrowthPlants = parseMaterials(getConfig().getStringList("downward-growth-plants"), "downward-growth-plants");
        shearSoundPlants = parseMaterials(getConfig().getStringList("shear-sound-plants"), "shear-sound-plants");
        disableMessage = getConfig().getString("messages.disable-growth", DEFAULT_DISABLE_MESSAGE);
        enableMessage = getConfig().getString("messages.enable-growth", DEFAULT_ENABLE_MESSAGE);
    }

    private Set<Material> parseMaterials(List<String> values, String configKey) {
        Set<Material> materials = EnumSet.noneOf(Material.class);
        for (String value : values) {
            try {
                materials.add(Material.valueOf(value.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                getLogger().warning(configKey + " 包含无效材料: " + value);
            }
        }
        return materials;
    }

    private GrowthDirection getGrowthDirection(Material material) {
        if (upwardGrowthPlants.contains(material)) {
            return GrowthDirection.UPWARD;
        }
        if (downwardGrowthPlants.contains(material)) {
            return GrowthDirection.DOWNWARD;
        }
        return null;
    }

    private boolean isTrackedPlant(Material material, GrowthDirection growthDirection) {
        return getMaterials(growthDirection).contains(material);
    }

    private Set<Material> getMaterials(GrowthDirection growthDirection) {
        if (growthDirection == GrowthDirection.UPWARD) {
            return upwardGrowthPlants;
        }
        return downwardGrowthPlants;
    }

    private boolean hasLockedBlockInColumn(Block origin, GrowthDirection growthDirection) {
        Block cursor = origin;
        while (isTrackedPlant(cursor.getType(), growthDirection)) {
            if (hasLock(cursor)) {
                return true;
            }
            cursor = cursor.getRelative(BlockFace.UP);
        }

        cursor = origin.getRelative(BlockFace.DOWN);
        while (isTrackedPlant(cursor.getType(), growthDirection)) {
            if (hasLock(cursor)) {
                return true;
            }
            cursor = cursor.getRelative(BlockFace.DOWN);
        }
        return false;
    }

    private Block getAnchorBlock(Block origin, GrowthDirection growthDirection) {
        Block cursor = origin;
        Block next = cursor.getRelative(growthDirection.anchorDirection());
        while (isTrackedPlant(next.getType(), growthDirection)) {
            cursor = next;
            next = cursor.getRelative(growthDirection.anchorDirection());
        }
        return cursor;
    }

    private boolean hasLock(Block block) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        return container.has(getBlockKey(block), PersistentDataType.BYTE);
    }

    private void clearLocks(List<Block> blocks) {
        for (Block block : blocks) {
            clearLock(block);
        }
    }

    private void clearLock(Block block) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = getBlockKey(block);
        if (container.has(key, PersistentDataType.BYTE)) {
            container.remove(key);
        }
    }

    private void setLock(Block block, boolean locked) {
        PersistentDataContainer container = block.getChunk().getPersistentDataContainer();
        NamespacedKey key = getBlockKey(block);
        if (locked) {
            container.set(key, PersistentDataType.BYTE, LOCKED_FLAG);
            return;
        }
        clearLock(block);
    }

    private NamespacedKey getBlockKey(Block block) {
        return new NamespacedKey(this, KEY_PREFIX + block.getX() + "_" + block.getY() + "_" + block.getZ());
    }

    private enum GrowthDirection {
        UPWARD(BlockFace.DOWN, BlockFace.DOWN),
        DOWNWARD(BlockFace.UP, BlockFace.UP);

        private final BlockFace sourceDirection;
        private final BlockFace anchorDirection;

        GrowthDirection(BlockFace sourceDirection, BlockFace anchorDirection) {
            this.sourceDirection = sourceDirection;
            this.anchorDirection = anchorDirection;
        }

        public BlockFace sourceDirection() {
            return sourceDirection;
        }

        public BlockFace anchorDirection() {
            return anchorDirection;
        }
    }


    @Override // 指令补全
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if(args.length == 1){
            List<String> list = new ArrayList<>();
            addTabComplete(list, args[0], "reload");
            addTabComplete(list, args[0], "chunk");
            addTabComplete(list, args[0], "clear");
            return list;
        }
        return null;
    }
    @Override // 执行指令
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args){

        // 默认输出插件信息
        if(args.length == 0){
            sender.sendMessage(
                    "\n"+
                            "IpacEL > IpacGrowthControl: 植物生长控制\n"+
                            "  指令:\n"+
                            "    - /pgc reload      - 重载配置\n"+
                            "    - /pgc chunk       - 列出当前区块的禁用列表\n"+
                            "    - /pgc clear       - 清除当前区块所有禁用配置\n"
            );
            return true;
        }

        // 重载配置
        else if(args[0].equals("reload")){
            if(!sender.hasPermission("IpacGrowthControl.reload")){
                sender.sendMessage("[IpacGrowthControl] 你没有权限使用此命令");
                return true;
            }

            reloadPlantMaterials();
            sender.sendMessage("[IpacGrowthControl] 已完成重载");
            return true;
        }

        else if(args[0].equals("chunk")){
            if(!sender.hasPermission("IpacGrowthControl.chunk")){
                sender.sendMessage("[IpacGrowthControl] 你没有权限使用此命令");
                return true;
            }

            if(!(sender instanceof Player player)){
                sender.sendMessage("[IpacGrowthControl] 该命令只能由玩家执行");
                return true;
            }

            List<LockedPlantEntry> lockedPlants = getLockedPlantsInChunk(player.getChunk());
            if(lockedPlants.isEmpty()){
                sender.sendMessage("[IpacGrowthControl] 当前区块没有停止生长的植物");
                return true;
            }

            sender.sendMessage("[IpacGrowthControl] 当前区块停止生长的植物:");
            for(LockedPlantEntry lockedPlant : lockedPlants){
                player.sendMessage(createLockedPlantMessage(lockedPlant));
            }
            return true;
        }

        else if(args[0].equals("clear")){
            if(!sender.hasPermission("IpacGrowthControl.clear")){
                sender.sendMessage("[IpacGrowthControl] 你没有权限使用此命令");
                return true;
            }

            if(!(sender instanceof Player player)){
                sender.sendMessage("[IpacGrowthControl] 该命令只能由玩家执行");
                return true;
            }

            int removedCount = clearLockedPlantsInChunk(player.getChunk());
            sender.sendMessage("[IpacGrowthControl] 已恢复当前区块 " + removedCount + " 个停止生长的植物");
            return true;
        }

        // 返回 false 时, 玩家将收到命令不存在的错误
        return false;
    }

    private void addTabComplete(List<String> list, String input, String value){
        if(value.startsWith(input.toLowerCase(Locale.ROOT))){
            list.add(value);
        }
    }

    private List<LockedPlantEntry> getLockedPlantsInChunk(Chunk chunk){
        List<LockedPlantEntry> lockedPlants = new ArrayList<>();
        for(NamespacedKey key : chunk.getPersistentDataContainer().getKeys()){
            if(!isLockKey(key)){
                continue;
            }

            LockedPlantEntry lockedPlant = parseLockedPlant(chunk, key);
            if(lockedPlant != null){
                lockedPlants.add(lockedPlant);
            }
        }
        return lockedPlants;
    }

    private int clearLockedPlantsInChunk(Chunk chunk){
        List<NamespacedKey> keysToRemove = new ArrayList<>();
        for(NamespacedKey key : chunk.getPersistentDataContainer().getKeys()){
            if(isLockKey(key)){
                keysToRemove.add(key);
            }
        }

        PersistentDataContainer container = chunk.getPersistentDataContainer();
        for(NamespacedKey key : keysToRemove){
            container.remove(key);
        }
        return keysToRemove.size();
    }

    private boolean isLockKey(NamespacedKey key){
        return key.getNamespace().equals(getName().toLowerCase(Locale.ROOT)) && key.getKey().startsWith(KEY_PREFIX);
    }

    private LockedPlantEntry parseLockedPlant(Chunk chunk, NamespacedKey key){
        String rawKey = key.getKey();
        if(!rawKey.startsWith(KEY_PREFIX)){
            return null;
        }

        String[] parts = rawKey.substring(KEY_PREFIX.length()).split("_");
        if(parts.length != 3){
            return null;
        }

        try {
            int x = Integer.parseInt(parts[0]);
            int y = Integer.parseInt(parts[1]);
            int z = Integer.parseInt(parts[2]);
            Material material = chunk.getWorld().getBlockAt(x, y, z).getType();
            return new LockedPlantEntry(x, y, z, material);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Component createLockedPlantMessage(LockedPlantEntry lockedPlant){
        String command = "/tp " + lockedPlant.x() + " " + lockedPlant.y() + " " + lockedPlant.z();
        return Component.text("  - [" + lockedPlant.x() + "/" + lockedPlant.y() + "/" + lockedPlant.z() + "] ")
                .append(Component.translatable(lockedPlant.material()))
                .clickEvent(ClickEvent.runCommand(command))
                .hoverEvent(HoverEvent.showText(Component.text(command)));
    }

    private record LockedPlantEntry(int x, int y, int z, Material material) {
    }
}
