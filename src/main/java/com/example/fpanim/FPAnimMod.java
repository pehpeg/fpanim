package com.example.fpanim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
        modid = FPAnimMod.MODID,
        name = "First Person Animations",
        version = "0.1.0",
        clientSideOnly = true,
        acceptedMinecraftVersions = "[1.12.2]"
)
public class FPAnimMod {
    public static final String MODID = "fpanim";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new FirstPersonRenderer());

        // Registering also triggers an immediate load. F3+T re-runs it, so you can
        // re-export from Blockbench and see changes without restarting the game.
        ((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager())
                .registerReloadListener(new AnimationLibrary());
    }
}
