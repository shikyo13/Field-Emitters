package com.zerotheabsolute.fieldemitters;

import java.lang.reflect.Method;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Converts contact geometry without changing the entity's world position or identity. */
final class FieldSpace {
  private static final FieldSpace WORLD = new FieldSpace(null);
  private static final Access ACCESS = Access.load();
  private final Object pose;

  private FieldSpace(Object pose) { this.pose = pose; }

  static FieldSpace at(EmitterEntity emitter) {
    if (ACCESS == null) return WORLD;
    try {
      Object subLevel = ACCESS.containing.invoke(ACCESS.companion, emitter);
      return subLevel == null ? WORLD : new FieldSpace(ACCESS.pose.invoke(subLevel));
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Cannot resolve Sable field coordinates", exception);
    }
  }

  Vec3 local(Vec3 point) { return transform(point, true); }
  Vec3 world(Vec3 point) { return transform(point, false); }

  private Vec3 transform(Vec3 point, boolean inverse) {
    if (pose == null) return point;
    try {
      return (Vec3) (inverse ? ACCESS.inverse : ACCESS.forward).invoke(pose, point);
    } catch (ReflectiveOperationException exception) {
      throw new IllegalStateException("Cannot transform Sable field coordinates", exception);
    }
  }

  AABB local(AABB box) {
    if (pose == null) return box;
    Vec3 first = local(new Vec3(box.minX, box.minY, box.minZ));
    AABB result = new AABB(first, first);
    for (int corner = 1; corner < 8; corner++) {
      Vec3 point = local(new Vec3((corner & 1) == 0 ? box.minX : box.maxX,
          (corner & 2) == 0 ? box.minY : box.maxY,
          (corner & 4) == 0 ? box.minZ : box.maxZ));
      result = result.minmax(new AABB(point, point));
    }
    return result;
  }

  private record Access(Object companion, Method containing, Method pose, Method inverse, Method forward) {
    static Access load() {
      try {
        Class<?> companion = Class.forName("dev.ryanhcode.sable.companion.SableCompanion");
        Class<?> subLevel = Class.forName("dev.ryanhcode.sable.companion.SubLevelAccess");
        Class<?> pose = Class.forName("dev.ryanhcode.sable.companion.math.Pose3dc");
        return new Access(companion.getField("INSTANCE").get(null),
            companion.getMethod("getContaining", BlockEntity.class), subLevel.getMethod("logicalPose"),
            pose.getMethod("transformPositionInverse", Vec3.class), pose.getMethod("transformPosition", Vec3.class));
      } catch (ClassNotFoundException absent) {
        return null;
      } catch (ReflectiveOperationException exception) {
        throw new IllegalStateException("Unsupported Sable companion API", exception);
      }
    }
  }
}
