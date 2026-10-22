package com.example.fpanim.anim;

import java.util.Map;

public final class Animation {
    public enum Loop { ONCE, LOOP, HOLD }

    public final String name;
    public final float length;
    public final Loop loop;
    private final Map<String, BoneTrack> bones;

    Animation(String name, float length, Loop loop, Map<String, BoneTrack> bones) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.bones = bones;
    }

    public float localTime(float elapsed) {
        if (length <= 0f) return 0f;
        if (loop == Loop.LOOP) return elapsed % length;
        return Math.min(elapsed, length);
    }

    public boolean isFinished(float elapsed) {
        return loop == Loop.ONCE && elapsed >= length;
    }

    /** Writes this animation's pose for {@code bone} into {@code out}. Returns false if the bone isn't animated. */
    public boolean sample(String bone, float elapsed, Pose out) {
        out.reset();
        BoneTrack track = bones.get(bone);
        if (track == null) return false;
        track.sample(localTime(elapsed), out);
        return true;
    }
}
