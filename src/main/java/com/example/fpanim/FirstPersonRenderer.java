package com.example.fpanim;

import com.example.fpanim.anim.Animation;
import com.example.fpanim.anim.HandAnimator;
import com.example.fpanim.anim.Pose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Takes over the vanilla first-person hand render: picks animations from the player's
 * state, wraps the vanilla call in the animated transform, and hides vanilla's own swing
 * or equip motion while a custom one is playing.
 */
public final class FirstPersonRenderer {
    private final HandAnimator right = new HandAnimator();
    private final HandAnimator left = new HandAnimator();
    private final Pose pose = new Pose();
    private final List<String> states = new ArrayList<>(6);

    private long ticks;
    private boolean primed;
    private boolean wasOnGround = true;
    private boolean wasSwinging;
    private int lastSwingInt;
    private Item lastMain, lastOff;
    private int lastSlot = -1;

    private float now(float partialTicks) {
        return (ticks + partialTicks) / 20f;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;
        if (p == null) { primed = false; return; }
        if (mc.isGamePaused()) return;

        ticks++;
        float now = now(0f);
        Item main = p.getHeldItemMainhand().getItem();
        Item off = p.getHeldItemOffhand().getItem();

        if (primed) {
            // jump / land
            if (wasOnGround && !p.onGround && p.motionY > 0) playBoth(p, "fp.jump", now);
            else if (!wasOnGround && p.onGround && !p.capabilities.isFlying) playBoth(p, "fp.land", now);

            // swing (restarts on every new click, like vanilla)
            boolean swingStarted = p.isSwingInProgress && (!wasSwinging || p.swingProgressInt < lastSwingInt);
            if (swingStarted) {
                play(p, p.swingingHand != null ? p.swingingHand : EnumHand.MAIN_HAND, "fp.swing", "swing", now);
            }

            // equip
            if (main != lastMain || p.inventory.currentItem != lastSlot) play(p, EnumHand.MAIN_HAND, "fp.equip", "equip", now);
            if (off != lastOff) play(p, EnumHand.OFF_HAND, "fp.equip", "equip", now);
        }

        primed = true;
        wasOnGround = p.onGround;
        wasSwinging = p.isSwingInProgress;
        lastSwingInt = p.swingProgressInt;
        lastMain = main;
        lastOff = off;
        lastSlot = p.inventory.currentItem;
    }

    @SubscribeEvent
    public void onRenderHand(RenderSpecificHandEvent e) {
        if (!AnimationLibrary.hasAnimations()) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP p = mc.player;
        if (p == null) return;

        EnumHand hand = e.getHand();
        EnumHandSide side = sideOf(p, hand);
        HandAnimator anim = side == EnumHandSide.RIGHT ? right : left;
        float now = now(e.getPartialTicks());
        ItemStack stack = e.getItemStack();

        anim.setBase(AnimationLibrary.find(baseStates(p, hand), stack), now);
        if (!anim.isActive(now)) return; // nothing authored for this situation: plain vanilla

        float swing = anim.isPlaying("swing", now) ? 0f : e.getSwingProgress();
        float equip = anim.isPlaying("equip", now) ? 0f : e.getEquipProgress();
        Rig rig = AnimationLibrary.rig();

        e.setCanceled(true);
        GlStateManager.pushMatrix();
        anim.compute("arms", now, pose);
        rig.apply("arms", pose);
        String bone = side == EnumHandSide.RIGHT ? "right_arm" : "left_arm";
        anim.compute(bone, now, pose);
        rig.apply(bone, pose);
        mc.getItemRenderer().renderItemInFirstPerson(p, e.getPartialTicks(), e.getInterpolatedPitch(), hand, swing, stack, equip);
        GlStateManager.popMatrix();
    }

    /** Looping states in priority order; the first one with an animation for this item wins. */
    private List<String> baseStates(EntityPlayerSP p, EnumHand hand) {
        states.clear();
        if (p.isHandActive() && p.getActiveHand() == hand) states.add("fp.use");
        if (!p.onGround && !p.capabilities.isFlying && !p.isInWater()) states.add("fp.air");
        if (p.isSneaking()) states.add("fp.sneak");
        boolean moving = Math.abs(p.moveForward) > 0.01f || Math.abs(p.moveStrafing) > 0.01f;
        if (moving && p.isSprinting()) states.add("fp.sprint");
        if (moving) states.add("fp.walk");
        states.add("fp.idle");
        return states;
    }

    private void play(EntityPlayerSP p, EnumHand hand, String state, String tag, float now) {
        Animation a = AnimationLibrary.find(state, p.getHeldItem(hand));
        if (a == null) return;
        (sideOf(p, hand) == EnumHandSide.RIGHT ? right : left).play(a, tag, now);
    }

    private void playBoth(EntityPlayerSP p, String state, float now) {
        play(p, EnumHand.MAIN_HAND, state, state, now);
        play(p, EnumHand.OFF_HAND, state, state, now);
    }

    private static EnumHandSide sideOf(EntityPlayerSP p, EnumHand hand) {
        return hand == EnumHand.MAIN_HAND ? p.getPrimaryHand() : p.getPrimaryHand().opposite();
    }
}
