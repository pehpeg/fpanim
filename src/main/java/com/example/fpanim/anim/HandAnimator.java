package com.example.fpanim.anim;

/**
 * Animation state for one arm. Two layers:
 *  - base: a looping state (idle, walk, sprint...) that crossfades when it changes
 *  - overlay: a one-shot action (swing, equip, jump...) that takes over the bones it animates
 */
public final class HandAnimator {
    private static final float BASE_FADE = 0.2f;
    private static final float OVERLAY_FADE_IN = 0.05f;
    private static final float OVERLAY_FADE_OUT = 0.1f;

    private Animation base, prevBase;
    private float baseStart, prevBaseStart, fadeStart = -1000f;

    private Animation overlay;
    private String overlayTag;
    private float overlayStart;

    private final Pose tmpPrev = new Pose();
    private final Pose tmpOverlay = new Pose();

    public void setBase(Animation a, float now) {
        if (a == base) return;
        prevBase = base;
        prevBaseStart = baseStart;
        base = a;
        baseStart = now;
        fadeStart = now;
    }

    public void play(Animation a, String tag, float now) {
        overlay = a;
        overlayTag = tag;
        overlayStart = now;
    }

    public boolean isPlaying(String tag, float now) {
        expire(now);
        return overlay != null && tag.equals(overlayTag);
    }

    public boolean isActive(float now) {
        expire(now);
        return base != null || overlay != null || now - fadeStart < BASE_FADE;
    }

    public void compute(String bone, float now, Pose out) {
        expire(now);

        if (base != null) base.sample(bone, now - baseStart, out);
        else out.reset();

        float w = (now - fadeStart) / BASE_FADE;
        if (w < 1f) {
            if (prevBase != null) prevBase.sample(bone, now - prevBaseStart, tmpPrev);
            else tmpPrev.reset();
            tmpPrev.lerpTo(out, Math.max(w, 0f));
            out.set(tmpPrev);
        } else {
            prevBase = null;
        }

        if (overlay != null && overlay.sample(bone, now - overlayStart, tmpOverlay)) {
            out.lerpTo(tmpOverlay, overlayWeight(now));
        }
    }

    private float overlayWeight(float now) {
        float e = now - overlayStart;
        float w = Math.min(1f, e / OVERLAY_FADE_IN);
        if (overlay.loop == Animation.Loop.ONCE) {
            w = Math.min(w, (overlay.length - e) / OVERLAY_FADE_OUT);
        }
        return Math.max(0f, Math.min(1f, w));
    }

    private void expire(float now) {
        if (overlay != null && (now < overlayStart || overlay.isFinished(now - overlayStart))) {
            overlay = null;
            overlayTag = null;
        }
    }
}
