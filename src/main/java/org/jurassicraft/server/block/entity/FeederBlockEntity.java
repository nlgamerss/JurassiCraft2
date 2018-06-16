package org.jurassicraft.server.block.entity;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.inventory.ItemStackHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import org.jurassicraft.JurassiCraft;
import org.jurassicraft.client.sound.SoundHandler;
import org.jurassicraft.server.block.machine.FeederBlock;
import org.jurassicraft.server.entity.DinosaurEntity;
import org.jurassicraft.server.food.FoodHelper;

import java.util.Random;

public class FeederBlockEntity extends TileEntityLockable implements ITickable, ISidedInventory {
    private static final int[] CARNIVOROUS_SLOTS = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private static final int[] HERBIVOROUS_SLOTS = new int[] { 9, 10, 11, 12, 13, 14, 15, 16, 17 };
    public int prevOpenAnimation;
    public int openAnimation;
    protected String customName;
    private NonNullList<ItemStack> slots = NonNullList.<ItemStack>withSize(18, ItemStack.EMPTY);
    private int stayOpen;
    private boolean open;
    private DinosaurEntity feeding;
    private int feedingExpire;

    @Override
    public Container createContainer(InventoryPlayer inventory, EntityPlayer player) {
        return null;
    }

    @Override
    public String getGuiID() {
        return JurassiCraft.MODID + ":feeder";
    }

    @Override
    public int[] getSlotsForFace(EnumFacing side) {
        return side.getAxis() == EnumFacing.Axis.Y ? CARNIVOROUS_SLOTS : HERBIVOROUS_SLOTS;
    }

    @Override
    public boolean canInsertItem(int index, ItemStack stack, EnumFacing direction) {
        return this.isItemValidForSlot(index, stack);
    }

    @Override
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction) {
        return true;
    }

    @Override
    public int getSizeInventory() {
        return this.slots.size();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        return (ItemStack)this.slots.get(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        return ItemStackHelper.getAndSplit(this.slots, index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        	return ItemStackHelper.getAndRemove(this.slots, index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        this.slots.set(index, stack);
        if (stack != ItemStack.EMPTY && stack.getCount() > this.getInventoryStackLimit()) {
            stack.setCount(this.getInventoryStackLimit());
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return this.world.getTileEntity(this.pos) == this && player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return true;
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
    public boolean receiveClientEvent(int id, int type) {
        if (id == 0) {
            boolean newOpen = type == 1;
            if (newOpen != this.open) {
                this.world.playSound(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5, SoundHandler.FEEDER, SoundCategory.BLOCKS, 1.0F, newOpen ? 1.0F : 0.9F, false);
            }
            this.open = newOpen;
            return true;
        } else {
            return super.receiveClientEvent(id, type);
        }
    }

    @Override
    public void clear() {
        for (int i = 0; i < this.slots.size(); ++i) {
            this.slots.clear();
        }
    }

    @Override
    public String getName() {
        return this.hasCustomName() ? this.customName : "container.feeder";
    }

    @Override
    public boolean hasCustomName() {
        return this.customName != null && this.customName.length() > 0;
    }

    public void setCustomInventoryName(String customName) {
        this.customName = customName;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);

        NBTTagList itemList = compound.getTagList("Items", 10);
        ItemStack[] slots = new ItemStack[this.slots.size()];

        for (int i = 0; i < itemList.tagCount(); ++i) {
            NBTTagCompound item = itemList.getCompoundTagAt(i);
            ItemStack stack = new ItemStack(item);

            byte slot = item.getByte("Slot");

            if (slot >= 0 && slot < slots.length) {
                slots[slot] = stack;
            }
        }

        if (compound.hasKey("CustomName", 8)) {
            this.customName = compound.getString("CustomName");
        }
        
        this.slots = NonNullList.<ItemStack>withSize(this.getSizeInventory(), ItemStack.EMPTY);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        compound = super.writeToNBT(compound);
        

        NBTTagList itemList = new NBTTagList();

        for (int slot = 0; slot < this.getSizeInventory(); ++slot) {
            if (!this.slots.isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) slot);
                super.writeToNBT(compound);
                itemList.appendTag(itemTag);
            }
        }
        compound.setTag("Items", itemList);
        
        if (this.hasCustomName()) {
            compound.setString("CustomName", this.customName);
        }
        
        return compound;
    }

    @Override
    public void update() {
        this.prevOpenAnimation = this.openAnimation;

        if (this.open && this.openAnimation < 20) {
            this.openAnimation++;
        } else if (!this.open && this.openAnimation > 0) {
            this.openAnimation--;
        }
        if (this.open && this.openAnimation == 19) {
            this.stayOpen = 20;
        }
        if (this.feeding != null && (this.feeding.isCarcass() || this.feeding.isDead)) {
            this.feeding = null;
        }
        if (this.feeding != null) {
            if (this.feedingExpire > 0) {
                this.feedingExpire--;
            } else {
                this.feeding = null;
            }
        }
        
        if (this.open && this.openAnimation == 20) {
            if (this.stayOpen > 0) {
                this.stayOpen--;

                if (this.stayOpen == 10 && this.feeding != null) {
                    int feedSlot = this.getFoodForDinosaur(this.feeding);
                    if (feedSlot >= 0) {
                        Random random = new Random();

                        float offsetX = 0.5F;
                        float offsetY = 0.5F;
                        float offsetZ = 0.5F;
                        float motionX = 0.0F;
                        float motionY = 0.0F;
                        float motionZ = 0.0F;

                        switch (this.world.getBlockState(this.pos).getValue(FeederBlock.FACING)) {
                            case UP:
                                offsetY = 1.0F;
                                motionY = 1.0F;
                                motionX = random.nextFloat() - 0.5F;
                                motionZ = random.nextFloat() - 0.5F;
                                break;
                            case DOWN:
                                offsetY = -1.0F;
                                break;
                            case NORTH:
                                offsetZ = -1.0F;
                                motionY = 0.5F;
                                motionZ = -0.5F;
                                break;
                            case SOUTH:
                                offsetZ = 1.0F;
                                motionY = 0.5F;
                                motionZ = 0.5F;
                                break;
                            case WEST:
                                offsetX = -1.0F;
                                motionY = 0.5F;
                                motionX = -0.5F;
                                break;
                            case EAST:
                                offsetX = 1.0F;
                                motionY = 0.5F;
                                motionX = 0.5F;
                                break;
                        }
                        
                        ItemStack stack = this.slots.get(feedSlot);

                        if (stack != ItemStack.EMPTY) {
                            EntityItem itemEntity = new EntityItem(this.world, this.pos.getX() + offsetX, this.pos.getY() + offsetY, this.pos.getZ() + offsetZ, new ItemStack(stack.getItem(), 1, stack.getItemDamage()));
                            itemEntity.setDefaultPickupDelay();
                            itemEntity.motionX = motionX * 0.3F;
                            itemEntity.motionY = motionY * 0.3F;
                            itemEntity.motionZ = motionZ * 0.3F;
                            this.world.spawnEntity(itemEntity);

                            this.decrStackSize(feedSlot, 1);
                            this.feeding.getNavigator().tryMoveToXYZ(itemEntity.posX + motionX, itemEntity.posY + motionY, itemEntity.posZ + motionZ, 0.8);
                        }
                    }

                    this.feeding = null;
                }
            } else if (!this.world.isRemote) {
                this.setOpen(false);
            }
        }
    }

    public void setOpen(boolean open) {
        if (!this.world.isRemote && this.open != open) {
            this.world.addBlockEvent(this.pos, this.getBlockType(), 0, open ? 1 : 0);
        }

        this.open = open;

        if (!open) {
            this.feeding = null;
        }
    }

    public boolean canFeedDinosaur(DinosaurEntity dinosaur) {
        return this.getFoodForDinosaur(dinosaur) != -1;
    }

    private int getFoodForDinosaur(DinosaurEntity dinosaur) {
        int i = 0;
        for (ItemStack stack : this.slots) {
            if (stack != ItemStack.EMPTY && stack.getCount() > 0 && FoodHelper.isEdible(dinosaur, dinosaur.getDinosaur().getDiet(), stack.getItem())) {
                return i;
            }
            i++;
        }
        return -1;
    }

    public DinosaurEntity getFeeding() {
        return this.feeding;
    }

    public void setFeeding(DinosaurEntity feeding) {
        this.feeding = feeding;
        if (this.feeding != null) {
            this.feedingExpire = 400;
        } else {
            this.feedingExpire = 0;
        }
    }

	@Override
	public boolean isEmpty() {
		return false;
	}
}