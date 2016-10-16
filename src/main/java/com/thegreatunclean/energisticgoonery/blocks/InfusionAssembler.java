package com.thegreatunclean.energisticgoonery.blocks;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingProviderHelper;
import appeng.api.networking.events.MENetworkCraftingPatternChange;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.core.localization.WailaText;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.tile.TileEvent;
import appeng.tile.events.TileEventType;
import appeng.tile.grid.AENetworkTile;
import com.thegreatunclean.energisticgoonery.EnergisticGoonery;
import com.thegreatunclean.energisticgoonery.utils.InfusionCraftingPatternDetails;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import thaumcraft.api.ThaumcraftApi;
import thaumcraft.api.ThaumcraftApiHelper;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IAspectContainer;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.api.crafting.InfusionEnchantmentRecipe;
import thaumcraft.api.crafting.InfusionRecipe;
import thaumcraft.common.config.ConfigBlocks;
import thaumcraft.common.lib.crafting.InfusionRunicAugmentRecipe;
import thaumcraft.common.lib.events.EssentiaHandler;
import thaumcraft.common.lib.research.ResearchManager;
import thaumicenergistics.api.grid.IEssentiaGrid;
import thaumicenergistics.api.grid.IMEEssentiaMonitor;
import thaumicenergistics.common.blocks.AbstractBlockAEWrenchable;
import thaumicenergistics.common.integration.IWailaSource;
import thaumicenergistics.common.items.ItemCraftingAspect;
import thaumicenergistics.common.registries.EnumCache;
import thaumicenergistics.common.storage.AspectStack;
import thaumicenergistics.common.tiles.abstraction.TileProviderBase;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import static com.thegreatunclean.energisticgoonery.blocks.InfusionAssembler.tile.STATES.CRAFTING;
import static com.thegreatunclean.energisticgoonery.blocks.InfusionAssembler.tile.STATES.IDLE;
import static thaumcraft.client.lib.PlayerNotifications.aspectList;

/**
 * Created by rob on 10/10/2016.
 */
public class InfusionAssembler {

    public static class block extends AbstractBlockAEWrenchable {
        public block() {
            super(Material.iron);
            this.setHardness(1.0f);
            this.setBlockName("infusionassembler");
            this.setBlockTextureName("energisticgoonery:infusionassembler"); //TODO: fix
        }

        @Override
        public TileEntity createNewTileEntity(World p_149915_1_, int p_149915_2_) {
            return new InfusionAssembler.tile();
        }

        @Override
        public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player) {
            if (player.isSneaking()) {
                TileEntity te = world.getTileEntity(x,y,z);
                if (te instanceof InfusionAssembler.tile) {
                    ((InfusionAssembler.tile)te).setOwner(player);
                    return true;
                }
            }
            return false;
        }
    }
    public static class tile extends AENetworkTile implements ICraftingProvider, IAspectContainer, IWailaSource {

        private Set<InfusionRecipe> infusionrecipes = null;
        private MachineSource myMachine;

        public tile() {
            myMachine = new MachineSource(this);

            if(FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                AENetworkProxy aeproxy = this.getProxy();
                aeproxy.setIdlePowerUsage(0.0f);
                aeproxy.setFlags(GridFlags.REQUIRE_CHANNEL);
            }
        }

        public void getInfusionRecipes() {
            infusionrecipes = new HashSet<InfusionRecipe>();
            List recipes = ThaumcraftApi.getCraftingRecipes();
            for(Object obj : recipes) {
                if (obj instanceof InfusionRunicAugmentRecipe || obj instanceof InfusionEnchantmentRecipe) {
                    continue;
                }
                if (obj instanceof InfusionRecipe) {
                    InfusionRecipe recipe = (InfusionRecipe)obj;
                    if (!ThaumcraftApiHelper.isResearchComplete(this.owner, recipe.getResearch())) {
                        continue; //doesn't know it
                    }
                    if (recipe.getComponents() == null || recipe.getRecipeInput() == null || recipe.getRecipeOutput() == null) {
                        continue; //invalid recipe?!
                    }
                    if (!(recipe.getRecipeOutput() instanceof ItemStack)) {
                        continue; //output isn't an item?!
                    }
                    if (((ItemStack)recipe.getRecipeOutput()).getItem() == null) {
                        continue; //fuck
                    }
                    infusionrecipes.add(recipe);
                }
            }
        }

        public void setOwner(EntityPlayer player) {
            this.owner = player.getDisplayName();
            getInfusionRecipes();

            if (!worldObj.isRemote) {
                ChatComponentText m1 = new ChatComponentText("Owner set to: " + this.owner);
                ChatComponentText m2 = new ChatComponentText("Loaded " + this.infusionrecipes.size() + " infusions.");
                player.addChatComponentMessage(m1);
                player.addChatComponentMessage(m2);
            }
            //post message informing network that patterns may have changed
            if (FMLCommonHandler.instance().getEffectiveSide().isServer()) {
                try {
                    this.getProxy().getGrid().postEvent(new MENetworkCraftingPatternChange(this, this.getProxy().getNode()));
                } catch (GridAccessException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void provideCrafting(ICraftingProviderHelper iCraftingProviderHelper) {
            if (this.owner == null) { return; } //No owner? No recipes.
            if (infusionrecipes == null) { getInfusionRecipes(); } //recipes not loaded? load them.
            for(InfusionRecipe recipe : infusionrecipes) {
                InfusionCraftingPatternDetails pattern = new InfusionCraftingPatternDetails(recipe);
                iCraftingProviderHelper.addCraftingOption(this, pattern);
                //logRecipe(recipe);
            }
        }


        @Override
        public boolean pushPattern(ICraftingPatternDetails iCraftingPatternDetails, InventoryCrafting inventoryCrafting) {
            if (this.state != CRAFTING && iCraftingPatternDetails instanceof InfusionCraftingPatternDetails) {
                this.state = CRAFTING;
                this.outputStack = iCraftingPatternDetails.getOutputs()[0].getItemStack();
                this.craftingAspects = ((InfusionCraftingPatternDetails)iCraftingPatternDetails).getAspects();
                this.tickCounter = 100;
                worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);

                FMLLog.info("Accepted recipe instability %d", ((InfusionCraftingPatternDetails)iCraftingPatternDetails).getInstability());
                return true;
            }
            return false;
        }

        @Override
        public boolean isBusy() {
            return this.state == CRAFTING;
        }

        public boolean isActive() { //is connected to AE network
            if (!worldObj.isRemote) {
                AENetworkProxy proxy = this.getProxy();
                if (proxy != null && proxy.getNode() != null) {
                    this.isActive = proxy.getNode().isActive();
                }
            }
            return this.isActive;
        }

        //AspectList junk = new AspectList(AspectList.)


        enum STATES {IDLE, CRAFTING};

        private int tickCountdown;
        @TileEvent(TileEventType.TICK)
        public void onTick() {
            if (worldObj.isRemote) { return; }
            if (this.state == CRAFTING) { craftingTick(); }
        }


        private String owner;
        private STATES state = IDLE;
        private ItemStack outputStack;
        private AspectList craftingAspects;
        private long tickCounter;
        private boolean isActive;



        public void craftingTick() {
            if (this.state != CRAFTING) {
                FMLLog.severe("craftingTick called when not crafting.  THIS IS A BUG.");
                return;
            }
            if (this.outputStack == null || this.craftingAspects == null) { //fuck how did you do this
                FMLLog.severe("craftingTick called with invalid setup. THIS IS A BUG.");
                this.state = IDLE;
                return;
            }

            //decrement tickCountdown, clamp at zero.
            this.tickCounter = (--this.tickCounter < 0 ? 0 : this.tickCounter);

            if (this.tickCounter <= 0 && this.craftingAspects.size() <= 0) {
                //Hooray!  Done crafting!
                try {
                    AENetworkProxy aeproxy = this.getProxy();
                    IStorageGrid storageGrid = aeproxy.getStorage();
                    IAEItemStack rejected = storageGrid.getItemInventory().injectItems( //can grid accept item?
                            AEApi.instance().storage().createItemStack(this.outputStack),
                            Actionable.SIMULATE, myMachine);
                    if (rejected == null || rejected.getStackSize() == 0) { //yes it can, do transfer
                        storageGrid.getItemInventory().injectItems(AEApi.instance().storage().createItemStack(this.outputStack),
                                Actionable.MODULATE, myMachine);
                        this.state = IDLE;
                        this.outputStack = null;
                        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                        this.markDirty();
                    }
                } catch (GridAccessException e) {
                    FMLLog.severe("Infusion Assembler GAE:");
                    e.printStackTrace();
                }
            } else {
                if (this.tickCounter % 100 == 0) {
                    //Attempt to grab 1 needed aspect.
                    if (this.craftingAspects.size() > 0) {
                        Aspect target = this.craftingAspects.getAspects()[0];
                        int quantity = this.craftingAspects.getAmount(target);
                        int extracted = extractEssentiaFromNetwork(target, quantity);
                        this.craftingAspects.reduce(target, extracted);
                        if (this.craftingAspects.getAmount(target) <= 0) {
                            this.craftingAspects.remove(target);
                            this.tickCounter += 100;
                        }
                        AENetworkProxy proxy = this.getProxy();
                        if (proxy != null) {
                            IGridNode node = proxy.getNode();
                            if (node != null) {
                                node.updateState();
                            }
                        }
                        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
                        this.markDirty();
                    } else {
                        FMLLog.severe("craftingAspects is empty!?");
                    }
                } else {
                    //Do something else per crafting tick that isn't 0mod20.
                }
            }
        }

        //Attempt to extract {qnt} of {aspect} from the network.
        //returns amount actually extracted
        private int extractEssentiaFromNetwork(Aspect aspect, int qnt) {
            IMEEssentiaMonitor monitor = null;
            IGridNode node = this.getProxy().getNode();
            if (node != null) {
                IGrid grid = node.getGrid();
                if (grid != null) {
                    monitor = grid.getCache(IEssentiaGrid.class);
                }
            }
            if (monitor != null) {
                long extracted = monitor.extractEssentia(aspect, qnt, Actionable.SIMULATE, this.myMachine, true);
                if (extracted > 0) {
                    //go for actual extract
                    return (int)monitor.extractEssentia(aspect, qnt, Actionable.MODULATE, this.myMachine, true);
                }
            }
            return 0;
        }

        //Synch server-client network blocks
        @TileEvent(TileEventType.NETWORK_WRITE)
        public void onSendNetworkData( final ByteBuf data ) throws IOException {
            if (this.craftingAspects != null) {
                data.writeInt(this.craftingAspects.size());
                for(Aspect aspect : this.craftingAspects.getAspects()) {
                    {
                        int qnt = this.craftingAspects.getAmount(aspect);
                        AspectStack stack = new AspectStack(aspect, qnt);
                        stack.writeToStream(data);
                    }
                }
            } else { //craftingAspects is null
                    data.writeInt(-1);
            }
        }
        @TileEvent(TileEventType.NETWORK_READ)
        @SideOnly(Side.CLIENT)
        public boolean onReceiveNetworkData( final ByteBuf data )
        {
            int status = data.readInt();
            if (status == -1) {
                //this.craftingAspects was null
                this.craftingAspects = null;
                return true;
            } else {
                //status is number of AspectStacks to follow
                AspectList list = new AspectList();
                for(int i=0; i < status; i++) {
                    AspectStack stack = new AspectStack();
                    stack.readFromStream(data);
                    list.add(stack.getAspect(), (int)stack.getStackSize());
                }
                this.craftingAspects = list;
                return true;
            }
        }

        @Override
        public AspectList getAspects() {
            return this.craftingAspects;
        }

        @Override
        public void setAspects(AspectList aspectList) {
            //no
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
            if (this.craftingAspects != null) {
                return (this.craftingAspects.getAmount(aspect) >= i);
            }
            return false;
        }

        @Override
        public boolean doesContainerContain(AspectList aspectList) {
            if (this.craftingAspects != null) {
                for (Aspect aspect : aspectList.getAspects()) {
                    int qnt = aspectList.getAmount(aspect);
                    if (!doesContainerContainAmount(aspect, qnt)) {
                        return false;
                    }
                }
            } else {
                return false;
            }
            return true;
        }

        @Override
        public int containerContains(Aspect aspect) {
            return 0;
        }

        @TileEvent(TileEventType.WORLD_NBT_READ)
        public void onLoadNBT( final NBTTagCompound data )
        {
            if (data.hasKey("owner")) {
                this.owner = data.getString("owner");
            }

            if (data.hasKey("state")) {
                this.state = STATES.valueOf(data.getString("state"));
            }

            if (data.hasKey("outputStack")) {
                this.outputStack = ItemStack.loadItemStackFromNBT((NBTTagCompound)data.getTag("outputStack"));
            }

            if (data.hasKey("craftingAspects")) {
                this.craftingAspects = new AspectList();
                this.craftingAspects.readFromNBT((NBTTagCompound)data.getTag("craftingAspects"));
            }

            if (data.hasKey("tickCounter")) {
                this.tickCounter = data.getLong("tickCounter");
            }

            if (data.hasKey("isActive")) {
                this.isActive = data.getBoolean("isActive");
            }
        }
        @TileEvent(TileEventType.WORLD_NBT_WRITE)
        public void onSaveNBT( final NBTTagCompound data )
        {
            if (this.owner != null) {
                data.setString("owner", owner);
            }

            if (this.state != null) {
                switch (this.state) {
                    case IDLE:
                        data.setString("state", "IDLE");
                        break;
                    case CRAFTING:
                        data.setString("state", "CRAFTING");
                }
            }

            if (this.outputStack != null) {
                NBTTagCompound osTag = new NBTTagCompound();
                this.outputStack.writeToNBT(osTag);
                data.setTag("outputStack", osTag);
            }

            if (this.craftingAspects != null) {
                NBTTagCompound caTag = new NBTTagCompound();
                this.craftingAspects.writeToNBT(caTag);
                data.setTag("craftingAspects", caTag);
            }

            data.setLong("tickCounter", this.tickCounter);

            data.setBoolean("isActive", this.isActive);


        }

        @Override
        public void addWailaInformation( final List<String> tooltip )
        {
            if( this.isActive() )
            {
                if (this.state == CRAFTING) {
                    tooltip.add("Crafting");
                    if (this.craftingAspects != null) {
                        for(Aspect aspect : this.craftingAspects.getAspects()) {
                            int qnt = this.craftingAspects.getAmount(aspect);
                            tooltip.add(qnt + " " + aspect.getName());
                        }
                    }
                } else {
                    tooltip.add("Idle");
                }
            }
            else
            {
                tooltip.add( WailaText.DeviceOffline.getLocal() );
            }
        }
    }
}
