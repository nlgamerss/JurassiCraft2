package net.timeless.jurassicraft.common.tileentity;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S35PacketUpdateTileEntity;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.tileentity.TileEntityLockable;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.timeless.jurassicraft.JurassiCraft;
import net.timeless.jurassicraft.common.container.ContainerEmbryonicMachine;
import net.timeless.jurassicraft.common.item.ItemDNA;
import net.timeless.jurassicraft.common.item.JCItemRegistry;

public class TileEmbryonicMachine extends TileEntityLockable implements IUpdatePlayerListBox, ISidedInventory
{
    private static final int[] slotsTop = new int[] { 0, 1, 2 }; //input
    private static final int[] slotsBottom = new int[] { 6, 5, 4, 3 }; //output
    private static final int[] slotsSides = new int[] {};

    /** The ItemStacks that hold the items currently being used in the embryonic machine */
    private ItemStack[] slots = new ItemStack[7];

    private int embryoTime;
    private int totalEmbryoTime;

    private String customName;

    /**
     * Returns the number of slots in the inventory.
     */
    public int getSizeInventory()
    {
        return this.slots.length;
    }

    /**
     * Returns the stack in slot i
     */
    public ItemStack getStackInSlot(int index)
    {
        return this.slots[index];
    }

    /**
     * Removes from an inventory slot (first arg) up to a specified number (second arg) of items and returns them in a
     * new stack.
     */
    public ItemStack decrStackSize(int index, int count)
    {
        if (this.slots[index] != null)
        {
            ItemStack itemstack;

            if (this.slots[index].stackSize <= count)
            {
                itemstack = this.slots[index];
                this.slots[index] = null;
                return itemstack;
            }
            else
            {
                itemstack = this.slots[index].splitStack(count);

                if (this.slots[index].stackSize == 0)
                {
                    this.slots[index] = null;
                }

                return itemstack;
            }
        }
        else
        {
            return null;
        }
    }

    /**
     * When some containers are closed they call this on each slot, then drop whatever it returns as an EntityItem -
     * like when you close a workbench GUI.
     */
    public ItemStack getStackInSlotOnClosing(int index)
    {
        if (this.slots[index] != null)
        {
            ItemStack itemstack = this.slots[index];
            this.slots[index] = null;
            return itemstack;
        }
        else
        {
            return null;
        }
    }

    /**
     * Sets the given item stack to the specified slot in the inventory (can be crafting or armor sections).
     */
    public void setInventorySlotContents(int index, ItemStack stack)
    {
        boolean flag = stack != null && stack.isItemEqual(this.slots[index]) && ItemStack.areItemStackTagsEqual(stack, this.slots[index]);
        this.slots[index] = stack;

        if (stack != null && stack.stackSize > this.getInventoryStackLimit())
        {
            stack.stackSize = this.getInventoryStackLimit();
        }

        if (index == 0 && !flag)
        {
            this.totalEmbryoTime = this.getStackEmbryoTime(stack);
            this.embryoTime = 0;
            worldObj.markBlockForUpdate(pos);
        }
    }

    /**
     * Gets the name of this command sender (usually username, but possibly "Rcon")
     */
    public String getName()
    {
        return this.hasCustomName() ? this.customName : "container.embryonic_machine";
    }

    /**
     * Returns true if this thing is named
     */
    public boolean hasCustomName()
    {
        return this.customName != null && this.customName.length() > 0;
    }

    public void setCustomInventoryName(String customName)
    {
        this.customName = customName;
    }

    public void readFromNBT(NBTTagCompound compound)
    {
        super.readFromNBT(compound);

        NBTTagList itemList = compound.getTagList("Items", 10);
        this.slots = new ItemStack[this.getSizeInventory()];

        for (int i = 0; i < itemList.tagCount(); ++i)
        {
            NBTTagCompound item = itemList.getCompoundTagAt(i);

            byte slot = item.getByte("Slot");

            if (slot >= 0 && slot < this.slots.length)
            {
                this.slots[slot] = ItemStack.loadItemStackFromNBT(item);
            }
        }

        this.embryoTime = compound.getShort("EmbryoTime");
        this.totalEmbryoTime = compound.getShort("EmbryoTimeTotal");

        if (compound.hasKey("CustomName", 8))
        {
            this.customName = compound.getString("CustomName");
        }
    }

    public void writeToNBT(NBTTagCompound compound)
    {
        super.writeToNBT(compound);
        compound.setShort("EmbryoTime", (short) this.embryoTime);
        compound.setShort("EmbryoTimeTotal", (short) this.totalEmbryoTime);
        NBTTagList itemList = new NBTTagList();

        for (int slot = 0; slot < this.slots.length; ++slot)
        {
            if (this.slots[slot] != null)
            {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setByte("Slot", (byte) slot);

                this.slots[slot].writeToNBT(itemTag);
                itemList.appendTag(itemTag);
            }
        }

        compound.setTag("Items", itemList);

        if (this.hasCustomName())
        {
            compound.setString("CustomName", this.customName);
        }
    }

    /**
     * Returns the maximum stack size for a inventory slot. Seems to always be 64, possibly will be extended. *Isn't
     * this more of a set than a get?*
     */
    public int getInventoryStackLimit()
    {
        return 64;
    }

    public boolean isInserting()
    {
        return this.embryoTime > 0;
    }

    @SideOnly(Side.CLIENT)
    public static boolean isInserting(IInventory inventory)
    {
        return inventory.getField(0) > 0;
    }

    /**
     * Updates the JList with a new model.
     */
    public void update()
    {
        boolean inserting = this.isInserting();
        boolean sync = false;

        if (!this.worldObj.isRemote)
        {
            if (!this.isInserting() && (this.slots[0] == null))
            {
                if (!this.isInserting() && this.embryoTime > 0)
                {
                    this.embryoTime = MathHelper.clamp_int(this.embryoTime - 2, 0, this.totalEmbryoTime);
                }
            }
            else
            {
                if (this.canInsert())
                {
                    ++this.embryoTime;

                    if (this.embryoTime == this.totalEmbryoTime)
                    {
                        this.embryoTime = 0;
                        this.totalEmbryoTime = this.getStackEmbryoTime(this.slots[0]);
                        this.ouputItem();
                        sync = true;
                    }
                }
                else
                {
                    this.embryoTime = 0;
                    sync = true;
                }
            }

            if (inserting != this.isInserting())
            {
                sync = true;
            }
        }
        else
        {
            if (this.canInsert())
            {
                ++this.embryoTime;
            }
        }

        if (sync)
        {
            worldObj.markBlockForUpdate(pos);
        }
    }

    public int getStackEmbryoTime(ItemStack stack)
    {
        return 200;
    }

    /**
     * Returns true if the embryonic machine can smelt an item, i.e. has a source item, destination stack isn't full, etc.
     */
    private boolean canInsert()
    {
        if (this.slots[0] != null && this.slots[0].getItem() instanceof ItemDNA && this.slots[1] != null && this.slots[1].getItem() == JCItemRegistry.petri_dish && this.slots[2] != null && this.slots[2].getItem() == JCItemRegistry.empty_syringe)
        {
            ItemStack output = new ItemStack(JCItemRegistry.syringe, 1, slots[0].getItemDamage());
            output.setTagCompound(slots[0].getTagCompound());

            for (int i = 3; i < 7; i++)
            {
                if (this.slots[i] == null || ItemStack.areItemsEqual(this.slots[i], output) && ItemStack.areItemStackTagsEqual(this.slots[i], output))
                    return true;
            }
        }

        return false;
    }

    /**
     * Turn one item from the embryonic machine source stack into the appropriate grinded item in the embryonic machine result stack
     */
    public void ouputItem()
    {
        if (this.canInsert())
        {
            ItemStack output = new ItemStack(JCItemRegistry.syringe, 1, slots[0].getItemDamage());
            output.setTagCompound(slots[0].getTagCompound());

            int emptySlot = -1;

            for (int i = 3; i < 7; i++)
            {
                if (this.slots[i] == null || (ItemStack.areItemStackTagsEqual(this.slots[i], output) && this.slots[i].getItem() == output.getItem()))
                {
                    emptySlot = i;

                    break;
                }
            }

            if (emptySlot != -1)
            {
                if (this.slots[emptySlot] == null)
                {
                    this.slots[emptySlot] = output;
                }
                else if (this.slots[emptySlot].getItem() == output.getItem() && ItemStack.areItemStackTagsEqual(this.slots[emptySlot], output))
                {
                    this.slots[emptySlot].stackSize += output.stackSize;
                }

                for (int i = 0; i < 3; i++)
                {
                    this.slots[i].stackSize--;

                    if (this.slots[i].stackSize <= 0)
                        this.slots[i] = null;
                }
            }
        }
    }

    /**
     * Do not make give this method the name canInteractWith because it clashes with Container
     */
    public boolean isUseableByPlayer(EntityPlayer player)
    {
        return this.worldObj.getTileEntity(this.pos) != this ? false : player.getDistanceSq((double) this.pos.getX() + 0.5D, (double) this.pos.getY() + 0.5D, (double) this.pos.getZ() + 0.5D) <= 64.0D;
    }

    public void openInventory(EntityPlayer player)
    {
    }

    public void closeInventory(EntityPlayer player)
    {
    }

    /**
     * Returns true if automation is allowed to insert the given stack (ignoring stack size) into the given slot.
     */
    public boolean isItemValidForSlot(int index, ItemStack stack)
    {
        return index == 2 ? false : true;
    }

    public int[] getSlotsForFace(EnumFacing side)
    {
        return side == EnumFacing.DOWN ? slotsBottom : (side == EnumFacing.UP ? slotsTop : slotsSides);
    }

    /**
     * Returns true if automation can insert the given item in the given slot from the given side. Args: slot, item,
     * side
     */
    public boolean canInsertItem(int index, ItemStack itemStackIn, EnumFacing direction)
    {
        return this.isItemValidForSlot(index, itemStackIn);
    }

    /**
     * Returns true if automation can extract the given item in the given slot from the given side. Args: slot, item,
     * side
     */
    public boolean canExtractItem(int index, ItemStack stack, EnumFacing direction)
    {
        if (direction == EnumFacing.DOWN && index == 1)
        {
            Item item = stack.getItem();

            if (item != Items.water_bucket && item != Items.bucket)
            {
                return false;
            }
        }

        return true;
    }

    public String getGuiID()
    {
        return JurassiCraft.modid + ":embryonic_machine";
    }

    public Container createContainer(InventoryPlayer playerInventory, EntityPlayer playerIn)
    {
        return new ContainerEmbryonicMachine(playerInventory, this);
    }

    public int getField(int id)
    {
        switch (id)
        {
            case 0:
                return this.embryoTime;
            case 1:
                return this.totalEmbryoTime;
            default:
                return 0;
        }
    }

    public void setField(int id, int value)
    {
        switch (id)
        {
            case 0:
                this.embryoTime = value;
                break;
            case 1:
                this.totalEmbryoTime = value;
        }
    }

    public int getFieldCount()
    {
        return 2;
    }

    public void clear()
    {
        for (int i = 0; i < this.slots.length; ++i)
        {
            this.slots[i] = null;
        }
    }

    @Override
    public Packet getDescriptionPacket()
    {
        NBTTagCompound compound = new NBTTagCompound();
        this.writeToNBT(compound);
        return new S35PacketUpdateTileEntity(this.pos, this.getBlockMetadata(), compound);
    }

    @Override
    public void onDataPacket(NetworkManager net, S35PacketUpdateTileEntity packet)
    {
        NBTTagCompound compound = packet.getNbtCompound();
        this.readFromNBT(compound);
    }
}