package net.blay09.mods.cookingbook.container;

import com.google.common.collect.Maps;
import cpw.mods.fml.common.Optional;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import invtweaks.api.container.ContainerSection;
import invtweaks.api.container.ContainerSectionCallback;
import net.blay09.mods.cookingbook.block.TileEntityCookingOven;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;

import java.util.List;
import java.util.Map;

public class ContainerCookingOven extends Container {

    private final TileEntityCookingOven tileEntity;
    private final int[] lastCookTime = new int[9];
    private int lastBurnTime;
    private int lastItemBurnTime;

    public ContainerCookingOven(InventoryPlayer inventoryPlayer, TileEntityCookingOven tileEntity) {
        this.tileEntity = tileEntity;

        for(int i = 0; i < 3; i++) {
            addSlotToContainer(new SlotOvenInput(tileEntity, i, 84 + i * 18, 19));
        }

        addSlotToContainer(new SlotOvenFuel(tileEntity, 3, 61, 59));

        for(int i = 0; i < 3; i++) {
            addSlotToContainer(new SlotOvenResult(tileEntity, i + 4, 142, 41 + i * 18));
        }

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 3; j++) {
                addSlotToContainer(new SlotOven(tileEntity, 7 + j + i * 3, 84 + j * 18, 41 + i * 18));
            }
        }

        for(int i = 0; i < 4; i++) {
            addSlotToContainer(new SlotOvenTool(tileEntity, 16 + i, 8, 19 + i * 18, i));
        }

        for(int i = 0; i < 3; i++) {
            for(int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(inventoryPlayer, j + i * 9 + 9, 30 + j * 18, 111 + i * 18));
            }
        }

        for(int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(inventoryPlayer, i, 30 + i * 18, 169));
        }

        tileEntity.openInventory();
    }

    @Override
    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);
        tileEntity.closeInventory();
    }

    @Override
    public void addCraftingToCrafters(ICrafting crafter) {
        super.addCraftingToCrafters(crafter);
        crafter.sendProgressBarUpdate(this, 0, tileEntity.furnaceBurnTime);
        crafter.sendProgressBarUpdate(this, 1, tileEntity.currentItemBurnTime);
        for(int i = 0; i < tileEntity.slotCookTime.length; i++) {
            crafter.sendProgressBarUpdate(this, 2 + i, tileEntity.slotCookTime[i]);
        }
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();

        for(Object obj : crafters) {
            ICrafting crafter = (ICrafting) obj;

            if (this.lastBurnTime != tileEntity.furnaceBurnTime) {
                crafter.sendProgressBarUpdate(this, 0, tileEntity.furnaceBurnTime);
            }

            if (this.lastItemBurnTime != tileEntity.currentItemBurnTime) {
                crafter.sendProgressBarUpdate(this, 1, tileEntity.currentItemBurnTime);
            }

            for (int i = 0; i < tileEntity.slotCookTime.length; i++) {
                if (lastCookTime[i] != tileEntity.slotCookTime[i]) {
                    crafter.sendProgressBarUpdate(this, 2 + i, tileEntity.slotCookTime[i]);
                }
            }

        }

        this.lastBurnTime = this.tileEntity.furnaceBurnTime;
        this.lastItemBurnTime = this.tileEntity.currentItemBurnTime;
        for (int i = 0; i < tileEntity.slotCookTime.length; i++) {
            lastCookTime[i] = tileEntity.slotCookTime[i];
        }
    }

    @SideOnly(Side.CLIENT)
    public void updateProgressBar(int id, int value) {
        if (id == 0) {
            tileEntity.furnaceBurnTime = value;
        } else if (id == 1) {
            tileEntity.currentItemBurnTime = value;
        } else if(id >= 2 && id <= tileEntity.slotCookTime.length + 2) {
            tileEntity.slotCookTime[id - 2] = value;
        }
    }

    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        ItemStack itemStack = null;
        Slot slot = (Slot) inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack slotStack = slot.getStack();
            itemStack = slotStack.copy();

            if (slotIndex >= 7 && slotIndex <= 20) {
                if (!mergeItemStack(slotStack, 20, 56, true)) {
                    return null;
                }
                slot.onSlotChange(slotStack, itemStack);
            } else if(slotIndex >= 4 && slotIndex <= 6) {
                if (!this.mergeItemStack(slotStack, 20, 56, false)) {
                    return null;
                }
            } else if (slotIndex > 20) {
                ItemStack smeltingResult = TileEntityCookingOven.getSmeltingResult(slotStack);
                if (TileEntityCookingOven.isItemFuel(slotStack)) {
                    if (!mergeItemStack(slotStack, 3, 4, false)) {
                        return null;
                    }
                } else if (smeltingResult != null && smeltingResult.getItem() instanceof ItemFood) {
                    if (!this.mergeItemStack(slotStack, 0, 3, false)) {
                        return null;
                    }
                } else if (slotIndex >= 21 && slotIndex < 49) {
                    if (!this.mergeItemStack(slotStack, 49, 56, false)) {
                        return null;
                    }
                } else if (slotIndex >= 49 && slotIndex < 56 && !this.mergeItemStack(slotStack, 20, 49, false)) {
                    return null;
                }
            } else if (!this.mergeItemStack(slotStack, 20, 49, false)) {
                return null;
            }

            if (slotStack.stackSize == 0) {
                slot.putStack(null);
            } else {
                slot.onSlotChanged();
            }

            if (slotStack.stackSize == itemStack.stackSize) {
                return null;
            }

            slot.onPickupFromSlot(player, slotStack);
        }

        return itemStack;
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }

    @ContainerSectionCallback
    @Optional.Method(modid = "inventorytweaks")
    @SuppressWarnings("unchecked")
    public Map<ContainerSection, List<Slot>> getContainerSections() {
        Map<ContainerSection, List<Slot>> map = Maps.newHashMap();
        map.put(ContainerSection.FURNACE_IN, inventorySlots.subList(0, 3));
        map.put(ContainerSection.FURNACE_FUEL, inventorySlots.subList(3, 4));
        map.put(ContainerSection.FURNACE_OUT, inventorySlots.subList(4, 7));
        map.put(ContainerSection.INVENTORY, inventorySlots.subList(20, 57));
        map.put(ContainerSection.INVENTORY_NOT_HOTBAR, inventorySlots.subList(20, 48));
        map.put(ContainerSection.INVENTORY_HOTBAR, inventorySlots.subList(47, 57));
        return map;
    }

    public TileEntityCookingOven getTileEntity() {
        return tileEntity;
    }
}
