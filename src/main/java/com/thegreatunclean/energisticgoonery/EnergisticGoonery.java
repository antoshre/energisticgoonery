package com.thegreatunclean.energisticgoonery;

import com.thegreatunclean.energisticgoonery.blocks.InfusionAssembler;
import com.thegreatunclean.energisticgoonery.blocks.PatternProducer;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.Random;

@Mod(modid = EnergisticGoonery.MODID, version = EnergisticGoonery.VERSION, dependencies = "required-after:thaumicenergistics")
public class EnergisticGoonery
{
    public static final String MODID = "energisticgoonery";
    public static final String VERSION = "1.0";
    public static final Random rng = new Random();

    @Mod.Instance("energisticgoonery")
    public static EnergisticGoonery INSTANCE;

    public static final Block bPatternProducer = new PatternProducer.block();
    public static final Block bInfusionAssembler = new InfusionAssembler.block();

    public static CreativeTabs cTab = new CreativeTabs( "EnergisticGoonery" ) {
        @Override
        public ItemStack getIconItemStack()
        {
            return new ItemStack(Item.getItemFromBlock(bPatternProducer));
        }
        @SideOnly(Side.CLIENT)
        public Item getTabIconItem()
        {
            return Item.getItemFromBlock(bPatternProducer);
        }
    };

    @EventHandler
    public void postInit(FMLInitializationEvent event) {

    }
    @EventHandler
    public void init(FMLInitializationEvent event) {

    }

    @EventHandler
    public void load(FMLInitializationEvent event) {
        GameRegistry.registerBlock(bInfusionAssembler, bInfusionAssembler.getUnlocalizedName());
        GameRegistry.registerTileEntity(InfusionAssembler.tile.class, "tile_" + bInfusionAssembler.getUnlocalizedName());

        GameRegistry.registerBlock(bPatternProducer, bPatternProducer.getUnlocalizedName());
        GameRegistry.registerTileEntity(PatternProducer.tile.class, "tile_" + bPatternProducer.getUnlocalizedName());
    }
}
