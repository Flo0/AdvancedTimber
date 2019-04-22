package com.gestankbratwurst.asynctimber;

import java.util.concurrent.Callable;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import com.gestankbratwurst.asynctimber.fileIO.FileManager;
import com.gestankbratwurst.asynctimber.listener.TreeBreakListener;
import com.gestankbratwurst.asynctimber.metrics.Metrics;
import com.gestankbratwurst.asynctimber.metrics.Metrics.SingleLineChart;
import com.gestankbratwurst.asynctimber.treehandler.TreeHandler;

import lombok.Getter;

public class AdvancedTimber extends JavaPlugin{
	
	public AdvancedTimber() {
		instance = this;
	}
	
	@Getter
	private static AdvancedTimber instance;
	@Getter
	private FileManager fileManager;
	@Getter
	private TreeHandler treeHandler;
	@Getter
	private String prefix;
	
	private Metrics metrics;
	
	public void onEnable() {
		
		this.metrics = new Metrics(this);
		
		this.fileManager = new FileManager(this);
		this.fileManager.setup();
		this.treeHandler = new TreeHandler(this);
		this.prefix = fileManager.getConfig().getString("MessagePrefix");
		
		Bukkit.getPluginManager().registerEvents(new TreeBreakListener(this), this);
		this.treeHandler.startThread();
		
		SingleLineChart lineChart = new SingleLineChart("trees_timbered", new Callable<Integer>() {

			@Override
			public Integer call() throws Exception {
				return treeHandler.getTreeMetric();
			}
			
		});
		
		this.metrics.addCustomChart(lineChart);
	}
	
	public void onDisable() {
		this.fileManager.save();
	}
	
}
