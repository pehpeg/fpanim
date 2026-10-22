package com.example.fpanim.anim;

import com.example.fpanim.FPAnimMod;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the Bedrock animation format that Blockbench writes with "Export Animations"
 * (format_version 1.8.0). Molang expressions are not evaluated: a string that isn't a
 * plain number reads as 0 and logs a warning.
 */
public final class AnimationParser {
    private AnimationParser() {}

    public static Map<String, Animation> parse(Reader reader) {
        Map<String, Animation> result = new HashMap<>();
        JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
        JsonObject anims = root.getAsJsonObject("animations");
        if (anims == null) return result;

        for (Map.Entry<String, JsonElement> e : anims.entrySet()) {
            String name = e.getKey().startsWith("animation.") ? e.getKey().substring("animation.".length()) : e.getKey();
            try {
                result.put(name, parseAnimation(name, e.getValue().getAsJsonObject()));
            } catch (RuntimeException ex) {
                FPAnimMod.LOG.error("Skipping animation '{}': {}", e.getKey(), ex.toString());
            }
        }
        return result;
    }

    private static Animation parseAnimation(String name, JsonObject json) {
        Animation.Loop loop = Animation.Loop.ONCE;
        JsonElement l = json.get("loop");
        if (l != null && l.isJsonPrimitive()) {
            JsonPrimitive p = l.getAsJsonPrimitive();
            if (p.isBoolean() && p.getAsBoolean()) loop = Animation.Loop.LOOP;
            else if (p.isString() && "hold_on_last_frame".equals(p.getAsString())) loop = Animation.Loop.HOLD;
        }

        Map<String, BoneTrack> bones = new HashMap<>();
        float maxTime = 0f;
        JsonObject bonesJson = json.getAsJsonObject("bones");
        if (bonesJson != null) {
            for (Map.Entry<String, JsonElement> b : bonesJson.entrySet()) {
                JsonObject bj = b.getValue().getAsJsonObject();
                BoneTrack track = new BoneTrack(
                        parseChannel(bj.get("rotation"), name),
                        parseChannel(bj.get("position"), name),
                        parseChannel(bj.get("scale"), name));
                bones.put(b.getKey(), track);
                maxTime = Math.max(maxTime, track.lastTime());
            }
        }

        float length = json.has("animation_length") ? json.get("animation_length").getAsFloat() : maxTime;
        return new Animation(name, length, loop, bones);
    }

    private static Channel parseChannel(JsonElement e, String anim) {
        if (e == null || e.isJsonNull()) return null;

        // A constant: "rotation": [0, 10, 0]  or  "scale": 1.2
        if (e.isJsonArray() || e.isJsonPrimitive()) {
            float[] v = vec(e, anim);
            return new Channel(new float[]{0f}, new float[][]{v}, new float[][]{v}, new Channel.Lerp[]{Channel.Lerp.LINEAR});
        }

        // Keyframes: { "0.0": [..], "0.5": {"pre": [..], "post": [..], "lerp_mode": "catmullrom"} }
        List<Map.Entry<String, JsonElement>> keys = new ArrayList<>(e.getAsJsonObject().entrySet());
        keys.sort((a, b) -> Float.compare(Float.parseFloat(a.getKey()), Float.parseFloat(b.getKey())));

        int n = keys.size();
        float[] times = new float[n];
        float[][] pre = new float[n][];
        float[][] post = new float[n][];
        Channel.Lerp[] lerp = new Channel.Lerp[n];

        for (int i = 0; i < n; i++) {
            times[i] = Float.parseFloat(keys.get(i).getKey());
            JsonElement v = keys.get(i).getValue();
            lerp[i] = Channel.Lerp.LINEAR;
            if (v.isJsonObject()) {
                JsonObject o = v.getAsJsonObject();
                JsonElement p = o.has("pre") ? o.get("pre") : o.get("post");
                JsonElement q = o.has("post") ? o.get("post") : o.get("pre");
                pre[i] = vec(p, anim);
                post[i] = vec(q, anim);
                if (o.has("lerp_mode")) {
                    String mode = o.get("lerp_mode").getAsString();
                    if ("catmullrom".equals(mode)) lerp[i] = Channel.Lerp.CATMULLROM;
                    else if ("step".equals(mode)) lerp[i] = Channel.Lerp.STEP;
                }
            } else {
                pre[i] = post[i] = vec(v, anim);
            }
        }
        return new Channel(times, pre, post, lerp);
    }

    private static float[] vec(JsonElement e, String anim) {
        if (e == null) return new float[3];
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            float[] v = new float[3];
            for (int i = 0; i < 3 && i < a.size(); i++) v[i] = num(a.get(i), anim);
            return v;
        }
        float f = num(e, anim);
        return new float[]{f, f, f};
    }

    private static float num(JsonElement e, String anim) {
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isNumber()) return p.getAsFloat();
        String s = p.getAsString().trim();
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException ex) {
            FPAnimMod.LOG.warn("Animation '{}': Molang '{}' isn't supported, using 0", anim, s);
            return 0f;
        }
    }
}
