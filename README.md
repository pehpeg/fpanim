# First Person Animations (Forge 1.12.2)

A client-side mod that replaces the vanilla first-person hand motion with animations you
author in Blockbench. The mod is the engine; the animations are data, so you can keep
iterating on the "pack" without touching Java.

```
fpanim/
├─ blockbench/fp_rig.bbmodel        ← open this in Blockbench and animate
├─ src/main/java/com/example/fpanim
│  ├─ FPAnimMod.java                mod entry, registers everything
│  ├─ FirstPersonRenderer.java      hooks the hand render, detects player state
│  ├─ AnimationLibrary.java         loads animations, F3+T reload, name lookup
│  ├─ Rig.java                      Blockbench space → camera space (pivots, signs)
│  └─ anim/                         parser, keyframe sampling, blending
└─ src/main/resources/assets/fpanim/animations/firstperson.animation.json  ← shipped pack
```

## 1. Build

**On GitHub (no local setup):** every push runs `.github/workflows/build.yml`. Open the
**Actions** tab → latest *Build* run → download **fpanim-jar** under *Artifacts* (it's a
zip containing the jar). Push a tag like `v0.1.0` to also get a GitHub Release with the jar.

**Locally:** needs a JDK 17 to run Gradle; Java 8 for Minecraft is downloaded automatically.

```
./gradlew build        # jar in build/libs/ (use the one without -dev / -sources)
./gradlew runClient    # launch the game with the mod
```

The build setup is the [GregTechCEu Buildscripts](https://github.com/GregTechCEu/Buildscripts)
template (RetroFuturaGradle). Mod name, id and version live in `buildscript.properties`.

## 2. Animate in Blockbench

Open `blockbench/fp_rig.bbmodel` (Bedrock entity format). The scene is laid out as the
game sees it: the small cube at the origin is the **camera**, it looks toward **north (-Z)**,
and the two boxes are your arms.

Bones the mod reads:

| bone        | moves                                 |
|-------------|---------------------------------------|
| `arms`      | both arms together (sway, bob, tilt) |
| `right_arm` | right arm + whatever it holds         |
| `left_arm`  | left arm + whatever it holds          |

Keep the bone **pivots** where they are — they're mirrored in `Rig.java`. If you move one,
update `rig.json` (below).

### Animation names

Blockbench names them `animation.<name>`; the mod strips the prefix.

| name        | type | when                                     |
|-------------|------|------------------------------------------|
| `fp.idle`   | loop | standing still                           |
| `fp.walk`   | loop | moving                                   |
| `fp.sprint` | loop | sprinting                                |
| `fp.sneak`  | loop | sneaking                                 |
| `fp.air`    | hold | in the air                               |
| `fp.use`    | loop/hold | using the item (eating, bow draw, blocking) |
| `fp.swing`  | once | attack / mine click — **replaces** the vanilla swing |
| `fp.equip`  | once | switching items — **replaces** the vanilla lower/raise |
| `fp.jump`   | once | leaving the ground                       |
| `fp.land`   | once | landing                                  |

Looping states are tried in priority order (use → air → sneak → sprint → walk → idle), so a
missing one just falls through to the next.

**Per-item variants:** add a suffix and it takes priority for that item:

- exact item: `fp.swing.minecraft.diamond_sword`, `fp.idle.minecraft.bow`
- category: `.sword` `.tool` `.bow` `.shield` `.food` `.block` `.item` `.empty`

So `fp.swing.sword` covers every sword, and `fp.idle.empty` is the bare-hand idle.

Loop modes map from Blockbench's loop setting: *Loop* → loop, *Play once* → once,
*Hold on last frame* → hold. Linear, Catmull-Rom and Step interpolation all work; Bezier
plays as linear. Molang expressions aren't evaluated — use plain numbers.

## 3. Iterate live

1. In Blockbench: **Animation → Export Animations** (all of them) into
   `run/config/fpanim/` in dev, or `.minecraft/config/fpanim/` in a normal install.
   Any `*.json` there is loaded, and overrides same-named animations from the jar.
2. In game press **F3+T**. The log prints the list of animations it loaded.

When you're happy, copy the export to
`src/main/resources/assets/fpanim/animations/firstperson.animation.json` and build.
Resource packs can ship that same path to swap the whole pack.

## 4. First-run calibration

Blockbench's Bedrock values flip some axes, and the defaults in `Rig.java` undo that.
Check once: key `right_arm` rotation X = 30 and position Y = 4 in a test `fp.idle`, export,
F3+T. The arm should tip and move the same way the Blockbench preview does when you look
north from the camera cube. If an axis is backwards, drop a `rig.json` in `config/fpanim/`:

```json
{
  "rotation_sign": [-1, -1, 1],
  "position_sign": [-1, 1, 1],
  "pivots": {
    "arms":      [0, -8.32, -11.52],
    "right_arm": [8.96, -8.32, -11.52],
    "left_arm":  [-8.96, -8.32, -11.52]
  }
}
```

flip the offending sign, F3+T, and once it's right move the file to
`assets/fpanim/animations/rig.json` so it ships with the mod.

## How it works

`RenderSpecificHandEvent` fires once per visible hand. The mod cancels it, pushes a matrix,
applies the `arms` then `right_arm`/`left_arm` transform about their pivots, and calls
vanilla's `ItemRenderer.renderItemInFirstPerson` itself — so items, enchant glint, maps,
bows and shields all still render through vanilla. When a custom swing or equip is playing,
vanilla's own swing/equip progress is passed as 0 so the two don't stack.

Each arm has two layers: a **base** loop that crossfades (0.2 s) when the state changes, and
an **overlay** one-shot that takes over the bones it animates, fading in and out quickly.
If nothing is authored for the current situation, the event isn't cancelled and you get
plain vanilla.

## Known limits / next steps

- The arm and the held item move together. A separate `item` bone would need rendering the
  arm and item separately (copying parts of `ItemRenderer`), which is the next big step.
- Mods that also cancel `RenderSpecificHandEvent` (some gun and weapon mods) will conflict.
- Walk/sprint play at a fixed rate; scaling by `limbSwingAmount` is an easy upgrade in
  `HandAnimator`.
- Untested in game: the code type-checks against the 1.12.2 API and the blending logic is
  unit-tested, but run it once and do the calibration above.
