package com.example.fpanim.anim;

/** A bone transform in Blockbench (Bedrock) units: degrees, pixels, scale factor. */
public final class Pose {
    public final float[] rot = new float[3];
    public final float[] pos = new float[3];
    public final float[] scale = {1f, 1f, 1f};

    public void reset() {
        for (int i = 0; i < 3; i++) {
            rot[i] = 0f;
            pos[i] = 0f;
            scale[i] = 1f;
        }
    }

    public void set(Pose o) {
        for (int i = 0; i < 3; i++) {
            rot[i] = o.rot[i];
            pos[i] = o.pos[i];
            scale[i] = o.scale[i];
        }
    }

    /** Moves this pose toward {@code o} by weight w (0 = unchanged, 1 = becomes o). */
    public void lerpTo(Pose o, float w) {
        if (w <= 0f) return;
        if (w >= 1f) { set(o); return; }
        for (int i = 0; i < 3; i++) {
            rot[i] += (o.rot[i] - rot[i]) * w;
            pos[i] += (o.pos[i] - pos[i]) * w;
            scale[i] += (o.scale[i] - scale[i]) * w;
        }
    }

    public boolean isIdentity() {
        for (int i = 0; i < 3; i++) {
            if (rot[i] != 0f || pos[i] != 0f || scale[i] != 1f) return false;
        }
        return true;
    }
}
