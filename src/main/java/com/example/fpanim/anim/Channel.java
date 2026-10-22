package com.example.fpanim.anim;

/** One animated property (rotation, position or scale) of a bone: a list of keyframes. */
public final class Channel {
    public enum Lerp { LINEAR, CATMULLROM, STEP }

    private final float[] times;
    private final float[][] pre;   // value arriving at the keyframe
    private final float[][] post;  // value leaving the keyframe (differs only for "pre/post" keyframes)
    private final Lerp[] lerp;

    public Channel(float[] times, float[][] pre, float[][] post, Lerp[] lerp) {
        this.times = times;
        this.pre = pre;
        this.post = post;
        this.lerp = lerp;
    }

    public float lastTime() {
        return times.length == 0 ? 0f : times[times.length - 1];
    }

    public void sample(float t, float[] out) {
        int n = times.length;
        if (n == 0) return;
        if (t <= times[0]) { copy(n == 1 ? post[0] : pre[0], out); return; }
        if (t >= times[n - 1]) { copy(post[n - 1], out); return; }

        int i = 0;
        while (i < n - 2 && t >= times[i + 1]) i++;

        float t0 = times[i], t1 = times[i + 1];
        float a = t1 > t0 ? (t - t0) / (t1 - t0) : 1f;
        float[] from = post[i];
        float[] to = pre[i + 1];

        if (lerp[i] == Lerp.STEP) {
            copy(from, out);
        } else if (lerp[i] == Lerp.CATMULLROM || lerp[i + 1] == Lerp.CATMULLROM) {
            float[] p0 = i > 0 ? post[i - 1] : from;
            float[] p3 = i + 2 < n ? pre[i + 2] : to;
            for (int k = 0; k < 3; k++) out[k] = catmull(p0[k], from[k], to[k], p3[k], a);
        } else {
            for (int k = 0; k < 3; k++) out[k] = from[k] + (to[k] - from[k]) * a;
        }
    }

    private static float catmull(float p0, float p1, float p2, float p3, float t) {
        float t2 = t * t, t3 = t2 * t;
        return 0.5f * ((2f * p1) + (-p0 + p2) * t
                + (2f * p0 - 5f * p1 + 4f * p2 - p3) * t2
                + (-p0 + 3f * p1 - 3f * p2 + p3) * t3);
    }

    private static void copy(float[] src, float[] dst) {
        dst[0] = src[0];
        dst[1] = src[1];
        dst[2] = src[2];
    }
}
