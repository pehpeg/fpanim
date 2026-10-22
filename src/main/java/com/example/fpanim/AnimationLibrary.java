package com.example.fpanim;

import com.example.fpanim.anim.Animation;
import com.example.fpanim.anim.AnimationParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.item.*;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Loads animations from, in order (later wins on name clashes):
 *   1. assets/fpanim/animations/firstperson.animation.json in the mod jar and any resource pack
 *   2. every *.json in .minecraft/config/fpanim/ (export straight here from Blockbench, then F3+T)
 * and the rig from config/fpanim/rig.json or assets/fpanim/animations/rig.json.
 *
 * Name lookup for a state like "fp.idle" holding a diamond sword tries:
 *   fp.idle.minecraft.diamond_sword  ->  fp.idle.sword  ->  fp.idle
 */
public final class AnimationLibrary implements IResourceManagerReloadListener {
    private static final ResourceLocation ANIMS = new ResourceLocation(FPAnimMod.MODID, "animations/firstperson.animation.json");
    private static final ResourceLocation RIG = new ResourceLocation(FPAnimMod.MODID, "animations/rig.json");

    private static Map<String, Animation> animations = Collections.emptyMap();
    private static Rig rig = Rig.defaults();
    private static final Map<String, Animation> cache = new HashMap<>();
    private static final Animation NONE = null;

    public static boolean hasAnimations() {
        return !animations.isEmpty();
    }

    public static Rig rig() {
        return rig;
    }

    /** First match across states (in priority order) for the held item. */
    public static Animation find(List<String> states, ItemStack stack) {
        for (String s : states) {
            Animation a = find(s, stack);
            if (a != null) return a;
        }
        return null;
    }

    public static Animation find(String state, ItemStack stack) {
        String itemId = stack.isEmpty() ? "empty" : String.valueOf(stack.getItem().getRegistryName());
        String key = state + '|' + itemId;
        if (cache.containsKey(key)) return cache.get(key);

        Animation a = animations.get(state + '.' + itemId.replace(':', '.'));
        if (a == null) a = animations.get(state + '.' + category(stack));
        if (a == null) a = animations.get(state);
        cache.put(key, a == null ? NONE : a);
        return a;
    }

    static String category(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        Item item = stack.getItem();
        if (item instanceof ItemSword) return "sword";
        if (item instanceof ItemBow) return "bow";
        if (item instanceof ItemShield) return "shield";
        if (item instanceof ItemTool || item instanceof ItemHoe) return "tool";
        if (item instanceof ItemFood) return "food";
        if (item instanceof ItemBlock) return "block";
        return "item";
    }

    @Override
    public void onResourceManagerReload(IResourceManager rm) {
        Map<String, Animation> map = new HashMap<>();
        Rig newRig = Rig.defaults();

        try {
            for (IResource res : rm.getAllResources(ANIMS)) {
                try (IResource r = res; Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
                    map.putAll(AnimationParser.parse(reader));
                } catch (Exception e) {
                    FPAnimMod.LOG.error("Failed to read {} from '{}'", ANIMS, res.getResourcePackName(), e);
                }
            }
        } catch (FileNotFoundException ignored) {
            // no pack provides it
        } catch (IOException e) {
            FPAnimMod.LOG.error("Failed to list {}", ANIMS, e);
        }

        try (IResource r = rm.getResource(RIG); Reader reader = new InputStreamReader(r.getInputStream(), StandardCharsets.UTF_8)) {
            newRig = Rig.fromJson(new JsonParser().parse(reader).getAsJsonObject());
        } catch (FileNotFoundException ignored) {
        } catch (Exception e) {
            FPAnimMod.LOG.error("Failed to read {}", RIG, e);
        }

        File dir = new File(Loader.instance().getConfigDir(), FPAnimMod.MODID);
        if (!dir.isDirectory() && !dir.mkdirs()) FPAnimMod.LOG.warn("Couldn't create {}", dir);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (files != null) {
            Arrays.sort(files);
            for (File f : files) {
                try (Reader reader = Files.newBufferedReader(f.toPath(), StandardCharsets.UTF_8)) {
                    if (f.getName().equals("rig.json")) {
                        newRig = Rig.fromJson(new JsonParser().parse(reader).getAsJsonObject());
                    } else {
                        map.putAll(AnimationParser.parse(reader));
                    }
                } catch (Exception e) {
                    FPAnimMod.LOG.error("Failed to read {}", f, e);
                }
            }
        }

        animations = map;
        rig = newRig;
        cache.clear();
        FPAnimMod.LOG.info("Loaded {} first-person animations: {}", map.size(), new TreeSet<>(map.keySet()));
    }
}
