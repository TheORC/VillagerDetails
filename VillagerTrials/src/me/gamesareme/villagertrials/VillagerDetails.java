package me.gamesareme.villagertrials;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.entity.Villager.Profession;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

/**
 * This class is used to store the state of a villager when it becomes zombiefied.
 * This class can be serialized allowing the state to be stored in a .yml
 * file in the event the the server restarts as a zombie is being cured.
 * 
 * @author olive
 * @date 01/04/2020
 *
 */
public class VillagerDetails implements Cloneable, ConfigurationSerializable { 

	private Profession vProffesion;
	private int vExperience;
	private int vLevel;
	private List<MerchantRecipe> recipes;
	private Location loc;

	public Profession getProfession() {
		return this.vProffesion;
	}

	public void setProfession(Profession prof) {
		this.vProffesion = prof;
	}

	public int getExperience() {
		return vExperience;
	}

	public void setExperience(int vExperience) {
		this.vExperience = vExperience;
	}

	public int getLevel() {
		return vLevel;
	}

	public void setLevel(int vLevel) {
		this.vLevel = vLevel;
	}

	public List<MerchantRecipe> getRecipe() {
		return this.recipes;
	}

	public void setRecipes(List<MerchantRecipe> recipes) {
		this.recipes = recipes;
	}

	public Location getLocation() {
		return this.loc;
	}

	public void setLocation(Location l) {
		this.loc = l;
	}

	@Override
	public Map<String, Object> serialize() {
		LinkedHashMap<String, Object> newMap = new LinkedHashMap<>();
		newMap.put("proffesion", this.vProffesion.toString());
		newMap.put("experience", this.vExperience);
		newMap.put("level", this.vLevel);
		newMap.put("location", this.loc);
		
		List<Map<String, Object>> recipes = new ArrayList<Map<String,Object>>();
		for(MerchantRecipe recip : this.recipes) {
			Map<String, Object> newRecipie = new HashMap<String, Object>();
			newRecipie.put("maxuses", recip.getMaxUses());
			newRecipie.put("pricemulti", recip.getPriceMultiplier());
			newRecipie.put("experience", recip.getVillagerExperience());
			newRecipie.put("ingredients", recip.getIngredients());
			newRecipie.put("reward", recip.getResult());
			recipes.add(newRecipie);
		}
		
		newMap.put("recipes", recipes.toArray());
		
		return newMap;
	}
	
	@SuppressWarnings("unchecked")
	public static VillagerDetails deserialize(Map<String, Object> args) {
		
		VillagerDetails newDet = new VillagerDetails();
		
		if(args.containsKey("proffesion"))
			newDet.setProfession(Profession.valueOf((String) args.get("proffesion")));
		
		if(args.containsKey("experience"))
			newDet.setExperience((int) args.get("experience"));
		
		if(args.containsKey("level"))
			newDet.setLevel((int) args.get("level"));
		
		if(args.containsKey("location"))
			newDet.setLocation((Location) args.get("location"));
		
		if(args.containsKey("recipes")) {
			List<Map<String, Object>> recipes = (ArrayList<Map<String, Object>>) args.get("recipes");
			
			List<MerchantRecipe> r = new ArrayList<MerchantRecipe>();
			
			for(Map<String, Object> t : recipes) {
				
				double d = (double) t.get("pricemulti");
				MerchantRecipe nM = new MerchantRecipe((ItemStack)t.get("reward"), 0, (int)t.get("maxuses"), true, (int)t.get("experience"), (float) d);
				
				for(ItemStack stack : (List<ItemStack>)t.get("ingredients")) {
					nM.addIngredient(stack);
				}

				r.add(nM);
			}
			
			newDet.setRecipes(r);
		}
		
		return newDet;
	}
}