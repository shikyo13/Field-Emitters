package com.zerotheabsolute.fieldemitters.client.tutorial;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.zerotheabsolute.fieldemitters.*;
import com.zerotheabsolute.fieldemitters.client.FieldRenderer;
import com.zeromods.core.animation.PlanarProjection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Read-only dioramas share the placed models and the gameplay field renderer. */
final class FieldTutorialScenes {
  private static final int WIDTH = 480, HEIGHT = 180;
  private static final int SPAN = 8, POST_HEIGHT = 5;
  private static final float WALL_SCALE = 22, DOME_SCALE = 9;
  private static final float TICKS_PER_SECOND = 20;
  private static final int SETTLED_TICKS = 100;
  private static final double DEMONSTRATION_DELAY_SECONDS = .75;
  private static final float DEMONSTRATION_SPEED = 1.6f;
  private static final double RESULT_HOLD_SECONDS = .75;
  private static final float WALK_START_SECONDS = 1, WALK_DURATION_SECONDS = 4;
  private static final float DAMAGE_END_SECONDS = 5.5f;
  private static final float SHUTDOWN_START_SECONDS = 3;
  private static final FieldRenderer FIELDS = new FieldRenderer(null);
  private final com.zerotheabsolute.fieldemitters.client.TutorialTunerPreview tuner =
      new com.zerotheabsolute.fieldemitters.client.TutorialTunerPreview();
  private ClientLevel sceneLevel;
  private LivingEntity zombie, secondZombie, sheep, visitor, baby;

  FieldTutorialScenes() {}

  static double minimumSeconds(String chapter, int step) {
    double animation = switch (chapter) {
      case "blocking", "directions", "age", "items", "cards", "checkpoint" -> WALK_START_SECONDS + WALK_DURATION_SECONDS;
      case "sensor" -> WALK_START_SECONDS + WALK_DURATION_SECONDS + (step == 2 ? .25 : 0);
      case "damage" -> DAMAGE_END_SECONDS;
      case "rails" -> step == 2 ? WALK_START_SECONDS + WALK_DURATION_SECONDS : 0;
      case "formations" -> PlanarProjection.DURATION_TICKS / TICKS_PER_SECOND;
      case "setup", "terrain" -> step == 1 ? PlanarProjection.DURATION_TICKS / TICKS_PER_SECOND : 0;
      case "towers" -> step == 1 ? SphereField.FORMATION_TICKS / TICKS_PER_SECOND : 0;
      case "trouble" -> step == 3 ? SHUTDOWN_START_SECONDS + FieldShutdown.DURATION_TICKS / TICKS_PER_SECOND : 0;
      default -> 0;
    };
    return animation == 0 ? 0 : DEMONSTRATION_DELAY_SECONDS + animation / DEMONSTRATION_SPEED + RESULT_HOLD_SECONDS;
  }

  void render(GuiGraphics graphics, String chapter, int step, double seconds, boolean controls) {
    var mc = Minecraft.getInstance();
    if (mc.level == null) return;
    if (controls) {
      tuner.render(graphics, chapter, seconds);
      return;
    }
    actors(mc.level);
    visitor.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    visitor.hurtTime = zombie.hurtTime = sheep.hurtTime = baby.hurtTime = 0;
    float local = (float) Math.max(0, seconds - DEMONSTRATION_DELAY_SECONDS) * DEMONSTRATION_SPEED;
    float ticks = local * TICKS_PER_SECOND;
    boolean dome = chapter.equals("towers");
    boolean perimeter = chapter.equals("networks") || chapter.equals("automation") || chapter.equals("directions") && step == 3;
    boolean bridge = chapter.equals("rails") && step != 3;
    boolean active = !(chapter.equals("rails") && step < 2)
        && !(chapter.equals("setup") && step == 0)
        && !(chapter.equals("trouble") && step == 0)
        && !(chapter.equals("terrain") && (step == 0 || step == 3));
    boolean shutdown = chapter.equals("trouble") && step == 3 && local >= SHUTDOWN_START_SECONDS;
    graphics.fill(0, 0, WIDTH, HEIGHT, 0xFF0B1D2B);
    graphics.flush();
    var pose = graphics.pose();
    pose.pushPose();
    try {
      // All three axes use the same world scale, including the depth axis inherited from the GUI.
      pose.translate(WIDTH / 2.0, dome ? (step == 3 ? 89 : 88) : perimeter ? 128 : bridge ? 90 : 124, 250);
      float scale = dome ? DOME_SCALE : perimeter ? 24 : chapter.equals("terrain") ? 20 : WALL_SCALE;
      pose.scale(scale, -scale, scale);
      pose.mulPose(Axis.XP.rotationDegrees(perimeter ? 12 : 18));
      pose.mulPose(Axis.YP.rotationDegrees(-24));
      pose.translate(dome ? -.5 : -(SPAN + 1) / 2.0, 0, perimeter ? 1 : -.5);
      Lighting.setupFor3DItems();
      var fields = new ArrayList<EmitterEntity>();
      if (dome) {
        if (step == 3) platform(graphics, -1, 1, -1, 1);
        else domeTerrain(graphics, step == 2);
        var tower = emitter(BlockPos.ZERO, FieldEmitters.TOWER.get().defaultBlockState(), active);
        tower.controls.sphereRadius = SphereField.MIN_RADIUS;
        tower.controls.dome = step != 3;
        tower.transition = 0;
        tower.controls.visible = step > 0;
        structure(graphics, tower);
        fields.add(tower);
      } else if (chapter.equals("rails")) {
        rails(graphics, fields, bridge, step, active, local);
      } else {
        if (chapter.equals("terrain")) terrain(graphics, active);
        else if (perimeter) platform(graphics, -1, SPAN + 1, -4, 1);
        else platform(graphics, -1, SPAN + 1, -3, 3);
        var left = emitter(BlockPos.ZERO, FieldEmitters.EMITTER.get().defaultBlockState(), active);
        var right = emitter(new BlockPos(SPAN, 0, 0), FieldEmitters.EMITTER.get().defaultBlockState(), active);
        int[] ground = new int[SPAN + 1];
        if (chapter.equals("terrain")) for (int x = 2; x < SPAN - 1; x++) ground[x] = -1;
        left.links = List.of(new EmitterEntity.Link(right.getBlockPos(), 1, 0, ground));
        if (chapter.equals("appearance") || chapter.equals("formations")) {
          int[] palette = {0x52E5FF, 0x9365FF, 0xFF65C8, 0x54E5A5};
          left.color = right.color = palette[step];
          left.controls.pattern = step;
          left.controls.formation = chapter.equals("formations") ? step + 4 : 0;
          if (chapter.equals("formations")) left.controls.pattern = 0;
        }
        left.powered = right.powered = active && !shutdown;
        left.transition = right.transition = shutdown ? (long) (SHUTDOWN_START_SECONDS * TICKS_PER_SECOND) : 0;
        structure(graphics, left);
        structure(graphics, right);
        fields.add(left);
        fields.add(right);
        details(graphics, chapter, step, local, left, fields);
      }
      // Flush opaque geometry first, so translucent fields depth-test against the actual models.
      graphics.flush();
      boolean forming = chapter.equals("setup") && step == 1 || dome && step == 1
          || chapter.equals("formations") || chapter.equals("terrain") && step == 1 || chapter.equals("trouble");
      float clock = ticks + (forming ? 0 : SETTLED_TICKS);
      for (var emitter : fields) {
        emitter.network = fields;
        if (!forming && !emitter.impactWaves.isEmpty()) {
          var waves = new ArrayList<FieldImpacts.Wave>();
          for (var wave : emitter.impactWaves)
            waves.add(new FieldImpacts.Wave(wave.position(), wave.time() + SETTLED_TICKS));
          emitter.impactWaves.clear();
          emitter.impactWaves.addAll(waves);
        }
        pose.pushPose();
        try {
          var pos = emitter.getBlockPos();
          pose.translate(pos.getX(), pos.getY(), pos.getZ());
          FIELDS.renderPreview(emitter, active ? clock : 200, pose, graphics.bufferSource());
        } finally { pose.popPose(); }
      }
      graphics.flush();
    } finally {
      graphics.flush();
      pose.popPose();
      Lighting.setupFor3DItems();
    }
    if (dome) graphics.drawString(mc.font, FieldTutorial.text(step == 3 ? "sphere_label" : "dome_label"),
        8, 5, 0xFFE2F5FF, false);
  }

  private EmitterEntity emitter(BlockPos pos, BlockState state, boolean powered) {
    var emitter = new EmitterEntity(pos, state);
    emitter.setLevel(sceneLevel);
    emitter.powered = powered;
    emitter.root = BlockPos.ZERO;
    return emitter;
  }

  private void structure(GuiGraphics g, EmitterEntity emitter) {
    var state = emitter.getBlockState();
    var pos = emitter.getBlockPos();
    int height = emitter.isTower() ? TowerBlock.HEIGHT : POST_HEIGHT;
    for (int section = 0; section < height; section++) {
      var part = emitter.isTower() ? state.setValue(TowerBlock.SECTION, section)
          : state.setValue(EmitterBlock.SECTION, section);
      part = part.setValue(emitter.isTower() ? TowerBlock.ACTIVE : EmitterBlock.ACTIVE, emitter.powered);
      block(g, part, pos.getX(), pos.getY() + section, pos.getZ());
    }
  }

  private void rails(GuiGraphics g, List<EmitterEntity> fields, boolean bridge, int step, boolean active, float local) {
    int strips = step == 0 ? 1 : 3;
    platform(g, -1, 0, -2, 4);
    platform(g, SPAN, SPAN + 1, -2, 4);
    for (int strip = 0; strip < strips; strip++) {
      int y = bridge ? 0 : strip, z = bridge ? strip : 0;
      for (int x : new int[]{0, SPAN}) {
        block(g, Blocks.SMOOTH_STONE.defaultBlockState(), x == 0 ? -1 : SPAN + 1, y, z);
        var rail = emitter(new BlockPos(x, y, z), FieldEmitters.RAIL.get().defaultBlockState()
            .setValue(RailBlock.FACING, x == 0 ? Direction.EAST : Direction.WEST)
            .setValue(RailBlock.ACTIVE, active), active);
        int[] ground = new int[SPAN + 1];
        Arrays.fill(ground, y);
        if (x == 0) rail.links = List.of(new EmitterEntity.Link(new BlockPos(SPAN, y, z), 1, 0,
            ground, 0, bridge ? Direction.Axis.Y : Direction.Axis.Z, true, null));
        block(g, rail.getBlockState(), x, y, z);
        fields.add(rail);
      }
    }
    if (bridge && step == 2) actor(g, visitor, 1 + travel(local) * 6, 1, 1, -90, progressWalking(local));
  }


  private void details(GuiGraphics g, String chapter, int step, float local, EmitterEntity field, List<EmitterEntity> fields) {
    float progress = travel(local);
    if (chapter.equals("setup") || chapter.equals("trouble")) {
      if (step == 3 || chapter.equals("trouble"))
        block(g, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
            .setValue(LeverBlock.POWERED, field.powered), -1, 0, 0);
      if (chapter.equals("setup") && step >= 2) {
        visitor.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(FieldEmitters.TUNER.get()));
        actor(g, visitor, 1, 0, 1.7, 150, 0);
      }
      return;
    }
    if (chapter.equals("networks") || chapter.equals("automation") || chapter.equals("directions") && step == 3) {
      for (int x : new int[]{0, SPAN}) {
        var back = emitter(new BlockPos(x, 0, -3), FieldEmitters.EMITTER.get().defaultBlockState(), true);
        int[] ground = new int[x == 0 ? SPAN + 1 : 4];
        back.links = List.of(x == 0
            ? new EmitterEntity.Link(new BlockPos(SPAN, 0, -3), 1, 0, ground)
            : new EmitterEntity.Link(new BlockPos(SPAN, 0, 0), 0, 1, ground));
        structure(g, back);
        fields.add(back);
      }
      var side = new EmitterEntity.Link(new BlockPos(0, 0, -3), 0, -1, new int[4]);
      field.links = List.of(field.links.get(0), side);
      if (chapter.equals("directions"))
        actor(g, visitor, SPAN / 2.0, 0, -2.6 + progress * 5.2, 0, progressWalking(local));
      return;
    }
    if (chapter.equals("terrain")) return;
    if (chapter.equals("formations")) return;
    if (chapter.equals("appearance")) {
      impact(field, local);
      return;
    }
    if (chapter.equals("age")) {
      for (var mob : List.of(sheep, baby)) {
        boolean stopped = step == 2 || step < 2 && (mob == sheep) == (step == 0);
        actor(g, mob, mob == sheep ? 3 : 5, 0, 2.6 - progress * (stopped ? 1.65 : 5.2),
            180, progressWalking(local));
      }
      if (step == 3) lamp(g, local >= 2.85 && local < 3.25);
      return;
    }
    if (chapter.equals("items")) {
      actor(g, sheep, 3, 0, 2.6 - progress * 5.2, 180, progressWalking(local));
      item(g, Items.GUNPOWDER.getDefaultInstance(), 5, .12, 2.6 - progress * 1.65);
      return;
    }
    if (chapter.equals("equipment")) {
      visitor.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(
          step == 3 ? FieldEmitters.BADGE_HOLDER.get() : FieldEmitters.TUNER.get()));
      actor(g, visitor, 3, 0, 2, 150, 0);
      return;
    }
    if (chapter.equals("sensor")) {
      double distance = step == 3 ? 1.65 : 5.2;
      actor(g, step == 2 ? secondZombie : sheep, 3, 0, 2.6 - travel(local - (step == 2 ? .25f : 0)) * distance, 180, progressWalking(local));
      actor(g, zombie, 5, 0, 2.6 - progress * distance, 180, progressWalking(local));
      lamp(g, step == 3 ? progress >= 1 : local >= 2.85 && local < (step == 2 ? 3.5 : 3.25));
      return;
    }
    boolean player = chapter.equals("cards") || chapter.equals("checkpoint") || chapter.equals("directions") || chapter.equals("damage") && step == 2;
    LivingEntity actor = player ? visitor : step == 1 ? sheep : zombie;
    boolean blocked = chapter.equals("blocking") && step != 1
        || chapter.equals("directions") && step % 2 == 0 || chapter.equals("damage") && step != 2
        || chapter.equals("cards") && step != 2 || chapter.equals("checkpoint") && step == 3;
    boolean reverse = chapter.equals("directions") && step % 2 == 1;
    double z = reverse ? -2.6 + progress * 5.2 : 2.6 - progress * (blocked ? 1.65 : 5.2);
    visitor.setItemSlot(EquipmentSlot.MAINHAND, (chapter.equals("cards") && step > 0 || chapter.equals("damage") && step == 2)
        ? new ItemStack(FieldEmitters.BADGE.get()) : ItemStack.EMPTY);
    if (chapter.equals("checkpoint") && (local < 2.3 || step == 3))
      visitor.setItemSlot(EquipmentSlot.MAINHAND, Items.TNT.getDefaultInstance());
    actor.hurtTime = chapter.equals("damage")
        && (step == 2 ? local >= 2.3 && local < 2.8 : progress >= 1 && local < DAMAGE_END_SECONDS) ? 5 : 0;
    actor(g, actor, SPAN / 2.0, 0, z, reverse ? 0 : 180, progressWalking(local));
    if (blocked && progress > .9) impact(field, local);
    if (chapter.equals("checkpoint")) {
      block(g, Blocks.CHEST.defaultBlockState(), SPAN + 1, 0, 0);
      // Storage is beside the emitter; the carried item appears only during confiscation.
      float transfer = Math.max(0, Math.min(1, (local - 2.3f) / 1.2f));
      boolean drop = step == 2;
      if (local >= 2.3 && (drop || transfer < 1) && step != 3)
        item(g, Items.TNT.getDefaultInstance(), drop ? SPAN / 2.0 : curve(4, 8, 9.5, transfer),
            1 - (drop ? .88 : .4) * transfer, drop ? 1.2 : curve(1.2, 3, .5, transfer));

    }
  }

  private double curve(double start, double control, double end, float progress) {
    double remaining = 1 - progress;
    return remaining * remaining * start + 2 * remaining * progress * control + progress * progress * end;
  }

  private void lamp(GuiGraphics g, boolean lit) {
    block(g, Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, lit), SPAN + 1, 0, 0);
  }

  private void impact(EmitterEntity field, float local) {
    long time = (long) (local * TICKS_PER_SECOND);
    field.impactWaves.add(new FieldImpacts.Wave(new Vec3(SPAN / 2.0, 1, .5), time / 40 * 40));
  }

  private float progressWalking(float local) {
    return travel(local) > 0 && travel(local) < 1 ? local : 0;
  }

  private float travel(float local) { return Math.max(0, Math.min(1, (local - WALK_START_SECONDS) / WALK_DURATION_SECONDS)); }

  private void domeTerrain(GuiGraphics g, boolean lowered) {
    int edge = SphereField.MIN_RADIUS + 1;
    int bottom = -SphereField.DOME_DEPTH - 1;
    for (int x = -edge; x <= edge; x++) for (int z = -edge; z <= edge; z++) {
      int floor = lowered && x >= 3 ? -2 : -1;
      block(g, Blocks.GRASS_BLOCK.defaultBlockState(), x, floor, z);
      // Solid cutaway edges hide the buried shell, just as terrain does in the world.
      if (Math.abs(x) == edge || Math.abs(z) == edge || lowered && x == 2)
        for (int y = bottom; y < floor; y++) block(g, Blocks.DIRT.defaultBlockState(), x, y, z);
    }
  }

  private void terrain(GuiGraphics g, boolean active) {
    for (int x = -1; x <= SPAN + 1; x++) for (int z = -3; z <= 3; z++) {
      int floor = x >= 2 && x < SPAN - 1 ? -2 : -1;
      block(g, Blocks.GRASS_BLOCK.defaultBlockState(), x, floor, z);
      if (!active && z == 0 && (x == 3 || x == 5))
        block(g, Blocks.DANDELION.defaultBlockState(), x, floor + 1, z);
    }
  }

  private void platform(GuiGraphics g, int minX, int maxX, int minZ, int maxZ) {
    for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++)
      block(g, Blocks.SMOOTH_STONE.defaultBlockState(), x, -1, z);
  }

  private void block(GuiGraphics g, BlockState state, double x, double y, double z) {
    g.pose().pushPose();
    try {
      g.pose().translate(x, y, z);
      Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state, g.pose(), g.bufferSource(),
          LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
    } finally { g.pose().popPose(); }
  }

  private void item(GuiGraphics g, ItemStack stack, double x, double y, double z) {
    g.pose().pushPose();
    try {
      g.pose().translate(x, y, z);
      Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.GROUND,
          LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, g.pose(), g.bufferSource(), sceneLevel, 0);
    } finally { g.pose().popPose(); }
  }

  private void actors(ClientLevel level) {
    if (sceneLevel == level) return;
    sceneLevel = level;
    zombie = EntityType.ZOMBIE.create(level);
    secondZombie = EntityType.ZOMBIE.create(level);
    sheep = EntityType.SHEEP.create(level);
    var lamb = EntityType.SHEEP.create(level);
    lamb.setBaby(true);
    baby = lamb;
    visitor = new RemotePlayer(level, new GameProfile(UUID.fromString("00000000-0000-0000-0000-000000000001"), "Visitor"));
  }

  private void actor(GuiGraphics g, LivingEntity entity, double x, double y, double z,
      float yaw, float walking) {
    if (entity == null) return;
    entity.tickCount = (int) (walking * TICKS_PER_SECOND);
    entity.yBodyRot = entity.yBodyRotO = yaw;
    entity.yHeadRot = entity.yHeadRotO = yaw;
    float speed = walking == 0 ? 0 : .6f;
    entity.walkAnimation.update(walking * 8 - entity.walkAnimation.position(), 1);
    entity.walkAnimation.update(0, 1);
    entity.walkAnimation.setSpeed(speed);
    var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
    dispatcher.setRenderShadow(false);
    try {
      dispatcher.render(entity, x, y, z, yaw, 1, g.pose(), g.bufferSource(), LightTexture.FULL_BRIGHT);
    } finally { dispatcher.setRenderShadow(true); }
  }
}
