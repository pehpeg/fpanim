package com.example.fpanim.anim;

/** The rotation/position/scale channels for one bone in one animation. Any channel may be null. */
public final class BoneTrack {
    final Channel rotation;
    final Channel position;
    final Channel scale;

    BoneTrack(Channel rotation, Channel position, Channel scale) {
        this.rotation = rotation;
        this.position = position;
        this.scale = scale;
    }

    float lastTime() {
        float t = 0f;
        if (rotation != null) t = Math.max(t, rotation.lastTime());
        if (position != null) t = Math.max(t, position.lastTime());
        if (scale != null) t = Math.max(t, scale.lastTime());
        return t;
    }

    void sample(float t, Pose out) {
        if (rotation != null) rotation.sample(t, out.rot);
        if (position != null) position.sample(t, out.pos);
        if (scale != null) scale.sample(t, out.scale);
    }
}
