package com.thegreatunclean.energisticgoonery.blocks;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.MachineSource;
import appeng.tile.grid.AENetworkTile;
import com.sun.org.apache.bcel.internal.generic.FNEG;
import com.thegreatunclean.energisticgoonery.EnergisticGoonery;
import com.thegreatunclean.energisticgoonery.utils.PatternMaker;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.AspectSourceHelper;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.tiles.TileInfusionMatrix;
import thaumicenergistics.common.blocks.AbstractBlockAEWrenchable;
import thaumicenergistics.common.items.ItemCraftingAspect;

/**
 * Created by rob on 10/10/2016.
 */
public class PatternProducer {
    public static class block extends AbstractBlockAEWrenchable {
        public block() {
            super(Material.iron);
            this.setHardness(1.0f);
            this.setBlockName("patternproducer");
            this.setBlockTextureName("energisticgoonery:patternproducer");
        }

        @Override
        public TileEntity createNewTileEntity(World world, int metadata) {
            return new PatternProducer.tile();
        }
        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player) {
            if (player.isSneaking()) {
                TileEntity te = world.getTileEntity(x,y,z);
                if (te instanceof PatternProducer.tile) {
                    ((PatternProducer.tile)te).activate(player);
                    return true;
                }
            }
            return false;
        }
    }
    public static class tile extends TileEntity implements IAspectContainer {


        private TileInfusionMatrix matrix;

        public tile() {
            aspectStorage = new AspectList();
        }

        public void activate(EntityPlayer player) {
            /*
            ItemStack pattern = AEApi.instance().definitions().items().encodedPattern().maybeStack( 1 ).orNull();
            ItemStack[] inputs = {new ItemStack(Items.blaze_powder,5), new ItemStack(Items.carrot)};
            ItemStack[] outputs = {ItemCraftingAspect.createStackForAspect(Aspect.AURA, 100)};

            pattern = PatternMaker.make(inputs, outputs);
            player.inventory.addItemStackToInventory(pattern);
            */

            aspectStorage.add(Aspect.AURA, 1);


        }

        private AspectList aspectStorage;
        @Override
        public AspectList getAspects() {
            return aspectStorage;
        }

        @Override
        public void setAspects(AspectList aspectList) {
            if (aspectList != null) {
                this.aspectStorage = aspectList;
            }

        }

        @Override
        public boolean doesContainerAccept(Aspect aspect) {
            return false;
        }

        @Override
        public int addToContainer(Aspect aspect, int i) {
            return 0;
        }

        @Override
        public boolean takeFromContainer(Aspect aspect, int i) {
            return false;
        }

        @Override
        public boolean takeFromContainer(AspectList aspectList) {
            return false;
        }

        @Override
        public boolean doesContainerContainAmount(Aspect aspect, int i) {
            if (aspect == Aspect.AURA) {
                return (aspectStorage.getAmount(Aspect.AURA) >= i);
            }
            return false;
        }

        @Override
        public boolean doesContainerContain(AspectList aspectList) {
            return false;
        }

        @Override
        public int containerContains(Aspect aspect) {
            return aspectStorage.getAmount(aspect);
        }
    }
}
