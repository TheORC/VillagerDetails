package me.gamesareme.villagertrials;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Villager;
import org.bukkit.entity.Zombie;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * This plugin takes away the control of zombie villages from the default game.
 * This allows 100% conversion rate, regardless of the difficulty of the game.
 * 
 * 
 * @author olive
 * @date 01/04/2020
 */
public class VillagerTrials extends JavaPlugin implements Listener {
	
	
	static {
		ConfigurationSerialization.registerClass(VillagerDetails.class, "VillagerDetails");
	}
	

	@Override
	public void onEnable() {
		super.onEnable();
	
		this.LoadConfiguration(); //Initial Load of the Config File
		Bukkit.getPluginManager().registerEvents(this, this);
		
		
		@SuppressWarnings("unchecked")
		List<VillagerDetails> vDetails = (List<VillagerDetails>) this.getConfig().getList("zombies");
		
		if(vDetails == null)
			vDetails = new ArrayList<VillagerDetails>();
		
		for(VillagerDetails details : vDetails) {
			
			List<LivingEntity> worldE = Bukkit.getWorld(details.getLocation().getWorld().getUID()).getLivingEntities();
			
			Boolean found = false;
			
			for(LivingEntity e : worldE) {
				
				if(!(e instanceof ZombieVillager))
					continue;
				
				if(details.getLocation().distance(e.getLocation()) <= 0.5) {
					found = true;
					break;
				}
			}
			
			//This was not found. Remove it from the list
			if(!found)
				vDetails.remove(details);
		}
		
		this.getConfig().set("zombies", vDetails);
		this.zVillagers = vDetails;
		
		this.saveConfig();
		this.reloadConfig();
	}

	@Override
	public void onDisable() {
		super.onDisable();
		this.saveConfig();
	}
	
	private void LoadConfiguration() {
		this.getConfig().options().copyDefaults(true);
		this.saveConfig();
	}
	
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
			Villager villager = (Villager) event.getEntity();
	
			// Check if the villages heath goes under 0
			if (villager.getHealth() - event.getDamage() <= 0) {
				event.setCancelled(true);
	
				villager.setAI(false); // We no longer want the villager to move
	
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

			Villager v = (Villager) event.getEntity();

			//Check to see if you villager is in the list of stored villagers
			for (VillagerDetails vD : zVillagers) {

				// We have our villager!
				if (vD.getLocation().distance(v.getLocation()) <= 0.5) {

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

	

}
