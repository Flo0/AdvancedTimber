package com.gestankbratwurst.asynctimber.treehandler;

import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.block.BlockBreakEvent;

import com.gestankbratwurst.asynctimber.AdvancedTimber;
import com.gestankbratwurst.asynctimber.api.TimberEvent;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;

import lombok.Getter;

public class TreeHandler {

	public TreeHandler(AdvancedTimber plugin) {
		this.plugin = plugin;
		this.config = plugin.getFileManager().getConfig();
		this.doDrops = !this.config.getBoolean("NoDrops", false);
		this.timberThread = new TimberThread(this);
		this.noDrops = Sets.newHashSet();
		this.treeMetric = plugin.getFileManager().getMetrics().getInt("treestimbered", 0);
		this.dropChance = config.getDouble("DropChance", 100);
	}

	private final AdvancedTimber plugin;
	private final FileConfiguration config;
	private final boolean doDrops;
	private final LinkedBlockingQueue<Block> blocksToBreak = Queues.newLinkedBlockingQueue();
	private final Set<Location> noDrops;
	private final double dropChance;
	private final TimberThread timberThread;
	@Getter
	private int treeMetric;
	
	
	public void startThread() {
		this.timberThread.start();
	}

	public void removeBlockToBreak(Block block) {
		this.blocksToBreak.remove(block);
	}

	public boolean canDrop(Location dropLoc) {
		return !this.noDrops.contains(dropLoc);
	}

	public void restoreDrop(Location dropLoc) {
		this.noDrops.remove(dropLoc);
	}

	public void stopTimberThread() {
		this.timberThread.stop();
	}

	public void addBlockToBreak(Block block) {
		this.blocksToBreak.add(block);
	}

	public boolean isBlockToBreak(Block block) {
		return blocksToBreak.contains(block);
	}

	public void handleBreak(BlockBreakEvent event) {
		
		TimberingTree tree = new TimberingTree(event.getBlock(), event.getPlayer(), this.plugin, this.config);
		
		TimberEvent timEvent = new TimberEvent(event.getPlayer(), tree);
		Bukkit.getPluginManager().callEvent(timEvent);
		if(timEvent.isCancelled()) return;
		
		if (tree.buildUp()) {
			this.treeMetric++;
		}
	}

	private void breakNext() {
		
		Block nextBlock = this.blocksToBreak.poll();
		
		if (this.doDrops) {
			if(this.dropChance >= ThreadLocalRandom.current().nextDouble(100D)) {
				nextBlock.breakNaturally();
			}else {
				nextBlock.setType(Material.AIR);
			}
		} else {
			nextBlock.setType(Material.AIR);
			this.noDrops.remove(nextBlock.getLocation());
		}
		
	}

	private final class TimberThread implements Runnable {

		public TimberThread(TreeHandler handler) {
			this.handler = handler;
		}

		public void start() {
			this.id = Bukkit.getScheduler().runTaskTimer(plugin, this, 0, checkPeriod).getTaskId();
		}

		public void stop() {
			Bukkit.getScheduler().cancelTask(this.id);
		}

		private final TreeHandler handler;
		private int id;
		private long stopRun;
		private final int maxTime = config.getInt("MillisecondsPerPlaceQueue");
		private final int checkPeriod = config.getInt("CheckPeriod");

		@Override
		public void run() {
			
			this.stopRun = System.currentTimeMillis() + this.maxTime;
			
			while (System.currentTimeMillis() < stopRun && !blocksToBreak.isEmpty()) {
				this.handler.breakNext();
			}
			
		}

	}
}
