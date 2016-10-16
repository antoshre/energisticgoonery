package com.thegreatunclean.energisticgoonery.utils;

import appeng.api.AEApi;
import cpw.mods.fml.common.FMLLog;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

/**
 * Created by rob on 10/10/2016.
 */
public class PatternMaker {
    private static final String NBTKEY_AE_IN = "in",
            NBTKEY_AE_OUT = "out",
            NBTKEY_AE_ISCRAFTING = "crafting",
            NBTKEY_AE_CAN_SUB = "substitute";

    //Construct special essentia crafting pattern.
    //returns null if invalid inputs / outputs.
    public static ItemStack make(ItemStack[] input, ItemStack[] output) {
        NBTTagCompound data = new NBTTagCompound();

        NBTTagList inTags = new NBTTagList();
        for(ItemStack item : input) {
            if (item == null) {
                FMLLog.severe("null input to PatternMaker!?");
                return null;
            }
            inTags.appendTag( item.writeToNBT(new NBTTagCompound()) );
        }
        NBTTagList outTags = new NBTTagList();
        for(ItemStack item : output) {
            if (item == null) {
                FMLLog.severe("null input to PatternMaker!?");
                return null;
            }
            outTags.appendTag( item.writeToNBT(new NBTTagCompound()) );
        }
        data.setBoolean( NBTKEY_AE_CAN_SUB, false );
        data.setBoolean( NBTKEY_AE_ISCRAFTING, false );

        data.setTag( NBTKEY_AE_IN, inTags );
        data.setTag( NBTKEY_AE_OUT, outTags );

        ItemStack pattern = AEApi.instance().definitions().items().encodedPattern().maybeStack( 1 ).orNull();
        pattern.setTagCompound( data );
        return pattern;
    }
}
