package morph.avaritia.tile;

import java.util.List;

import codechicken.lib.packet.PacketCustom;
import codechicken.lib.util.ItemUtils;
import morph.avaritia.recipe.compressor.CompressorManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;

public class TileNeutroniumCompressor extends TileMachineBase implements ISidedInventory {

	//Number of ticks needed to consume an item.
	public static int CONSUME_TICKS = 1;//TODO Config.

	//The consumption progress.
	private int consumption_progress;
	//The production progress.
	public int compression_progress;
	//What we are creating.
	private ItemStack target_stack = ItemStack.EMPTY;
	private int compression_target;

	private ItemStack input = ItemStack.EMPTY;
	private ItemStack output = ItemStack.EMPTY;
	private int ticksSinceLastCheck = 0;
	private boolean is_running = false;

	private NonNullList<ItemStack> c_InputItems = NonNullList.withSize(2, ItemStack.EMPTY);

	private static final int[] top = new int[] {
			0
	};
	private static final int[] sides = new int[] {
			1
	};

	public void setRunning(boolean running) {
		is_running = running;
	}

	public boolean isRunning() {
		return is_running;
	}

	@Override
	public void doWork() {

		boolean dirty = false;

		if (target_stack.isEmpty()) {
			fullContainerSync = true;
			target_stack = CompressorManager.getOutput(input);
			compression_target = CompressorManager.getPrice(target_stack);
		}

		consumption_progress++;
		if (consumption_progress == CONSUME_TICKS) {
			consumption_progress = 0;

			input.shrink(1);
			if (input.getCount() == 0) {
				input = ItemStack.EMPTY;
			}

			compression_progress++;
			dirty = true;
		}

		if (compression_progress >= compression_target) {
			compression_progress = 0;
			if (output.isEmpty()) {
				output = ItemUtils.copyStack(target_stack, 1);
			}
			else {
				output.grow(1);
			}
			dirty = true;
			target_stack = ItemStack.EMPTY;
			fullContainerSync = true;
		}

		if (dirty) {
			markDirty();
		}
	}

	@Override
	protected void onWorkStopped() {
		consumption_progress = 0;
	}

	@Override
	protected boolean canWork() {
		if (input == null) {
			input = ItemStack.EMPTY;
		}
		if (target_stack == null) {
			target_stack = ItemStack.EMPTY;
		}
		if (output == null) {
			output = ItemStack.EMPTY;
		}
		return (input.isEmpty() && target_stack.isEmpty()) || (CompressorManager.isValidInputForOutput(input, target_stack) && (output.isEmpty() || output.getCount() < Math.min(output.getMaxStackSize(), getInventoryStackLimit())));
	}

	@Override
	public void writeGuiData(PacketCustom packet, boolean isFullSync) {
		packet.writeVarInt(consumption_progress);
		packet.writeVarInt(compression_progress);

		if (isFullSync) {
			packet.writeBoolean(!target_stack.isEmpty());
			if (!target_stack.isEmpty()) {
				packet.writeVarInt(compression_target);
				packet.writeItemStack(target_stack);
			}

			List<ItemStack> inputs = CompressorManager.getInputs(target_stack);

			packet.writeInt(inputs.size());
			for (ItemStack input : inputs) {
				packet.writeItemStack(input);
			}
		}
	}

	@Override
	public void readGuiData(PacketCustom packet, boolean isFullSync) {
		consumption_progress = packet.readVarInt();
		compression_progress = packet.readVarInt();
		if (isFullSync) {
			if (packet.readBoolean()) {
				compression_target = packet.readVarInt();
				target_stack = packet.readItemStack();
			}
			else {
				target_stack = ItemStack.EMPTY;
				compression_target = 0;
			}

			NonNullList<ItemStack> inputs = NonNullList.<ItemStack>create();
			int items = packet.readInt();
			for (int i = 0; i < items; i++) {
				ItemStack stack = packet.readItemStack();
				inputs.add(stack);
			}

			c_InputItems = inputs;
		}
	}

	public int getCompressionProgress() {
		return compression_progress;
	}

	public int getCompressionTarget() {
		return compression_target;
	}

	public int getConsumptionProgress() {
		return consumption_progress;
	}

	public int getConsumptionTarget() {
		return CONSUME_TICKS;
	}

	public ItemStack getTargetStack() {
		return target_stack;
	}

	public NonNullList<ItemStack> getInputItems() {
		;
		return c_InputItems;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		input = new ItemStack(tag.getCompoundTag("input"));
		consumption_progress = tag.getInteger("consumption_progress");
		compression_progress = tag.getInteger("compression_progress");
		target_stack = new ItemStack(tag.getCompoundTag("target"));
		//Calc compression target.
		compression_target = CompressorManager.getPrice(target_stack);
		fullContainerSync = true;
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound tag) {
		if (!input.isEmpty()) {
			NBTTagCompound inputTag = new NBTTagCompound();
			input.writeToNBT(inputTag);
			tag.setTag("input", inputTag);
		}
		if (!output.isEmpty()) {
			NBTTagCompound outputTag = new NBTTagCompound();
			output.writeToNBT(outputTag);
			tag.setTag("output", outputTag);
		}
		if (!target_stack.isEmpty()) {
			NBTTagCompound targetTag = new NBTTagCompound();
			target_stack.writeToNBT(targetTag);
			tag.setTag("target", targetTag);
		}
		tag.setInteger("consumption_progress", consumption_progress);
		tag.setInteger("compression_progress", compression_progress);
		return super.writeToNBT(tag);
	}

	@Override
	public int getSizeInventory() {
		return 2;
	}

	@Override
	public ItemStack getStackInSlot(int slot) {
		if (slot == 0) {
			return input;
		}
		else {
			return output;
		}
	}

	@Override
	public ItemStack decrStackSize(int slot, int decrement) {
		if (slot == 0) {
			if (input.isEmpty()) {
				return ItemStack.EMPTY;
			}
			else {
				if (decrement < input.getCount()) {
					ItemStack take = input.splitStack(decrement);
					if (input.getCount() <= 0) {
						input = ItemStack.EMPTY;
					}
					return take;
				}
				else {
					ItemStack take = input;
					input = ItemStack.EMPTY;
					return take;
				}
			}
		}
		else if (slot == 1) {
			if (output.isEmpty()) {
				return ItemStack.EMPTY;
			}
			else {
				if (decrement < output.getCount()) {
					ItemStack take = output.splitStack(decrement);
					if (output.getCount() <= 0) {
						output = ItemStack.EMPTY;
					}
					return take;
				}
				else {
					ItemStack take = output;
					output = ItemStack.EMPTY;
					return take;
				}
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return world.getTileEntity(getPos()) == this && player.getDistanceSq(getPos().getX() + 0.5D, getPos().getY() + 0.5D, getPos().getZ() + 0.5D) <= 64.0D;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (stack.isEmpty()) {
			return false;
		}
		if (slot == 0) {
			if (target_stack.isEmpty()) {
				return true;
			}
			if (CompressorManager.getOutput(stack).isEmpty()) {
				return false;
			}
			if (CompressorManager.getOutput(stack).isItemEqual(target_stack)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public void setInventorySlotContents(int slot, ItemStack stack) {
		if (slot == 0) {
			input = stack;
		}
		else if (slot == 1) {
			output = stack;
		}
	}

	/**
	 * Returns the name of the inventory
	 */
	@Override
	public String getName() {
		return "container.neutronium_compressor";
	}

	/**
	 * Returns if the inventory is named
	 */
	@Override
	public boolean hasCustomName() {
		return false;
	}

	@Override
	public int[] getSlotsForFace(EnumFacing side) {
		if (side == EnumFacing.UP) {
			return top;
		}
		else {
			return sides;
		}
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, EnumFacing side) {
		return isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, EnumFacing side) {
		if (slot == 1 && side != EnumFacing.UP) {
			return true;
		}
		return false;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public void openInventory(EntityPlayer player) {
	}

	@Override
	public void closeInventory(EntityPlayer player) {
	}

	@Override
	public int getField(int id) {
		return 0;
	}

	@Override
	public void setField(int id, int value) {
	}

	@Override
	public int getFieldCount() {
		return 0;
	}

	@Override
	public void clear() {
	}

	@Override
	public boolean isEmpty() {
		return getInputItems().size() == 0;
	}

}
