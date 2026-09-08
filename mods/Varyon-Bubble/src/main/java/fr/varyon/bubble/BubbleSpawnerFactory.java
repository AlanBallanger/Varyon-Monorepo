package fr.varyon.bubble;

import com.hypixel.hytale.protocol.Color;
import com.hypixel.hytale.protocol.EmitShape;
import com.hypixel.hytale.protocol.FXRenderMode;
import com.hypixel.hytale.protocol.Particle;
import com.hypixel.hytale.protocol.ParticleAnimationFrame;
import com.hypixel.hytale.protocol.ParticleRotationInfluence;
import com.hypixel.hytale.protocol.ParticleScaleRatioConstraint;
import com.hypixel.hytale.protocol.ParticleSpawner;
import com.hypixel.hytale.protocol.ParticleSpawnerGroup;
import com.hypixel.hytale.protocol.ParticleSystem;
import com.hypixel.hytale.protocol.ParticleUVOption;
import com.hypixel.hytale.protocol.Range;
import com.hypixel.hytale.protocol.RangeVector2f;
import com.hypixel.hytale.protocol.Rangef;
import com.hypixel.hytale.protocol.Size;
import com.hypixel.hytale.protocol.SoftParticle;
import com.hypixel.hytale.protocol.UpdateType;
import com.hypixel.hytale.protocol.packets.assets.UpdateParticleSpawners;
import com.hypixel.hytale.protocol.packets.assets.UpdateParticleSystems;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Map;
import org.joml.Vector3f;

public class BubbleSpawnerFactory {
   private static final String TEX_SMALL = "Particles/Textures/Shapes/Thoughts/EllipsisThought_SmallStart.png";
   private static final String TEX_MEDIUM = "Particles/Textures/Shapes/Thoughts/EllipsisThought_MediumStart.png";

   public static ColorRegistration createRegistration(int netId, Color tint) {
      String base = "BB" + netId;
      Map<String, ParticleSpawner> spawners = new HashMap<>();
      Map<String, ParticleSystem> systems = new HashMap<>();

      String introSmall = base + "_Intro_Small";
      spawners.put(introSmall, createIntroSmallSpawner(introSmall, tint));
      systems.put(introSmall, createSystem(introSmall, 1.0F, new Vector3f(0.0F, -0.55F, 0.0F)));

      String introMedium = base + "_Intro_Medium";
      spawners.put(introMedium, createIntroMediumSpawner(introMedium, tint));
      systems.put(introMedium, createSystem(introMedium, 1.0F, new Vector3f(0.0F, -0.4F, 0.0F)));

      for (BubbleState state : BubbleState.values()) {
         if (state != BubbleState.IDLE) {
            String texName = state.getTextureName();
            String texPath = state.getTexturePath();

            String main = base + "_" + texName;
            spawners.put(main, createMainSpawner(main, texPath, tint));
            systems.put(main, createSystem(main, 5.0F, null));

            String loop = base + "_" + texName + "_Loop";
            spawners.put(loop, createLoopSpawner(loop, texPath, tint));
            systems.put(loop, createSystem(loop, 5.0F, null));

            String fadeout = base + "_" + texName + "_Fadeout";
            spawners.put(fadeout, createFadeoutSpawner(fadeout, texPath, tint));
            systems.put(fadeout, createSystem(fadeout, 0.5F, null));

            for (int i = 0; i < 4; i++) {
               String frame = base + "_" + texName + "_Loop_F" + i;
               String framePath = state.getFrameTexturePath(i);
               spawners.put(frame, createStaticFrameSpawner(frame, framePath, tint));
               systems.put(frame, createSystem(frame, 1.5F, null));
            }
         }
      }

      UpdateParticleSpawners spawnerPacket = new UpdateParticleSpawners(UpdateType.AddOrUpdate, spawners, null);
      UpdateParticleSystems systemPacket = new UpdateParticleSystems(UpdateType.AddOrUpdate, systems, null);
      return new ColorRegistration(spawnerPacket, systemPacket, base, tint);
   }

   private static ParticleSpawner createIntroSmallSpawner(String name, Color tint) {
      Int2ObjectOpenHashMap<ParticleAnimationFrame> frames = new Int2ObjectOpenHashMap<>();
      frames.put(0, frame(0, 0.03F, tint, 1.0F));
      frames.put(5, frame(0, 0.283F, tint, 1.0F));
      frames.put(86, frame(0, 0.283F, tint, 1.0F));
      frames.put(93, frame(0, 0.17F, tint, 0.6F));
      frames.put(100, frame(0, 0.03F, tint, 0.0F));
      Particle particle = makeParticle(TEX_SMALL, 512, 512, ParticleScaleRatioConstraint.OneToOne, frame(0, 0.03F, tint, 1.0F), frames);
      return makeSpawner(name, particle, 0.667F, new Rangef(0.778F, 0.778F));
   }

   private static ParticleSpawner createIntroMediumSpawner(String name, Color tint) {
      Int2ObjectOpenHashMap<ParticleAnimationFrame> frames = new Int2ObjectOpenHashMap<>();
      frames.put(0, frame(0, 0.03F, tint, 1.0F));
      frames.put(5, frame(0, 0.48F, tint, 1.0F));
      frames.put(86, frame(0, 0.48F, tint, 1.0F));
      frames.put(93, frame(0, 0.288F, tint, 0.6F));
      frames.put(100, frame(0, 0.03F, tint, 0.0F));
      Particle particle = makeParticle(TEX_MEDIUM, 512, 512, ParticleScaleRatioConstraint.OneToOne, frame(0, 0.03F, tint, 1.0F), frames);
      return makeSpawner(name, particle, 0.667F, new Rangef(0.778F, 0.778F));
   }

   private static ParticleSpawner createMainSpawner(String name, String texturePath, Color tint) {
      Int2ObjectOpenHashMap<ParticleAnimationFrame> frames = new Int2ObjectOpenHashMap<>();
      frames.put(0, frame(0, 0.5F, tint, 1.0F));
      frames.put(2, frame(0, 0.55F, tint, 1.0F));
      frames.put(5, frame(0, 0.5F, tint, 1.0F));
      frames.put(25, frame(1, 0.5F, tint, 1.0F));
      frames.put(50, frame(2, 0.5F, tint, 1.0F));
      frames.put(75, frame(3, 0.5F, tint, 1.0F));
      frames.put(96, frame(3, 0.5F, tint, 1.0F));
      frames.put(100, frame(3, 0.0F, tint, 0.0F));
      Particle particle = makeParticle(texturePath, 160, 160, ParticleScaleRatioConstraint.OneToOne, frame(0, 0.5F, tint, 1.0F), frames);
      return makeSpawner(name, particle, 5.0F, new Rangef(4.0F, 4.0F));
   }

   private static ParticleSpawner createLoopSpawner(String name, String texturePath, Color tint) {
      Int2ObjectOpenHashMap<ParticleAnimationFrame> frames = new Int2ObjectOpenHashMap<>();
      frames.put(0, frame(3, 0.5F, tint, 1.0F));
      frames.put(11, frame(0, 0.5F, tint, 1.0F));
      frames.put(31, frame(1, 0.5F, tint, 1.0F));
      frames.put(51, frame(2, 0.5F, tint, 1.0F));
      frames.put(71, frame(3, 0.5F, tint, 1.0F));
      frames.put(90, frame(3, 0.5F, tint, 1.0F));
      frames.put(100, frame(3, 0.5F, tint, 0.0F));
      Particle particle = makeParticle(texturePath, 160, 160, ParticleScaleRatioConstraint.OneToOne, frame(3, 0.5F, tint, 1.0F), frames);
      return makeSpawner(name, particle, 5.0F, new Rangef(5.0F, 5.0F));
   }

   private static ParticleSpawner createFadeoutSpawner(String name, String texturePath, Color tint) {
      Int2ObjectOpenHashMap<ParticleAnimationFrame> frames = new Int2ObjectOpenHashMap<>();
      frames.put(0, frame(3, 0.5F, tint, 1.0F));
      frames.put(100, frame(3, 0.0F, tint, 0.0F));
      Particle particle = makeParticle(texturePath, 160, 160, ParticleScaleRatioConstraint.OneToOne, frame(3, 0.5F, tint, 1.0F), frames);
      return makeSpawner(name, particle, 0.5F, new Rangef(0.5F, 0.5F));
   }

   private static ParticleSpawner createStaticFrameSpawner(String name, String texturePath, Color tint) {
      Int2ObjectOpenHashMap<ParticleAnimationFrame> frames = new Int2ObjectOpenHashMap<>();
      frames.put(0, frame(0, 0.5F, tint, 1.0F));
      frames.put(80, frame(0, 0.5F, tint, 1.0F));
      frames.put(100, frame(0, 0.5F, tint, 0.0F));
      Particle particle = makeParticle(texturePath, 160, 160, ParticleScaleRatioConstraint.OneToOne, frame(0, 0.5F, tint, 1.0F), frames);
      return makeSpawner(name, particle, 1.5F, new Rangef(1.5F, 1.5F));
   }

   private static ParticleAnimationFrame frame(int frameIdx, float scale, Color tint, float opacity) {
      Range range = new Range(frameIdx, frameIdx);
      Rangef scaleRange = new Rangef(scale, scale);
      RangeVector2f scaleVec = new RangeVector2f(scaleRange, scaleRange);
      return new ParticleAnimationFrame(range, scaleVec, null, tint, opacity);
   }

   private static Particle makeParticle(
      String texturePath,
      int texW,
      int texH,
      ParticleScaleRatioConstraint constraint,
      ParticleAnimationFrame initFrame,
      Int2ObjectOpenHashMap<ParticleAnimationFrame> frames
   ) {
      return new Particle(
         texturePath,
         new Size(texW, texH),
         ParticleUVOption.None,
         constraint,
         SoftParticle.Disable,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         0.0F,
         false,
         initFrame,
         null,
         frames
      );
   }

   private static ParticleSpawner makeSpawner(String name, Particle particle, float lifeSpan, Rangef particleLifeSpan) {
      return new ParticleSpawner(
         name,
         particle,
         EmitShape.Sphere,
         null,
         0.0F,
         false,
         lifeSpan,
         null,
         true,
         null,
         null,
         1,
         null,
         0.0F,
         ParticleRotationInfluence.Billboard,
         false,
         false,
         0.0F,
         1.0F,
         null,
         FXRenderMode.BlendLinear,
         0.0F,
         true,
         particleLifeSpan,
         null,
         null,
         null
      );
   }

   private static ParticleSystem createSystem(String name, float lifeSpan, Vector3f positionOffset) {
      ParticleSpawnerGroup group = new ParticleSpawnerGroup(name, positionOffset, null, false, 0.0F, null, null, 1, 1, null, null, null, null);
      return new ParticleSystem(name, new ParticleSpawnerGroup[]{group}, lifeSpan, 1000.0F, 1000.0F, false);
   }

   public static class ColorRegistration {
      public final UpdateParticleSpawners spawnerPacket;
      public final UpdateParticleSystems systemPacket;
      public final String prefix;
      public final Color tint;

      ColorRegistration(UpdateParticleSpawners spawnerPacket, UpdateParticleSystems systemPacket, String prefix, Color tint) {
         this.spawnerPacket = spawnerPacket;
         this.systemPacket = systemPacket;
         this.prefix = prefix;
         this.tint = tint;
      }
   }
}
