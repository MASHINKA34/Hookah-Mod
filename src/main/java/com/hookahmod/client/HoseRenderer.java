package com.hookahmod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

final class HoseRenderer {
    private static final int SIDES = 8;
    private static final double[] COS = new double[SIDES + 1];
    private static final double[] SIN = new double[SIDES + 1];

    static {
        for (int side = 0; side <= SIDES; side++) {
            double angle = 2.0 * Math.PI * side / SIDES;
            COS[side] = Math.cos(angle);
            SIN[side] = Math.sin(angle);
        }
    }

    private HoseRenderer() {}

    static void draw(PoseStack pose, VertexConsumer vertices, Vec3 a, Vec3 b, Vec3 c, Vec3 d,
                     int segments, float radius, int light) {
        Vec3 previous = a;
        for (int segment = 1; segment <= segments; segment++) {
            double t = (double) segment / segments;
            double u = 1.0 - t;
            Vec3 next = new Vec3(
                    u * u * u * a.x + 3 * u * u * t * b.x + 3 * u * t * t * c.x + t * t * t * d.x,
                    u * u * u * a.y + 3 * u * u * t * b.y + 3 * u * t * t * c.y + t * t * t * d.y,
                    u * u * u * a.z + 3 * u * u * t * b.z + 3 * u * t * t * c.z + t * t * t * d.z);
            drawSegment(pose.last(), vertices, previous, next, ((segment - 1) % 4) * 0.25f, radius, light);
            previous = next;
        }
    }

    private static void drawSegment(PoseStack.Pose pose, VertexConsumer vertices, Vec3 a, Vec3 b,
                                    float v, float radius, int light) {
        Vec3 direction = b.subtract(a);
        if (direction.lengthSqr() < 1.0E-12) return;
        direction = direction.normalize();
        Vec3 reference = Math.abs(direction.y) < 0.99 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0);
        Vec3 right = direction.cross(reference).normalize();
        Vec3 up = right.cross(direction).normalize();
        for (int side = 0; side < SIDES; side++) {
            vertex(pose, vertices, a, right, up, side, radius, v, light);
            vertex(pose, vertices, b, right, up, side, radius, v + 0.25f, light);
            vertex(pose, vertices, b, right, up, side + 1, radius, v + 0.25f, light);
            vertex(pose, vertices, a, right, up, side + 1, radius, v, light);
        }
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer vertices, Vec3 center, Vec3 right, Vec3 up,
                               int side, float radius, float v, int light) {
        float nx = (float) (right.x * COS[side] + up.x * SIN[side]);
        float ny = (float) (right.y * COS[side] + up.y * SIN[side]);
        float nz = (float) (right.z * COS[side] + up.z * SIN[side]);
        vertices.addVertex(pose.pose(), (float) center.x + nx * radius,
                        (float) center.y + ny * radius, (float) center.z + nz * radius)
                .setColor(255, 255, 255, 255)
                .setUv((float) side / SIDES, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
