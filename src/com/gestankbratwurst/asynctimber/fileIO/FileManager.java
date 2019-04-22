package com.gestankbratwurst.asynctimber.fileIO;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import com.gestankbratwurst.asynctimber.AdvancedTimber;

import lombok.Getter;

public class FileManager {
	
	public FileManager(AdvancedTimber plugin) {
		this.plugin = plugin;
		this.configFile = new File(plugin.getDataFolder(), "configuration.yml");
		this.metricFile = new File(plugin.getDataFolder(), "metrics.yml");
	}
	
	private final AdvancedTimber plugin;
	private final File metricFile;
	private final File configFile;
	@Getter
	private FileConfiguration config;
	@Getter
	private FileConfiguration metrics;
	
	public void setup() {
		plugin.getDataFolder().mkdir();
		if(!this.configFile.exists()) {
			this.plugin.saveResource("configuration.yml", false);
		}
		if(!this.metricFile.exists()) {
			this.plugin.saveResource("metrics.yml", false);
		}
		
		this.updateConfigEntry(configFile, "DropChance", "100", "DropChance in percent if block should drop when timbering");
		
		this.metrics = YamlConfiguration.loadConfiguration(this.metricFile);
		this.config = YamlConfiguration.loadConfiguration(this.configFile);
	}
	
	public void save() {
		this.metrics.set("treestimbered", this.plugin.getTreeHandler().getTreeMetric());
		try {
			this.metrics.save(this.metricFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//Returns true if write was needed
	@SuppressWarnings("deprecation")
	private boolean updateConfigEntry(File configFile, String key, String value, String... description) {
		FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		
		if(config.contains(key)) return false;
		
		try {
			FileUtils.write(this.configFile, "\n", true);
			for(String infoLine : description) {
				FileUtils.write(this.configFile, "\n#" + infoLine, true);
			}
			
			FileUtils.write(this.configFile, "\n" + key + ": " + value, true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return true;
	}
	
}
