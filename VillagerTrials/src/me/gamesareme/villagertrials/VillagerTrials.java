package me.gamesareme.villagertrials;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This plugin takes away the control of zombie villages from the default game.
 * This allows 100% conversion rate, regardless of the difficulty of the game.
 * 
 *                  .-^-.
 *                .'=^=^='.
 *               /=^=^=^=^=\
 *       .-~-.  :^= HAPPY =^;
 *     .'~~*~~'.|^ EASTER! ^|
 *    /~~*~~~*~~\^=^=^=^=^=^:
 *   :~*~~~*~~~*~;\.-*))`*-,/
 *   |~~~*~~~*~~|/*  ((*   *'.
 *   :~*~~~*~~~*|   *))  *   *\
 *    \~~*~~~*~~| *  ((*   *  /
 *     `.~~*~~.' \  *))  *  .'
 *       `~~~`    '-.((*_.-'
 *
 * 
 * @author olive
 * @date 01/04/2020
 */
public class VillagerTrials extends JavaPlugin implements Listener {
	
	
	static {
		ConfigurationSerialization.registerClass(VillagerDetails.class, "VillagerDetails");
	}
	
	//Used to message the console
	private Logger logger = null;
	

	/**
	 * Run the initial configuration when the plugin first loads
	 */
	
	@SuppressWarnings("unchecked")
	@Override
	public void onEnable() {
		super.onEnable();
		
		this.logger = Bukkit.getLogger();
	
		this.LoadConfiguration(); //Create the config if it does not exist
		Bukkit.getPluginManager().registerEvents(this, this);
		
		//Get the config file.
		FileConfiguration config = this.getConfig();
		
		//Retrieve all current zombie villagers from the list
		this.zVillagers = (List<VillagerDetails>) config.getList("zombies");
		
		//Handle first initiation errors.
		if(zVillagers == null)
			zVillagers = new ArrayList<VillagerDetails>();
		
		//Save the config
		this.saveConfig();
	}

	/**
	 * Run all end of server requirements
	 */
	@Override
	public void onDisable() {
		super.onDisable();
		this.saveConfig();
	}
	
	/**
	 *  Method which creates a configuration file if it does not exist.
	 */
	private void LoadConfiguration() {
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
	}
	
	/**
	 *  Method for reloading the configuration file.
	 */
	private void ReloadConfiguration() {
		this.reloadConfig();
	}

	/**
	 * List containing information on zombified villages
	 */
	private List<VillagerDetails> zVillagers = new ArrayList<>();


	/**
	 * 
	 * Detects when a villager dies due to a zombie. Saves the state of the villager
	 * and creates a zombie in it's place.
	 * 
	 * @param event
	 */
	@EventHandler
	public void onEntityDamagedByEntity(EntityDamageByEntityEvent event) {

		//Ignore non villager creatures
		if (!(event.getEntity() instanceof Villager))
			return;

		//Check to see if a zombie is the cause of the damage
		if(event.getDamager() instanceof Zombie || event.getDamager() instanceof ZombieVillager)
		{
			//Get the villager in question
			Villager villager = (Villager) event.getEntity();
	
			// Check if the villages heath goes under 0
			if (villager.getHealth() - event.getDamage() <= 0) {
					
				event.setCancelled(true); //Cancel the death event
	
				/**
				 * Store all of the villages details
				 */
				VillagerDetails vDetails = new VillagerDetails();
				vDetails.setProfession(villager.getProfession());
				vDetails.setLocation(villager.getLocation());
				vDetails.setRecipes(villager.getRecipes());
				vDetails.setExperience(villager.getVillagerExperience());
				vDetails.setLevel(villager.getVillagerLevel());
				
				
				zVillagers.add(vDetails); // Add the the villager to the current list		
				
				/**
				 * Save the villages trades in the config, in case the server crashes
				 */
				this.getConfig().set("zombies", zVillagers);
				this.saveConfig();
				this.ReloadConfiguration();
				
	
				/**
				 * Create a zombie in the villagers place
				 */
				ZombieVillager newEntity = (ZombieVillager) Bukkit.getWorld(villager.getWorld().getUID()).spawnEntity(vDetails.getLocation(), EntityType.ZOMBIE_VILLAGER);
				newEntity.setVillagerProfession(villager.getProfession());
				newEntity.setAI(false);
				newEntity.setCollidable(false);
				newEntity.setGravity(false);
				newEntity.setBaby(false);   //I hate this one.  No we don't want them to become a baby XD
					
				//Send the console a message
				this.SendMessage("Stored villager details.");
					
				/**
				 * Ensure the villager does die
				 */
				villager.setHealth(0);
			}
		}
	}

	/**
	 * Used to check when a create spawns.
	 * We are interested in when a villager spawns because of being cured
	 * @param event
	 */
	@EventHandler
	public void onCreateSpawnEvent(CreatureSpawnEvent event) {

		//Ensure we only handle cure events
		if (event.getSpawnReason() == SpawnReason.CURED) {

			//Get the villager which just spawned
			Villager v = (Villager) event.getEntity();

			//Check to see if you villager is in the list of stored villagers
			for (VillagerDetails vD : zVillagers) {

				// Search in a 2 block radius for the villager
				if (vD.getLocation().distance(v.getLocation()) <= 2.0) {
					
					this.SendMessage("Villager details restored!");
					
					/**
					 * Set the details of the new villager to that of the old one
					 */
					v.setProfession(vD.getProfession());
					v.setVillagerExperience(vD.getExperience());
					v.setVillagerLevel(vD.getLevel());
					v.setRecipes(vD.getRecipe());
					v.setAI(true); //Make so it can walk again

					//We no longer need to watch this villager
					zVillagers.remove(vD);
					
					this.getConfig().set("zombies", zVillagers);
					this.saveConfig();
					this.reloadConfig();
					
					break; //Finished
				}

			}
		}
	}
	
	/**
	 *  This method checks when a zombie villager dies.
	 *  When a zombie villager dies we do not want to continue storing it's information
	 *  
	 * @param event
	 */
	@EventHandler
	public void onCreateDeathEvent(EntityDeathEvent event) {
		
		//We only care for zombie villagers
		if(event.getEntityType() != EntityType.ZOMBIE_VILLAGER)
			return;
		
		//Our villager died...  Sad
		for(VillagerDetails details : zVillagers) {
			if(event.getEntity().getLocation().distance(details.getLocation()) <= 2.0) {
				
				this.SendMessage("A zombie with stored information was just killed!!!");
				
				zVillagers.remove(details);
				
				this.getConfig().set("zombies", zVillagers);
				this.saveConfig();
				
				return;
			}
		}
	}
	
	/**
	 * Method used to send a message to the console.
	 * @param message
	 */
	public void SendMessage(String message) {
		this.logger.info("[VillagerTrials] " + message);
	}

}
