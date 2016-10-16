package com.thegreatunclean.energisticgoonery.utils;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumicenergistics.common.items.ItemCraftingAspect;

import java.util.*;

/**
 * Created by rob on 10/10/2016.
 */
public class InfusionCraftingPatternDetails implements ICraftingPatternDetails {

    private IAEItemStack output;
    private IAEItemStack[] ingredients;
    private AspectList aspects;

    private int instability;

    public InfusionCraftingPatternDetails(InfusionRecipe recipe) {
        ItemStack temp = (ItemStack)recipe.getRecipeOutput();
        //temp.setItemDamage(0);
        this.output = AEItemStack.create(temp);
        //this.output = AEItemStack.create((ItemStack)recipe.getRecipeOutput());

        //length+1 is for the components plus the central pedestal item.
        //was going to precalculate the essentia cost to preallocate
        //but that turned out to be ugly and for basically no benefit
        ArrayList<IAEItemStack> inputs = new ArrayList<IAEItemStack>(recipe.getComponents().length + 1);
        for (ItemStack itemStack : recipe.getComponents()) {
            if (itemStack.getItemDamage() == 32767) {
                itemStack.setItemDamage(0);
            }
            inputs.add(AEItemStack.create(itemStack));
        }
        inputs.add(AEItemStack.create(recipe.getRecipeInput()));


        /*
        //create essence components
        AspectList aspectList = recipe.getAspects();
        for (Aspect aspect : aspectList.getAspects()) {
            int qnt = aspectList.getAmount(aspect);
            ItemStack itemStack = ItemCraftingAspect.createStackForAspect(aspect, qnt);
            inputs.add(AEItemStack.create(itemStack));
        }
        */
        this.aspects = recipe.getAspects().copy();

        this.ingredients = inputs.toArray(new IAEItemStack[inputs.size()]);

        this.instability = recipe.getInstability();
    }

    public AspectList getAspects() {
        return this.aspects.copy();
    }

    public int getInstability() {
        return this.instability;
    }

    @Override
    public ItemStack getPattern() {
        return null;
    }

    @Override
    public boolean isValidItemForSlot(int i, ItemStack itemStack, World world) {
        return false;
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    //make sure the return stack is at least 9 long.  Otherwise NPE inside AE2.
    //supposed to represent a crafting table inventory, empty spots should be null
    @Override
    public IAEItemStack[] getInputs() {
        IAEItemStack[] inputs = new IAEItemStack[9];
        Arrays.fill(inputs, null); //null initialize
        for (int i=0; i < Math.min(inputs.length,this.ingredients.length); i++) {
            inputs[i] = this.ingredients[i]; //populate with actual things
        }
        return inputs;
    }

    //doesn't seem to fuck up when this is longer than 9.
    //probably helps that most items are the crystal essences, which are terminal items
    //in the crafting chain.  If you have some fucked up / super complicated crafting mechanism
    //that makes crystal essence, probably not going to play well with this.
    @Override
    public IAEItemStack[] getCondensedInputs() {
        return this.ingredients;
    }

    @Override
    public IAEItemStack[] getCondensedOutputs() {
        List<IAEItemStack> list = new ArrayList<IAEItemStack>(1);
        list.add(this.output);
        return list.toArray(new IAEItemStack[list.size()]);
    }

    @Override
    public IAEItemStack[] getOutputs() {
        return getCondensedOutputs();
    }

    @Override
    public boolean canSubstitute() {
        return false;
    }

    @Override
    public ItemStack getOutput(InventoryCrafting inventoryCrafting, World world) {
        return this.output.getItemStack();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void setPriority(int i) {

    }

    public void setOutputs(ItemStack stuff) {
        this.output = AEItemStack.create(stuff);
    }
}
