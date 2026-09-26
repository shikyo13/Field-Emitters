package com.zerotheabsolute.fieldemitters.client;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.zeromods.core.client.ShaderPackCompat;
import com.zerotheabsolute.fieldemitters.EmitterEntity;
import com.zerotheabsolute.fieldemitters.FieldEmitters;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

public final class FieldEffects {
  private static final int BUFFER_BYTES = 65536;
  private static final Map<EmitterEntity, Consumer<MultiBufferSource>> PENDING = new LinkedHashMap<>();
  private static final MultiBufferSource.BufferSource BUFFERS =
      MultiBufferSource.immediate(new BufferBuilder(BUFFER_BYTES));
  // Model-view in effect when the fields were captured; the later draw stage may differ.
  private static final Matrix4f MODEL_VIEW = new Matrix4f();
  private static boolean drawing;

  private FieldEffects() {}

  static boolean defer(EmitterEntity emitter, PoseStack pose,
      BiConsumer<PoseStack, MultiBufferSource> draw) {
    if (drawing) return false;
    MODEL_VIEW.set(RenderSystem.getModelViewMatrix());
    var copy = new PoseStack();
    copy.last().pose().set(pose.last().pose());
    copy.last().normal().set(pose.last().normal());
    float light = sceneLight(emitter);
    PENDING.put(emitter, buffers -> draw.accept(copy, type -> type == FieldRenderType.ENERGY
        ? new Exposed(buffers.getBuffer(type), light) : buffers.getBuffer(type)));
    return true;
  }

  /** Brightest light around the hardware, with moonlit sky counted as dark. */
  private static float sceneLight(EmitterEntity emitter) {
    if (!(emitter.getLevel() instanceof ClientLevel level)) return 1;
    var top = emitter.getBlockPos().above(6);
    int block = 0, sky = 0;
    for (var pos : new net.minecraft.core.BlockPos[] {top, top.relative(Direction.NORTH),
        top.relative(Direction.SOUTH), top.relative(Direction.EAST), top.relative(Direction.WEST)}) {
      int packed = LevelRenderer.getLightColor(level, pos);
      block = Math.max(block, LightTexture.block(packed));
      sky = Math.max(sky, LightTexture.sky(packed));
    }
    float daylight = (level.getSkyDarken(1) - .2f) / .8f;
    return Math.max(block, sky * daylight) / 15f;
  }

  /** Energy vertices whose alpha is adjusted for a shader pack's emissive response. */
  private record Exposed(VertexConsumer target, float light) implements VertexConsumer {
    @Override public VertexConsumer vertex(double x, double y, double z) {
      target.vertex(x, y, z);
      return this;
    }
    @Override public VertexConsumer color(int red, int green, int blue, int alpha) {
      target.color(red, green, blue,
          Math.round(ShaderPackCompat.emissiveAlpha(alpha / 255f, light) * 255));
      return this;
    }
    @Override public VertexConsumer uv(float u, float v) {
      target.uv(u, v);
      return this;
    }
    @Override public VertexConsumer overlayCoords(int u, int v) {
      target.overlayCoords(u, v);
      return this;
    }
    @Override public VertexConsumer uv2(int u, int v) {
      target.uv2(u, v);
      return this;
    }
    @Override public VertexConsumer normal(float x, float y, float z) {
      target.normal(x, y, z);
      return this;
    }
    @Override public void endVertex() { target.endVertex(); }
    @Override public void defaultColor(int red, int green, int blue, int alpha) {
      target.defaultColor(red, green, blue, alpha);
    }
    @Override public void unsetDefaultColor() { target.unsetDefaultColor(); }
  }

  public static void register() {
    WorldRenderEvents.START.register(context -> PENDING.clear());
    WorldRenderEvents.LAST.register(context -> render());
  }

  private static void render() {
    if (PENDING.isEmpty()) return;
    drawing = true;
    var modelView = RenderSystem.getModelViewStack();
    modelView.pushPose();
    modelView.setIdentity();
    modelView.mulPoseMatrix(MODEL_VIEW);
    RenderSystem.applyModelViewMatrix();
    try {
      PENDING.forEach((emitter, draw) -> {
        if (!emitter.isRemoved()) draw.accept(BUFFERS);
      });
      BUFFERS.endBatch();
    } finally {
      modelView.popPose();
      RenderSystem.applyModelViewMatrix();
      drawing = false;
      PENDING.clear();
    }
  }
}
