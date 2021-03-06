package exnihiloomnia.registries.barrel.files;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import exnihiloomnia.ENO;
import exnihiloomnia.registries.barrel.BarrelCraftingTrigger;
import exnihiloomnia.registries.barrel.pojos.BarrelCraftingRecipe;
import exnihiloomnia.registries.barrel.pojos.BarrelCraftingRecipeList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BarrelCraftingRecipeLoader {
	public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	public static ArrayList<BarrelCraftingTrigger> entries;
	
	public static List<BarrelCraftingTrigger> load(String path) {
		generateExampleJsonFile(path);
		entries = new ArrayList<>();
		
		File[] files = new File(path).listFiles();
		
		for (File file : files) {
			//Ignore the example file
			if (!file.getName().equals("example.json") && !file.getName().equals("defaults.json")) {
				BarrelCraftingRecipeList list = loadRecipes(file);
				
				if (list != null && !list.getRecipes().isEmpty()) {
					for (BarrelCraftingRecipe recipe : list.getRecipes()) {
						BarrelCraftingTrigger entry = BarrelCraftingTrigger.fromRecipe(recipe);
						
						if (entry != null) {
							entries.add(entry);
						}
					}
				}
			}
		}
		
		return entries;
	}
	
	private static void generateExampleJsonFile(String path) {
		File file = new File(path + "example.json");
		BarrelCraftingRecipeList recipes = null;
		
		if (!file.exists()) {
			ENO.log.info("Attempting to generate example barrel recipe file: '" + file + "'.");
			
			recipes = BarrelCraftingRecipeExample.getExampleRecipeList();
			FileWriter writer;
			
			try {
				file.getParentFile().mkdirs();
				
				writer = new FileWriter(file);
				writer.write(gson.toJson(recipes)); 
				writer.close();
			} 
			catch (Exception e)  {
				ENO.log.error("Failed to write file: '" + file + "'.");
				ENO.log.error(e);
			}  
		}
	}
	
	private static BarrelCraftingRecipeList loadRecipes(File file) {
		BarrelCraftingRecipeList recipes = null;
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file)); 
			
			if (reader.ready()) {
				recipes = gson.fromJson(reader, BarrelCraftingRecipeList.class);
			}
			
			reader.close();
		} 
		catch (Exception e)  {
			ENO.log.error("Failed to read barrel recipe file: '" + file + "'.");
			ENO.log.error(e);
		}  
		
		return recipes;
	}

	public static void dumpRecipes(HashMap<String, BarrelCraftingTrigger> recipes, String path) {
		if (!recipes.isEmpty()) {
			BarrelCraftingRecipeList list = new BarrelCraftingRecipeList();

			for (BarrelCraftingTrigger entry : recipes.values()) {
				list.addRecipe(entry.toRecipe());
			}

			File file = new File(path + "defaults.json");

			ENO.log.info("Attempting to dump Fluid Crafting recipe list: '" + file + "'.");

			FileWriter writer;

			try {
				file.getParentFile().mkdirs();

				writer = new FileWriter(file);
				writer.write(gson.toJson(list));
				writer.close();
			} catch (Exception e) {
				ENO.log.error("Failed to write file: '" + file + "'.");
				ENO.log.error(e);
			}
		}
	}
}
