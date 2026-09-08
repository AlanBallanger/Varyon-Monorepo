package fr.varyon.bubble;

public enum BubbleState {
   INVENTORY("Thinking_Inventory", "Inventory"),
   CONTAINER("Thinking_Container", "Container"),
   PAUSED("Thinking_Paused", "Paused"),
   MAP("Thinking_Map", "Map"),
   MENU("Thinking_Menu", "Menu"),
   BASE("Thinking_Base", "Base"),
   IDLE("Thinking_Base", "Base");

   private static final String TEX_PREFIX = "Particles/Textures/Shapes/Thoughts/EllipsisThought_";
   private final String particleSystem;
   private final String loopParticleSystem;
   private final String fadeoutParticleSystem;
   private final String textureName;

   BubbleState(String particleSystem, String textureName) {
      this.particleSystem = particleSystem;
      this.loopParticleSystem = particleSystem + "_Loop";
      this.fadeoutParticleSystem = particleSystem + "_Fadeout";
      this.textureName = textureName;
   }

   public String getTextureName() {
      return this.textureName;
   }

   public String getTexturePath() {
      return TEX_PREFIX + this.textureName + ".png";
   }

   public String getFrameTexturePath(int frame) {
      return TEX_PREFIX + this.textureName + "_Frame" + frame + ".png";
   }

   public String getParticleSystem() {
      return this.particleSystem;
   }

   public String getLoopParticleSystem() {
      return this.loopParticleSystem;
   }

   public String getFadeoutParticleSystem() {
      return this.fadeoutParticleSystem;
   }

   public String getLoopFrameSystem(int frame) {
      return this.particleSystem + "_Loop_F" + frame;
   }
}
