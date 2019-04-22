package com.gestankbratwurst.asynctimber.treehandler;

import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

import com.gestankbratwurst.asynctimber.AdvancedTimber;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.Setter;
import net.crytec.api.util.Cuboid;

public class TimberingTree {
	
	public TimberingTree(Block startBlock, Player timberer, AdvancedTimber plugin, FileConfiguration config) {
		this.startBlock = startBlock;
		this.player = timberer;
		this.treeBlocks = Lists.newLinkedList();
		this.handler = plugin.getTreeHandler();
		this.config = config;
		this.maxSize = config.getInt("MaxTreeSize", 256);
		this.fireEvents = config.getBoolean("FireEvents", false);
		this.doDrops = !this.config.getBoolean("NoDrops", false);
		this.checkIfTree = config.getBoolean("CheckIfTree", false);
		this.useCascades = config.getBoolean("UseJumpCascade", false);
		this.cubeRadius = config.getInt("CubeRadius", 2);
		this.checkEnchants = config.getBoolean("CheckEnchantments", false);
		this.durabilityLoss = config.getInt("DurabilityLoss", 1);
		this.smooth = config.getBoolean("SmoothBreak", false);
		this.smoothBlocks = config.getInt("BlocksPerSmoothBreak", 1);
		this.dropChance = config.getDouble("DropChance", 100);
		this.plugin = plugin;
	}
	
	private final FileConfiguration config;
	@Getter @Setter
	private Block startBlock;
	private final Player player;
	private final TreeHandler handler;
	@Getter @Setter
	private int maxSize;
	@Getter @Setter
	private boolean fireEvents;
	@Getter @Setter
	private boolean doDrops;
	private boolean isTree;
	@Getter @Setter
	private boolean checkIfTree;
	@Getter @Setter
	private boolean useCascades;
	@Getter @Setter
	private int cubeRadius;
	@Getter @Setter
	private boolean checkEnchants;
	private int durabilityLoss;
	@Getter @Setter
	private boolean smooth;
	@Getter @Setter
	private int smoothBlocks;
	private final AdvancedTimber plugin;
	@Getter @Setter
	private double dropChance;
	
	private final LinkedList<Block> treeBlocks;
	
	public void addBlockToBreak(Block block) {
		this.treeBlocks.add(block);
	}
	
	protected boolean buildUp() {
		
		if(this.addNextValids(this.startBlock)) {
			this.player.sendMessage(this.config.getString("MessagePrefix") + this.config.getString("TreeTooBigMessage"));
			return false;
		}else {
			
			if(this.checkIfTree && !this.isTree) return false;
			
			int count = 0;
			if(this.smooth) {
				count = this.smoothbreak();
			}else {
				count = this.fastBreak();
			}
			
			ItemStack item = this.player.getInventory().getItemInMainHand();
			if(item != null && !item.getType().equals(Material.AIR)) {
				this.removeDurability(item, count, this.checkEnchants);
			}
			
		}
		
		return true;
		
	}
	
	private int fastBreak() {
		int count = 0;
		
		for(Block breaks : this.treeBlocks) {
			if(this.fireEvents) {
				this.handler.addBlockToBreak(breaks);
				BlockBreakEvent event = new BlockBreakEvent(breaks, player);
				Bukkit.getPluginManager().callEvent(event);
				if(event.isCancelled()) {
					this.handler.removeBlockToBreak(breaks);
				}else {
					count++;
				}
			}else {
				this.handler.addBlockToBreak(breaks);
				count++;
			}
			if(!this.doDrops) {
				this.addDrops(breaks);
			}
		}
		
		return count;
	}
	
	private int smoothbreak() {
		int count = 0;
		if(this.treeBlocks.isEmpty()) return count;
		
		int runs = this.treeBlocks.size() / this.smoothBlocks;
		int rest = this.treeBlocks.size() % this.smoothBlocks;
		
		for(int p = 0; p < runs; p++) {
			
			Bukkit.getScheduler().runTaskLater(plugin, () ->{
				for(int i = 0; i < smoothBlocks; i++) {
					this.breakNext();
				}
			}, p);
			
		}
		Bukkit.getScheduler().runTaskLater(plugin, () ->{
			for(int i = 0; i < rest; i++) {
				this.breakNext();
			}
		}, runs + 1);
		
		return count;
	}
	
	private void breakNext() {
		
		Block nextBlock = this.treeBlocks.poll();
		
		if(this.fireEvents) {
			BlockBreakEvent event = new BlockBreakEvent(nextBlock, player);
			Bukkit.getPluginManager().callEvent(event);
			if(event.isCancelled()) return;
		}
		
		if(this.doDrops) {
			if(this.dropChance >= ThreadLocalRandom.current().nextDouble(100D)) {
				nextBlock.breakNaturally();
			}else {
				nextBlock.setType(Material.AIR);
			}
		}else {
			nextBlock.setType(Material.AIR);
			this.handler.restoreDrop(nextBlock.getLocation());
		}
		
	}
	
	private ItemStack removeDurability(ItemStack item, int count, boolean checkEnchants) {
		int loss = count * this.durabilityLoss;
		ItemMeta meta = item.getItemMeta();
		if(!(meta instanceof Damageable)) return item;
		if(checkEnchants) {
			if(item.hasItemMeta()) {
				if(item.getItemMeta().hasEnchants()) {
					loss = (int) (loss * (1D / (item.getItemMeta().getEnchantLevel(Enchantment.DURABILITY) + 1D)));
				}
			}
		}
		
		Damageable damItem = (Damageable) meta;
		damItem.setDamage(damItem.getDamage() + loss);
		item.setItemMeta(meta);
		return item;
	}
	
	private void addDrops(Block block) {
		PlayerInventory playerInv = this.player.getInventory();
		Collection<ItemStack> items = block.getDrops(playerInv.getItemInMainHand());
		items.forEach(item ->{
			if(ThreadLocalRandom.current().nextDouble(100) <= this.dropChance) {
				playerInv.addItem(item).forEach((index, left) -> player.getWorld().dropItemNaturally(player.getLocation(), left));
			}
		});
	}
	
	private boolean addNextValids(Block block){
		
		if(this.useCascades) {
			Location up = block.getLocation().clone().add(0.5 + cubeRadius, 0.5 + cubeRadius, 0.5 + cubeRadius);
			Location down = block.getLocation().clone().add(0.5 - cubeRadius, 0.5 - cubeRadius, 0.5 - cubeRadius);
			Cuboid cube = new Cuboid(up, down);
			
			for(Block next : cube.getBlocks()) {
				if(this.validate(next)) {
					
					if(this.treeBlocks.size() >= this.maxSize) return true;
					this.addNextValids(next);
					
				}
			}
			
		}else {
			
			for(BlockFace face : BlockFace.values()) {
				Block next = block.getRelative(face);
				if(this.validate(next)) {
					
					if(this.treeBlocks.size() >= this.maxSize) return true;
					this.addNextValids(next);
					
				}
			}
			
		}
		
		return false;
	}
	
	private boolean validate(Block block) {
		String mat = block.getType().toString();
		if(mat.contains("LEAVES")) this.isTree = true;
		if(!mat.contains("LOG") && !mat.contains("WOOD")) return false;
		if(this.handler.isBlockToBreak(block)) return false;
		if(this.treeBlocks.contains(block)) return false;
		this.treeBlocks.add(block);
		
		return true;
	}
}
