package dev.zsskayr.merlins_inferno;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

import dev.zsskayr.merlins_inferno.event.DemoniteCombatHandler;
import dev.zsskayr.merlins_inferno.registry.ModArmorMaterials;
import dev.zsskayr.merlins_inferno.registry.ModBlocks;
import dev.zsskayr.merlins_inferno.registry.ModCreativeModeTabs;
import dev.zsskayr.merlins_inferno.registry.ModEntityTypes;
import dev.zsskayr.merlins_inferno.registry.ModItems;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(Merlins_inferno.MODID)
public class Merlins_inferno {
    public static final String MODID = "merlins_inferno";
    private static final Logger LOGGER = LogUtils.getLogger();

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public Merlins_inferno(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Each registry class owns its DeferredRegister(s) and hooks itself onto the mod event bus.
        // ArmorMaterials go first since ModItems' armor pieces reference them.
        ModArmorMaterials.register(modEventBus);
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModCreativeModeTabs.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new DemoniteCombatHandler());

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Merlin's Inferno common setup");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Merlin's Inferno server starting");
    }
}
