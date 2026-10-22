package com.example.fpanim;

import com.example.fpanim.anim.Pose;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.renderer.GlStateManager;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps Blockbench bone transforms onto the first-person camera space.
 *
 * Space: camera at the origin looking toward -Z (Blockbench "north"), +X right, +Y up,
 * 16 pixels per block. Pivots must match the bone origins in blockbench/fp_rig.bbmodel.
 *
 * Signs: Blockbench's Bedrock values flip rotation X/Y and position X relative to its
 * own 3D view, so the defaults undo that. If an axis moves the wrong way in game,
 * override the signs in rig.json instead of re-keying every animation.
 */
public final class Rig {
    private static final float[] ZERO = new float[3];

    final Map<String, float[]> pivots = new HashMap<>();
    float[] rotationSign = {-1f, -1f, 1f};
    float[] positionSign = {-1f, 1f, 1f};

    static Rig defaults() {
        Rig r = new Rig();
        r.pivots.put("arms", new float[]{0f, -8.32f, -11.52f});
        r.pivots.put("right_arm", new float[]{8.96f, -8.32f, -11.52f});
        r.pivots.put("left_arm", new float[]{-8.96f, -8.32f, -11.52f});
        return r;
    }

    /** rig.json: {"pivots": {"right_arm": [x,y,z]}, "rotation_sign": [x,y,z], "position_sign": [x,y,z]} */
    static Rig fromJson(JsonObject json) {
        Rig r = defaults();
        if (json.has("pivots")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("pivots").entrySet()) {
                r.pivots.put(e.getKey(), vec(e.getValue().getAsJsonArray()));
            }
        }
        if (json.has("rotation_sign")) r.rotationSign = vec(json.getAsJsonArray("rotation_sign"));
        if (json.has("position_sign")) r.positionSign = vec(json.getAsJsonArray("position_sign"));
        return r;
    }

    /** Multiplies the current GL matrix by the bone's transform about its pivot. */
    public void apply(String bone, Pose p) {
        if (p.isIdentity()) return;
        float[] pv = pivots.getOrDefault(bone, ZERO);

        GlStateManager.translate(
                (pv[0] + p.pos[0] * positionSign[0]) / 16f,
                (pv[1] + p.pos[1] * positionSign[1]) / 16f,
                (pv[2] + p.pos[2] * positionSign[2]) / 16f);
        // Blockbench bones use ZYX Euler order
        if (p.rot[2] != 0f) GlStateManager.rotate(p.rot[2] * rotationSign[2], 0f, 0f, 1f);
        if (p.rot[1] != 0f) GlStateManager.rotate(p.rot[1] * rotationSign[1], 0f, 1f, 0f);
        if (p.rot[0] != 0f) GlStateManager.rotate(p.rot[0] * rotationSign[0], 1f, 0f, 0f);
        GlStateManager.scale(p.scale[0], p.scale[1], p.scale[2]);
        GlStateManager.translate(-pv[0] / 16f, -pv[1] / 16f, -pv[2] / 16f);
    }

    private static float[] vec(JsonArray a) {
        return new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()};
    }
}
