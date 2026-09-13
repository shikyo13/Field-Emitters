package com.zerotheabsolute.fieldemitters.client;

import com.zeromods.core.animation.HexFieldPattern;
import com.zerotheabsolute.fieldemitters.EmitterEntity;
import com.zerotheabsolute.fieldemitters.SphereField;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** The planar ripple profile, measured along the sphere rather than through it. */
final class SphereSurfaceRipples implements HexFieldPattern.Ripples {
  private record Wave(Vec3 normal, float age, double minDot, double maxDot) {}
  private final List<Wave> waves = new ArrayList<>();
  private final float textureRadius;
  private final int radius;

  SphereSurfaceRipples(EmitterEntity emitter, float partial, float textureRadius) {
    this.textureRadius = textureRadius;
    radius = emitter.controls.sphereRadius;
    var style = HexFieldPattern.Style.FIELD;
    double now = emitter.getLevel().getGameTime() + partial;
    for (var hit : emitter.impactWaves) {
      float age = (float)(now - hit.time());
      if (age < 0 || age >= style.lifetime()) continue;
      double reach = age * style.speed(), margin = style.bandWidth() * 3;
      waves.add(new Wave(hit.position().subtract(SphereField.center(emitter)).normalize(), age,
          Math.cos(Math.min(Math.PI, (reach + margin) / radius)),
          Math.cos(Math.max(0, (reach - margin) / radius))));
    }
  }

  public float sample(float u, float v, HexFieldPattern.Style style) {
    var point = normal(u / textureRadius, v / textureRadius);
    float strongest = 0, total = 0;
    for (var wave : waves) {
      double dot = point.dot(wave.normal);
      if (dot < wave.minDot || dot > wave.maxDot) continue;
      double distance = Math.acos(Mth.clamp(dot, -1, 1)) * radius;
      double band = (distance - wave.age * style.speed()) / style.bandWidth();
      float value = (float)Math.exp(-band * band) * (1 - wave.age / style.lifetime());
      strongest = Math.max(strongest, value);
      total += value;
    }
    return strongest + (1 - strongest) * (float)(1 - Math.exp(-(total - strongest)));
  }

  public void rings(float left, float right, float bottom, float top, int color,
      HexFieldPattern.Style style, HexFieldPattern.Stroke stroke) {
    final int segments = HexFieldPattern.RING_SEGMENTS;
    for (var wave : waves) {
      var tangent = wave.normal.cross(Math.abs(wave.normal.y) > .9 ? new Vec3(1,0,0) : new Vec3(0,1,0)).normalize();
      var bitangent = wave.normal.cross(tangent);
      double angle = wave.age * style.speed() / radius;
      float fade = 1 - wave.age / style.lifetime();
      Vec3 previous = ringPoint(wave.normal, tangent, bitangent, angle, 0);
      for (int i = 1; i <= segments; i++) {
        Vec3 next = ringPoint(wave.normal, tangent, bitangent, angle, i * Math.PI * 2 / segments);
        float x1 = azimuth(previous), x2 = azimuth(next);
        float y1 = (float)Math.asin(Mth.clamp(previous.y,-1,1)) * textureRadius;
        float y2 = (float)Math.asin(Mth.clamp(next.y,-1,1)) * textureRadius;
        if (Math.abs(x1-x2) > (right-left)/2) {
          if (x1<x2) x1 += right-left; else x2 += right-left;
          segment(x1,y1,x2,y2,left,right,bottom,top,color,fade,stroke);
          segment(x1-right+left,y1,x2-right+left,y2,left,right,bottom,top,color,fade,stroke);
        } else segment(x1,y1,x2,y2,left,right,bottom,top,color,fade,stroke);
        previous = next;
      }
    }
  }

  private static void segment(float x1,float y1,float x2,float y2,float left,float right,
      float bottom,float top,int color,float fade,HexFieldPattern.Stroke stroke) {
    float dx=x2-x1,dy=y2-y1,lo=0,hi=1;
    if (Math.abs(dx)<.00001f) { if(x1<left||x1>right)return; }
    else { float a=(left-x1)/dx,b=(right-x1)/dx;lo=Math.max(lo,Math.min(a,b));hi=Math.min(hi,Math.max(a,b)); }
    if (Math.abs(dy)<.00001f) { if(y1<bottom||y1>top)return; }
    else { float a=(bottom-y1)/dy,b=(top-y1)/dy;lo=Math.max(lo,Math.min(a,b));hi=Math.min(hi,Math.max(a,b)); }
    if(hi<=lo)return;
    HexFieldPattern.ringSegment(stroke, x1+dx*lo, y1+dy*lo, x1+dx*hi, y1+dy*hi, color, fade);
  }

  private float azimuth(Vec3 point) {
    double angle = Math.atan2(point.z,point.x);
    return (float)(angle < 0 ? angle + Math.PI*2 : angle) * textureRadius;
  }

  private static Vec3 normal(double azimuth,double elevation) {
    double horizontal=Math.cos(elevation);
    return new Vec3(horizontal*Math.cos(azimuth),Math.sin(elevation),horizontal*Math.sin(azimuth));
  }

  private static Vec3 ringPoint(Vec3 normal,Vec3 tangent,Vec3 bitangent,double radius,double angle) {
    return normal.scale(Math.cos(radius)).add(tangent.scale(Math.sin(radius)*Math.cos(angle)))
        .add(bitangent.scale(Math.sin(radius)*Math.sin(angle)));
  }
}
