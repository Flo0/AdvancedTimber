package com.gestankbratwurst.asynctimber.listener;

import java.util.ArrayList;
import java.util.Set;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.gestankbratwurst.asynctimber.AdvancedTimber;
import com.gestankbratwurst.asynctimber.treehandler.TreeHandler;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import net.crytec.api.nbt.NBTItem;

@SuppressWarnings("deprecation")
public class TreeBreakListener implements Listener{
	
	public TreeBreakListener(AdvancedTimber plugin) {
		FileConfiguration config = plugin.getFileManager().getConfig();
		
		this.treeHandler = plugin.getTreeHandler();
		
		this.allowedWorlds = Sets.newHashSet(config.getStringList("Worlds"));
		this.permission = config.getString("Permission", "timber.trigger");
		this.allowCreative = config.getBoolean("AllowCreative", false);
		this.safeDrops = config.getBoolean("Safedrops", false);
		
		this.triggerLores = Sets.newHashSet(config.getStringList("TriggerItems.Lore.contains"));
		this.triggerNames = Sets.newHashSet(config.getStringList("TriggerItems.Name.contains"));
		this.triggerNBTKeys = Sets.newHashSet(config.getStringList("TriggerItems.NBT_Key"));
		this.triggerContains = Sets.newHashSet(config.getStringList("TriggerItems.Material.contains"));
		this.triggerMaterials = Sets.newHashSet(config.getStringList("TriggerItems.Material.exact"));
	}
	
	private final TreeHandler treeHandler;
	
	private final Set<String> allowedWorlds;
	private final String permission;
	private final boolean allowCreative;
	private final boolean safeDrops;
	
	private final Set<String> triggerMaterials;
	private final Set<String> triggerContains;
	private final Set<String> triggerNames;
	private final Set<String> triggerLores;
	private final Set<String> triggerNBTKeys;
	
	@EventHandler
	public void onBreakTrigger(BlockBreakEvent event) {
		String blockMaterial = event.getBlock().getType().toString();
		if(!blockMaterial.contains("LOG") && !blockMaterial.contains("WOOD")) return;
		if(this.treeHandler.isBlockToBreak(event.getBlock())) return;
		Player player = event.getPlayer();
		if(!this.isTriggerPlayer(player)) return;
		ItemStack item = player.getInventory().getItemInMainHand();
		if(!this.isTriggerItem(item)) return;
		if(event.isCancelled()) return;
		this.treeHandler.handleBreak(event);
	}
	
	@EventHandler
	public void onDrop(BlockDropItemEvent event) {
		if(this.safeDrops) {
			if(!this.treeHandler.canDrop(event.getBlock().getLocation())) event.setCancelled(true);
		}
	}
	
	private boolean isTriggerPlayer(Player player) {
		if(player.getGameMode().equals(GameMode.CREATIVE) && !this.allowCreative) return false;
		if(!this.permission.isEmpty() && !player.hasPermission(this.permission)) {
			System.out.println("perm");
			return false;
		}
		if(!this.allowedWorlds.contains(player.getWorld().getName())) return false;
		return true;
	}
	
	private boolean isTriggerItem(final ItemStack item) {
		
		if(item == null || item.getType().equals(Material.AIR)) return false;
		
		final String material = item.getType().toString();
		String name = "";
		final ArrayList<String> lores = Lists.newArrayList();
		Set<String> nbtKeys = Sets.newHashSet();
		
		if(item.hasItemMeta()) {
			
			ItemMeta meta = item.getItemMeta();
			
			if(meta.hasDisplayName()) {
				name = meta.getDisplayName();
			}
			if(meta.hasLore()) {
				lores.addAll(meta.getLore());
			}
			
		}
		
		NBTItem nbt = new NBTItem(item);
		nbtKeys = nbt.getKeys();
		
		if(this.triggerMaterials.contains(material)) return true;
		if(this.triggerContains.stream().anyMatch(cont -> material.contains(cont))) return true;
		if(!name.isEmpty()) {
			
			for(String cont : this.triggerNames) {
				
				if(name.contains(cont)) return true;
				
			}
			
		}
		if(lores.stream().anyMatch(lore -> this.triggerLores.stream().anyMatch(entry -> lore.contains(entry)))) return true;
		if(nbtKeys.stream().anyMatch(key -> this.triggerNBTKeys.stream().anyMatch(entry -> key.contains(entry)))) return true;
		
		return false;
	}
}
