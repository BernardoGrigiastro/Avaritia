package morph.avaritia.recipe.compressor;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

public class CompressorManager {

	private static ArrayList<CompressorRecipe> recipes = new ArrayList<>();

	public static void addRecipe(ItemStack output, int amount, ItemStack input) {
		recipes.add(new CompressorRecipe(output, amount, input));
	}

	public static void addOreRecipe(ItemStack output, int amount, String ore) {
		recipes.add(new CompressOreRecipe(output, amount, ore));
	}

	public static boolean isValidInput(ItemStack input) {
		if (!input.isEmpty()) {
			for (CompressorRecipe recipe : recipes) {
				if (recipe.isValidInput(input)) {
					return true;
				}
			}
		}
		return false;
	}

	public static boolean isValidInputForOutput(ItemStack input, ItemStack output) {
		if (!input.isEmpty()) {
			for (CompressorRecipe recipe : recipes) {
				if (recipe.isValidInput(input)) {
					if (!output.isEmpty()) {
						if (!recipe.getOutput().isItemEqual(output)) {
							continue;
						}
					}
					return true;
				}
			}
		}
		return false;
	}

	public static ItemStack getOutput(ItemStack input) {
		if (!input.isEmpty()) {
			for (CompressorRecipe recipe : recipes) {
				if (recipe.isValidInput(input)) {
					return recipe.getOutput();
				}
			}
		}
		return ItemStack.EMPTY;
	}

	public static List<ItemStack> getInputs(ItemStack output) {
		if (!output.isEmpty()) {
			for (CompressorRecipe recipe : recipes) {
				if (recipe.getOutput().isItemEqual(output)) {
					return recipe.getInputs();
				}
			}
		}
		return new ArrayList<>();
	}

	public static int getCost(ItemStack input) {
		if (!input.isEmpty()) {
			for (CompressorRecipe recipe : recipes) {
				if (recipe.isValidInput(input)) {
					return recipe.getCost();
				}
			}
		}
		return 0;
	}

	public static int getPrice(ItemStack output) {
		if (!output.isEmpty()) {
			for (CompressorRecipe recipe : recipes) {
				if (recipe.getOutput().isItemEqual(output)) {
					return recipe.getCost();
				}
			}
		}
		return 0;
	}

	public static String getName(ItemStack input) {
		for (CompressorRecipe recipe : recipes) {
			if (recipe.isValidInput(input)) {
				return recipe.getIngredientName();
			}
		}
		return "";
	}

	public static List<CompressorRecipe> getRecipes() {
		return recipes;
	}

}
