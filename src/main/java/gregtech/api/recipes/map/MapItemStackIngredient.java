package gregtech.api.recipes.map;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class MapItemStackIngredient extends AbstractMapIngredient {

    protected ItemStack stack;
    protected int meta;
    protected NBTTagCompound tag;

    public MapItemStackIngredient(ItemStack stack, int meta, NBTTagCompound tag) {
        this.stack = stack;
        this.meta = meta;
        this.tag = tag;
    }

    public MapItemStackIngredient(ItemStack stack) {
        this.stack = stack;
        this.meta = stack.getMetadata();
        this.tag = stack.getTagCompound();
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) {
            return false;
        }
        MapItemStackIngredient other = (MapItemStackIngredient) o;
        if (this.stack.getItem() != other.stack.getItem()) {
            return false;
        }

        return this.meta == other.meta;
    }

    @Override
    protected int hash() {
        int hash = stack.getItem().hashCode() * 31;
        hash += 31 * this.meta;
        hash += 31 * (this.tag != null ? this.tag.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "MapItemStackIngredient{" + "item=" + stack.getItem().getRegistryName() + "} {meta=" + meta + "} {tag=" +
                tag + "}";
    }
}
