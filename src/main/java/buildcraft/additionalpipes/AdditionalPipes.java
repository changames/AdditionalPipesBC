package buildcraft.additionalpipes;

import java.io.File;

import buildcraft.additionalpipes.api.TeleportManagerBase;
import buildcraft.additionalpipes.chunkloader.BlockTeleportTether;
import buildcraft.additionalpipes.chunkloader.ChunkLoadingHandler;
import buildcraft.additionalpipes.chunkloader.TileTeleportTether;
import buildcraft.additionalpipes.gates.GateProvider;
import buildcraft.additionalpipes.gates.TriggerPipeClosed;
import buildcraft.additionalpipes.gui.GuiHandler;
import buildcraft.additionalpipes.item.ItemDogDeaggravator;
import buildcraft.additionalpipes.network.PacketHandler;
import buildcraft.additionalpipes.pipes.TeleportManager;
import buildcraft.additionalpipes.sound.APSounds;
import buildcraft.additionalpipes.test.TeleportManagerTest;
import buildcraft.additionalpipes.utils.Log;
import buildcraft.api.BCModules;
import buildcraft.api.statements.ITriggerInternal;
import buildcraft.api.statements.StatementManager;
import buildcraft.lib.registry.CreativeTabManager;
import buildcraft.lib.registry.CreativeTabManager.CreativeTabBC;
import buildcraft.silicon.BCSiliconItems;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod(modid = AdditionalPipes.MODID, name = AdditionalPipes.NAME, dependencies = "required-after:buildcrafttransport@[7.99.13,);required-after:buildcraftsilicon;required-after:buildcraftfactory", version = AdditionalPipes.VERSION)
public class AdditionalPipes {
	public static final String MODID = "additionalpipes";
	public static final String NAME = "Additional Pipes";
	public static final String VERSION = "6.0.0.1";

	@Instance(MODID)
	public static AdditionalPipes instance;

	@SidedProxy(clientSide = "buildcraft.additionalpipes.MultiPlayerProxyClient", serverSide = "buildcraft.additionalpipes.MultiPlayerProxy")
	public static MultiPlayerProxy proxy;

	public File configFile;
	
	// chunk load boundaries
	//public ChunkLoadViewDataProxy chunkLoadViewer;
	
	public CreativeTabBC creativeTab;
	


	// obsidian fluid pipe
	public Item pipeLiquidsObsidian;
	
	// chunk loader
	public BlockTeleportTether blockTeleportTether;
	
	//dog deaggravator
	public Item dogDeaggravator;
	
	public ITriggerInternal triggerPipeClosed;

	Block blockFoo;
	
	@EventHandler
	public void preInit(FMLPreInitializationEvent event) 
	{
		
		System.err.println("buildcraft transport is loaded: " + BCModules.TRANSPORT.isLoaded());
		
		PacketHandler.init();

		configFile = event.getSuggestedConfigurationFile();
		APConfiguration.loadConfigs(false, configFile);
		MinecraftForge.EVENT_BUS.register(this);
		
		//create BuildCraft creative tab
		creativeTab = CreativeTabManager.createTab("apcreativetab");
		
		Log.info("Registering pipes");
		APPipeDefintions.createPipes();
		APPipeDefintions.setFluidCapacities();
		
		Log.info("Registering gates");
		triggerPipeClosed = new TriggerPipeClosed();
		StatementManager.registerTriggerProvider(new GateProvider());
		
		// create blocks
		if(APConfiguration.enableChunkloader)
		{
			blockTeleportTether = new BlockTeleportTether();
			blockTeleportTether.setRegistryName("teleport_tether");
		}

	}
	
	@SubscribeEvent
	public void registerBlocks(RegistryEvent.Register<Block> event)
	{
		Log.info("Registering blocks");
		
		if(APConfiguration.enableChunkloader)
		{
			event.getRegistry().register(blockTeleportTether);
			
			Log.debug("Chunkloader enabled!");
		}
	    
	}
	
	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event)
	{
		
		Log.info("Registering items");
		dogDeaggravator = new ItemDogDeaggravator();
		event.getRegistry().register(dogDeaggravator);
	    
		event.getRegistry().register(new ItemBlock(blockTeleportTether).setRegistryName(blockTeleportTether.getRegistryName()));
	}
	
	@SubscribeEvent
	public void registerRecipes(RegistryEvent.Register<IRecipe> event)
	{
		Log.info("Registering recipes");
		
		ShapedOreRecipe deaggravatorRecipe = new ShapedOreRecipe(new ResourceLocation(MODID, "recipes/dog_deaggravator"), dogDeaggravator, "gsg", "gig", "g g", 'i', "ingotIron", 'g', "ingotGold", 's', "stickWood");
		deaggravatorRecipe.setRegistryName("dog_deaggravator");
		event.getRegistry().register(deaggravatorRecipe);
		
		if(APConfiguration.enableChunkloader)
		{
			ShapedOreRecipe chunkloaderRecipe = new ShapedOreRecipe(new ResourceLocation(MODID, "recipes/teleport_tether"), blockTeleportTether, "iii", "iLi", "ici", 'i', "ingotIron", 'L', "gemLapis", 'c', BCSiliconItems.redstoneChipset);
			chunkloaderRecipe.setRegistryName("teleport_tether");
			event.getRegistry().register(chunkloaderRecipe);
		}
	}
	
	@SubscribeEvent
	public void registerSounds(RegistryEvent.Register<SoundEvent> event)
	{
		Log.info("Registering sounds");
		
		APSounds.register(event.getRegistry());
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		
		NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());
	
		
		if(APConfiguration.enableChunkloader)
		{
			Log.info("Registering chunk load handler");
			ForgeChunkManager.setForcedChunkLoadingCallback(this, new ChunkLoadingHandler());
			//chunkLoadViewer = new ChunkLoadViewDataProxy(APConfiguration.chunkSightRange);
			//MinecraftForge.EVENT_BUS.register(chunkLoadViewer);
			
			GameRegistry.registerTileEntity(TileTeleportTether.class, "teleport_tether");
			
			// the lasers key function depends on the chunk loading code, so it can only be enabled if the chunk loader is
			proxy.registerKeyHandler();

		}
		APConfiguration.loadConfigs(true, configFile);

		
		//set creative tab icon
		creativeTab.setItem(new ItemStack(APPipeDefintions.itemsTeleportPipeItem));
		
		Log.info("Running Teleport Manager Tests");
		TeleportManagerTest.runAllTests();
		
		//set the reference in the API
		TeleportManagerBase.INSTANCE = TeleportManager.instance;
		
		Log.info("Setting up renderings...");
		proxy.registerRendering();

	}
	
	@EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		
	}

	@EventHandler
	public void onServerStart(FMLServerStartingEvent event) {
		event.registerServerCommand(new CommandAdditionalPipes());
		TeleportManager.instance.reset();
	}


}
