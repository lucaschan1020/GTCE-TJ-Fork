package gregtech.common.metatileentities.storage;

import codechicken.lib.render.CCRenderState;
import codechicken.lib.render.pipeline.ColourMultiplier;
import codechicken.lib.render.pipeline.IVertexOperation;
import codechicken.lib.vec.Matrix4;
import codechicken.lib.vec.Vector3;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.ModularUI.Builder;
import gregtech.api.gui.widgets.*;
import gregtech.api.metatileentity.ITieredMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntityHolder;
import gregtech.api.render.Textures;
import gregtech.api.util.GTUtility;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.items.ItemStackHandler;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.util.List;

public class MetaTileEntityCreativeTank extends MetaTileEntity implements ITieredMetaTileEntity {

    private static final double[] rotations = new double[]{180.0, 0.0, -90.0, 90.0};

    private FluidTank fluidTank;
    private final ItemStackHandler containerInventory;

    public MetaTileEntityCreativeTank(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId);
        this.containerInventory = new ItemStackHandler(2);
        initializeInventory();
    }

    @Override
    public int getTier() {
        return 1;
    }

    @Override
    protected void initializeInventory() {
        super.initializeInventory();
        this.fluidTank = new CreativeFluidTank();
        this.fluidInventory = fluidTank;
        this.importFluids = new FluidTankList(false, fluidTank);
        this.exportFluids = new FluidTankList(false, fluidTank);
    }

    @Override
    public int getActualComparatorValue() {
        FluidTank fluidTank = this.fluidTank;
        int fluidAmount = fluidTank.getFluidAmount();
        int maxCapacity = fluidTank.getCapacity();
        float f = fluidAmount / (maxCapacity * 1.0f);
        return MathHelper.floor(f * 14.0f) + (fluidAmount > 0 ? 1 : 0);
    }

    @Override
    public void update() {
        super.update();

        if (!getWorld().isRemote) {
            fillContainerFromInternalTank(containerInventory, containerInventory, 0, 1);
            fillInternalTankFromFluidContainer(containerInventory, containerInventory, 0, 1);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        data.setTag("ContainerInventory", containerInventory.serializeNBT());
        data.setTag("FluidInventory", fluidTank.writeToNBT(new NBTTagCompound()));
        return data;
    }

    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        this.containerInventory.deserializeNBT(data.getCompoundTag("ContainerInventory"));
        this.fluidTank.readFromNBT(data.getCompoundTag("FluidInventory"));
    }

    @Override
    public void initFromItemStackData(NBTTagCompound itemStack) {
        super.initFromItemStackData(itemStack);
        if (itemStack.hasKey("FluidName", Constants.NBT.TAG_COMPOUND)) {
            this.fluidTank.setFluid(FluidStack.loadFluidStackFromNBT(itemStack.getCompoundTag("FluidName")));
        }
    }

    @Override
    public void writeItemStackData(NBTTagCompound itemStack) {
        super.writeItemStackData(itemStack);
        FluidStack fluidStack = fluidTank.getFluid();
        if (fluidStack != null && fluidStack.amount > 0) {
            NBTTagCompound tagCompound = new NBTTagCompound();
            fluidStack.writeToNBT(tagCompound);
            itemStack.setTag("FluidName", tagCompound);
        }
    }

    @Override
    protected boolean shouldSerializeInventories() {
        return false;
    }

    @Override
    public void clearMachineInventory(NonNullList<ItemStack> itemBuffer) {
        super.clearMachineInventory(itemBuffer);
        clearInventory(itemBuffer, containerInventory);
    }

    @Override
    public MetaTileEntity createMetaTileEntity(MetaTileEntityHolder holder) {
        return new MetaTileEntityCreativeTank(metaTileEntityId);
    }

    @Override
    protected FluidTankList createImportFluidHandler() {
        return new FluidTankList(false, fluidTank);
    }

    @Override
    protected FluidTankList createExportFluidHandler() {
        return new FluidTankList(false, fluidTank);
    }

    @Override
    public boolean hasFrontFacing() {
        return false;
    }

    @Override
    public void renderMetaTileEntity(CCRenderState renderState, Matrix4 translation, IVertexOperation[] pipeline) {
        Textures.VOLTAGE_CASINGS[1].render(renderState, translation, ArrayUtils.add(pipeline,
                new ColourMultiplier(GTUtility.convertRGBtoOpaqueRGBA_CL(getPaintingColorForRendering()))));
        translation.translate(0.5, 0.001, 0.5);
        translation.rotate(Math.toRadians(rotations[getFrontFacing().getIndex() - 2]), new Vector3(0.0, 1.0, 0.0));
        translation.translate(-0.5, 0.0, -0.5);
        Textures.SCREEN.renderSided(EnumFacing.UP, renderState, translation, pipeline);
    }

    @Override
    public Pair<TextureAtlasSprite, Integer> getParticleTexture() {
        return Pair.of(Textures.VOLTAGE_CASINGS[1].getParticleSprite(), getPaintingColor());
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World player, List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.machine.quantum_tank.capacity", Integer.MAX_VALUE));
        NBTTagCompound compound = stack.getTagCompound();
        if (compound != null) {
            if (compound.hasKey("FluidName", Constants.NBT.TAG_COMPOUND)) {
                FluidStack fluidStack = FluidStack.loadFluidStackFromNBT(compound.getCompoundTag("FluidName"));
                if (fluidStack != null) {
                    tooltip.add(I18n.format("gregtech.machine.quantum_tank.tooltip.name", fluidStack.getLocalizedName()));
                    tooltip.add(I18n.format("gregtech.machine.quantum_tank.tooltip.count", fluidStack.amount));
                }
            }
        }
    }

    @Override
    protected ModularUI createUI(EntityPlayer entityPlayer) {
        Builder builder = ModularUI.defaultBuilder();
        builder.image(7, 16, 81, 55, GuiTextures.DISPLAY);
        TankWidget tankWidget = new TankWidget(fluidTank, 69, 52, 18, 18)
                .setHideTooltip(true).setAlwaysShowFull(true);
        builder.widget(tankWidget);
        builder.label(11, 20, "gregtech.gui.fluid_amount", 0xFFFFFF);
        builder.dynamicLabel(11, 30, tankWidget::getFormattedFluidAmount, 0xFFFFFF);
        builder.dynamicLabel(11, 40, tankWidget::getFluidLocalizedName, 0xFFFFFF);
        return builder.label(6, 6, getMetaFullName())
                .widget(new FluidContainerSlotWidget(containerInventory, 0, 90, 17, false)
                        .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.IN_SLOT_OVERLAY))
                .widget(new ImageWidget(91, 36, 14, 15, GuiTextures.TANK_ICON))
                .widget(new SlotWidget(containerInventory, 1, 90, 54, true, false)
                        .setBackgroundTexture(GuiTextures.SLOT, GuiTextures.OUT_SLOT_OVERLAY))
                .widget(new ToggleButtonWidget(151, 64, 18, 18,
                        GuiTextures.BUTTON_FLUID_VOID, this::isFluidCleared, this::clearFluid)
                        .setTooltipText("gregtech.gui.clear_fluid.tooltip")
                        .shouldUseBaseBackground())
                .bindPlayerInventory(entityPlayer.inventory)
                .build(getHolder(), entityPlayer);
    }

    protected boolean isFluidCleared() {
        return false;
    }

    protected void clearFluid(boolean shouldClear) {
        ((CreativeFluidTank) this.fluidTank).clearFluid();
        if (!getWorld().isRemote) {
            markDirty();
        }
    }

    private class CreativeFluidTank extends FluidTank {
        public CreativeFluidTank() {
            super(Integer.MAX_VALUE);
        }

        @Override
        public int fillInternal(FluidStack resource, boolean doFill) {
            int accepted = super.fillInternal(new FluidStack(resource, Integer.MAX_VALUE), doFill);

            // if we couldn't accept "resource", and "resource" is not the same as the stored fluid.
            if (accepted == 0 && !resource.isFluidEqual(getFluid())) {
                return 0;
            }
            return resource.amount;
        }

        @Override
        @Nullable
        public FluidStack drainInternal(int maxDrain, boolean doDrain) {
            if (fluid == null || maxDrain <= 0) {
                return null;
            }

            return new FluidStack(fluid, maxDrain);
        }

        public void clearFluid() {
            super.drainInternal(Integer.MAX_VALUE, true);
        }
    }
}
