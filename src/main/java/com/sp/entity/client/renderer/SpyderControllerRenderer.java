package com.sp.entity.client.renderer;

import com.sp.SPBRevamped;
import com.sp.entity.custom.SpyderControllerEntity;
import com.sp.init.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Locale;

public class SpyderControllerRenderer extends EntityRenderer<SpyderControllerEntity> {
    private static final Identifier TEXTURE = new Identifier("spb-revamped", "textures/block/void_block.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float MODEL_SCALE = 1.5F;
    private static final double CLAW_RAYCAST_UP = 0.35;
    private static final double CLAW_RAYCAST_DOWN = 1.35;
    private long nextDebugLogMillis;

    public SpyderControllerRenderer(EntityRendererFactory.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    @Override
    public void render(SpyderControllerEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        matrices.push();
        applySurfaceBasis(matrices, entity);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(entity.getBodyPitchDegrees()));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getBodyRollDegrees()));

        float alpha = getAlpha(entity, tickDelta);
        Vec3d up = safeNormal(entity.getSurfaceNormal(), new Vec3d(0.0, 1.0, 0.0));
        Vec3d forward = safeProjectedForward(entity.getSurfaceForward(), up);
        Vec3d right = safeNormal(up.crossProduct(forward), new Vec3d(1.0, 0.0, 0.0));

        int legCount = entity.getLegCount();
        Vec3d[] localTargets = new Vec3d[legCount];
        Vec3d[] localEndEffectors = new Vec3d[legCount];
        Vec3d[] localGrounds = new Vec3d[legCount];
        Vec3d[][] localJoints = new Vec3d[legCount][];
        Vec3d[] tipWorldPositions = new Vec3d[legCount];
        ClawGroundDebug[] clawGroundDebugs = new ClawGroundDebug[legCount];

        for (int i = 0; i < legCount; i++) {
            localTargets[i] = worldToModelLocal(entity.getLegTarget(i).subtract(entity.getPos()), right, up, forward);
            localEndEffectors[i] = worldToModelLocal(entity.getLegEndEffector(i).subtract(entity.getPos()), right, up, forward);
            localGrounds[i] = worldToModelLocal(entity.getLegGroundPosition(i).subtract(entity.getPos()), right, up, forward);

            Vec3d[] worldJoints = entity.getLegJoints(i);
            localJoints[i] = new Vec3d[worldJoints.length];
            for (int jointIndex = 0; jointIndex < worldJoints.length; jointIndex++) {
                localJoints[i][jointIndex] = worldToModelLocal(worldJoints[jointIndex].subtract(entity.getPos()), right, up, forward);
            }

            tipWorldPositions[i] = worldJoints[worldJoints.length - 1];
            clawGroundDebugs[i] = sampleClawGround(entity, tipWorldPositions[i], up);
        }

        matrices.scale(MODEL_SCALE, MODEL_SCALE, MODEL_SCALE);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        renderCuboid(pos, normal, buffer, FULL_BRIGHT, alpha, -0.16F, -0.05F, -0.22F, 0.16F, 0.05F, 0.20F);
        renderCuboid(pos, normal, buffer, FULL_BRIGHT, alpha, -0.19F, -0.055F, 0.17F, 0.19F, 0.055F, 0.53F);
        renderCuboid(pos, normal, buffer, FULL_BRIGHT, alpha, -0.075F, -0.045F, -0.46F, 0.075F, 0.045F, -0.24F);

        for (int i = 0; i < legCount; i++) {
            renderLeg(matrices, vertexConsumers, alpha, localJoints[i]);
        }

        renderDebugGizmos(entity, matrices, vertexConsumers, localTargets, localEndEffectors, localGrounds, localJoints, tipWorldPositions, clawGroundDebugs, right, up, forward);
        maybeLogSpyderDebug(entity, localTargets, localEndEffectors, localGrounds, localJoints, clawGroundDebugs);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void applySurfaceBasis(MatrixStack matrices, SpyderControllerEntity entity) {
        Vec3d up = safeNormal(entity.getSurfaceNormal(), new Vec3d(0.0, 1.0, 0.0));
        Vec3d forward = safeProjectedForward(entity.getSurfaceForward(), up);
        Vec3d right = safeNormal(up.crossProduct(forward), new Vec3d(1.0, 0.0, 0.0));

        Matrix4f basis = new Matrix4f(
                (float) right.x, (float) right.y, (float) right.z, 0.0F,
                (float) up.x, (float) up.y, (float) up.z, 0.0F,
                (float) (-forward.x), (float) (-forward.y), (float) (-forward.z), 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F
        );

        matrices.peek().getPositionMatrix().mul(basis);
        matrices.peek().getNormalMatrix().mul(new Matrix3f(basis));
    }

    @Override
    public Identifier getTexture(SpyderControllerEntity entity) {
        return TEXTURE;
    }

    private void renderLeg(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float alpha, Vec3d[] joints) {
        if (joints == null || joints.length < 2) {
            return;
        }

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        float[] thickness = new float[]{0.020F, 0.016F, 0.012F, 0.008F};
        for (int i = 0; i < joints.length - 1; i++) {
            renderSegmentBetween(matrices, buffer, alpha, joints[i], joints[i + 1], thickness[Math.min(i, thickness.length - 1)]);
        }
    }

    private void renderDebugGizmos(SpyderControllerEntity entity,
                                   MatrixStack matrices,
                                   VertexConsumerProvider vertexConsumers,
                                   Vec3d[] localTargets,
                                   Vec3d[] localEndEffectors,
                                   Vec3d[] localGrounds,
                                   Vec3d[][] localJoints,
                                   Vec3d[] tipWorldPositions,
                                   ClawGroundDebug[] clawGroundDebugs,
                                   Vec3d right,
                                   Vec3d up,
                                   Vec3d forward) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (!player.getMainHandStack().isOf(ModItems.SPYDER_DEBUG) && !player.getOffHandStack().isOf(ModItems.SPYDER_DEBUG)) {
            return;
        }

        VertexConsumer lines = vertexConsumers.getBuffer(RenderLayer.getLines());
        for (int i = 0; i < entity.getLegCount(); i++) {
            Vec3d[] joints = localJoints[i];
            if (joints == null || joints.length == 0) {
                continue;
            }

            Vec3d root = joints[0];
            Vec3d tip = joints[joints.length - 1];
            Vec3d target = localTargets[i];
            Vec3d effector = localEndEffectors[i];
            Vec3d ground = localGrounds[i];

            renderDebugMarker(matrices, lines, root, 1.0F, 1.0F, 1.0F);
            renderDebugMarker(matrices, lines, target, 1.0F, 0.85F, 0.2F);
            renderDebugMarker(matrices, lines, ground, 0.15F, 1.0F, 0.35F);
            renderDebugMarker(matrices, lines, effector, entity.isLegGrounded(i) ? 0.2F : 1.0F, entity.isLegGrounded(i) ? 1.0F : 0.2F, 0.25F);
            renderDebugMarker(matrices, lines, tip, 0.25F, 0.6F, 1.0F);

            renderDebugLine(matrices, lines, root, effector, 0.2F, 1.0F, 0.3F);
            renderDebugLine(matrices, lines, effector, target, 1.0F, 0.8F, 0.2F);
            renderDebugLine(matrices, lines, effector, ground, 0.15F, 1.0F, 0.35F);
            renderDebugLine(matrices, lines, tip, effector, 1.0F, 0.2F, 0.9F);

            for (int jointIndex = 0; jointIndex < joints.length; jointIndex++) {
                renderDebugMarker(matrices, lines, joints[jointIndex], 0.85F, 0.85F, 0.85F);
            }

            ClawGroundDebug clawDebug = clawGroundDebugs[i];
            if (clawDebug != null && clawDebug.hit) {
                Vec3d rayStartLocal = worldToModelLocal(clawDebug.rayStart.subtract(entity.getPos()), right, up, forward);
                Vec3d rayHitLocal = worldToModelLocal(clawDebug.hitPos.subtract(entity.getPos()), right, up, forward);
                Vec3d tipLocalForRay = worldToModelLocal(tipWorldPositions[i].subtract(entity.getPos()), right, up, forward);

                renderDebugMarker(matrices, lines, rayHitLocal, clawDebug.penetration > 0.0 ? 1.0F : 0.15F, clawDebug.penetration > 0.0 ? 0.2F : 1.0F, 0.25F);
                renderDebugLine(matrices, lines, rayStartLocal, rayHitLocal, 0.35F, 0.85F, 1.0F);
                renderDebugLine(matrices, lines, tipLocalForRay, rayHitLocal,
                        clawDebug.penetration > 0.0 ? 1.0F : 0.15F,
                        clawDebug.penetration > 0.0 ? 0.2F : 1.0F,
                        0.25F);
            }
        }

        if (entity.hasDebugGoal()) {
            Vec3d goalLocal = worldToModelLocal(entity.getDebugGoalPos().subtract(entity.getPos()), right, up, forward);
            Vec3d goalNormalEndLocal = worldToModelLocal(entity.getDebugGoalPos().add(entity.getDebugGoalNormal().multiply(0.38)).subtract(entity.getPos()), right, up, forward);

            renderDebugMarker(matrices, lines, goalLocal, 1.0F, 0.15F, 0.15F);
            renderDebugLine(matrices, lines, new Vec3d(0.0, 0.0, 0.0), goalLocal, 1.0F, 0.3F, 0.3F);
            renderDebugLine(matrices, lines, goalLocal, goalNormalEndLocal, 1.0F, 0.6F, 0.1F);
        }
    }

    private void maybeLogSpyderDebug(SpyderControllerEntity entity,
                                     Vec3d[] localTargets,
                                     Vec3d[] localEndEffectors,
                                     Vec3d[] localGrounds,
                                     Vec3d[][] localJoints,
                                     ClawGroundDebug[] clawGroundDebugs) {
        MinecraftClient client = MinecraftClient.getInstance();
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }
        if (!player.getMainHandStack().isOf(ModItems.SPYDER_DEBUG) && !player.getOffHandStack().isOf(ModItems.SPYDER_DEBUG)) {
            return;
        }
        if (player.squaredDistanceTo(entity) > 48.0 * 48.0) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now < this.nextDebugLogMillis) {
            return;
        }
        this.nextDebugLogMillis = now + 1000L;

        StringBuilder builder = new StringBuilder();
        builder.append("Spyder leg debug: bodyLift=").append(format(entity.getBodyHeightOffset()));
        for (int i = 0; i < localJoints.length; i++) {
            Vec3d[] joints = localJoints[i];
            if (joints == null || joints.length == 0) {
                continue;
            }

            Vec3d rootLocal = joints[0];
            Vec3d tipLocal = joints[joints.length - 1];
            Vec3d effectorLocal = localEndEffectors[i];
            Vec3d targetLocal = localTargets[i];
            Vec3d groundLocal = localGrounds[i];
            Vec3d solveDeltaLocal = tipLocal.subtract(effectorLocal);
            Vec3d targetDeltaLocal = effectorLocal.subtract(targetLocal);
            ClawGroundDebug clawDebug = clawGroundDebugs[i];
            Vec3d tipWorld = entity.getLegJoints(i)[joints.length - 1];

            builder.append(" | L").append(i)
                    .append(" rootRel=(").append(format(rootLocal.x)).append(",").append(format(rootLocal.y)).append(",").append(format(rootLocal.z)).append(")")
                    .append(" tipRel=(").append(format(tipLocal.x)).append(",").append(format(tipLocal.y)).append(",").append(format(tipLocal.z)).append(")")
                    .append(" effRel=(").append(format(effectorLocal.x)).append(",").append(format(effectorLocal.y)).append(",").append(format(effectorLocal.z)).append(")")
                    .append(" groundRel=(").append(format(groundLocal.x)).append(",").append(format(groundLocal.y)).append(",").append(format(groundLocal.z)).append(")")
                    .append(" targetRel=(").append(format(targetLocal.x)).append(",").append(format(targetLocal.y)).append(",").append(format(targetLocal.z)).append(")")
                    .append(" solveDelta=(").append(format(solveDeltaLocal.x)).append(",").append(format(solveDeltaLocal.y)).append(",").append(format(solveDeltaLocal.z)).append(")")
                    .append(" targetDelta=(").append(format(targetDeltaLocal.x)).append(",").append(format(targetDeltaLocal.y)).append(",").append(format(targetDeltaLocal.z)).append(")")
                    .append(" grounded=").append(entity.isLegGrounded(i))
                    .append(" stepping=").append(entity.isLegStepping(i))
                    .append(" tipY=").append(format(tipWorld.y));

            if (clawDebug != null) {
                builder.append(" rayHit=").append(clawDebug.hit)
                        .append(" rayLen=").append(format(clawDebug.rayLength))
                        .append(" tipGround=").append(format(clawDebug.tipToGround))
                        .append(" penetration=").append(format(clawDebug.penetration));
            }
        }

        SPBRevamped.LOGGER.info(builder.toString());
    }

    private ClawGroundDebug sampleClawGround(SpyderControllerEntity entity, Vec3d tipWorld, Vec3d up) {
        Vec3d rayStart = tipWorld.add(up.multiply(CLAW_RAYCAST_UP));
        Vec3d rayEnd = tipWorld.subtract(up.multiply(CLAW_RAYCAST_DOWN));
        HitResult hit = entity.getWorld().raycast(new RaycastContext(
                rayStart,
                rayEnd,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                entity
        ));

        if (hit.getType() == HitResult.Type.MISS) {
            return new ClawGroundDebug(false, rayStart, rayEnd, rayEnd, rayStart.distanceTo(rayEnd), Double.NaN, 0.0);
        }

        Vec3d hitPos = hit.getPos();
        double tipToGround = tipWorld.distanceTo(hitPos);
        double penetration = Math.max(0.0, -tipWorld.subtract(hitPos).dotProduct(up));
        return new ClawGroundDebug(true, rayStart, rayEnd, hitPos, rayStart.distanceTo(hitPos), tipToGround, penetration);
    }

    private Vec3d worldToModelLocal(Vec3d worldOffset, Vec3d right, Vec3d up, Vec3d forward) {
        Vec3d translated = toLocal(worldOffset, right, up, forward);
        return new Vec3d(
                translated.x / MODEL_SCALE,
                translated.y / MODEL_SCALE,
                translated.z / MODEL_SCALE
        );
    }

    private Vec3d toLocal(Vec3d worldOffset, Vec3d right, Vec3d up, Vec3d forward) {
        return new Vec3d(
                worldOffset.dotProduct(right),
                worldOffset.dotProduct(up),
                -worldOffset.dotProduct(forward)
        );
    }

    private Vec3d safeNormal(Vec3d vec, Vec3d fallback) {
        return vec.lengthSquared() < 0.0001 ? fallback : vec.normalize();
    }

    private Vec3d safeProjectedForward(Vec3d forward, Vec3d up) {
        Vec3d projected = forward.subtract(up.multiply(forward.dotProduct(up)));
        if (projected.lengthSquared() < 0.0001) {
            projected = new Vec3d(0.0, 0.0, 1.0).subtract(up.multiply(up.z));
        }
        return safeNormal(projected, new Vec3d(0.0, 0.0, 1.0));
    }

    private void renderDebugMarker(MatrixStack matrices, VertexConsumer buffer, Vec3d pos, float r, float g, float b) {
        float size = 0.045F;
        renderDebugLine(matrices, buffer, pos.add(-size, 0.0, 0.0), pos.add(size, 0.0, 0.0), r, g, b);
        renderDebugLine(matrices, buffer, pos.add(0.0, -size, 0.0), pos.add(0.0, size, 0.0), r, g, b);
        renderDebugLine(matrices, buffer, pos.add(0.0, 0.0, -size), pos.add(0.0, 0.0, size), r, g, b);
    }

    private void renderDebugLine(MatrixStack matrices, VertexConsumer buffer, Vec3d start, Vec3d end, float r, float g, float b) {
        MatrixStack.Entry entry = matrices.peek();
        Vec3d direction = end.subtract(start);
        if (direction.lengthSquared() < 0.0000001) {
            return;
        }
        Vec3d normal = direction.normalize();
        buffer.vertex(entry.getPositionMatrix(), (float) start.x, (float) start.y, (float) start.z)
                .color(r, g, b, 1.0F)
                .normal(entry.getNormalMatrix(), (float) normal.x, (float) normal.y, (float) normal.z)
                .next();
        buffer.vertex(entry.getPositionMatrix(), (float) end.x, (float) end.y, (float) end.z)
                .color(r, g, b, 1.0F)
                .normal(entry.getNormalMatrix(), (float) normal.x, (float) normal.y, (float) normal.z)
                .next();
    }

    private void renderSegment(MatrixStack matrices, VertexConsumer buffer, float alpha, float x, float y, float z, float thickness) {
        MatrixStack.Entry entry = matrices.peek();
        renderCuboid(entry.getPositionMatrix(), entry.getNormalMatrix(), buffer, FULL_BRIGHT, alpha,
                x - thickness, y - thickness, 0.0F,
                x + thickness, y + thickness, z);
    }

    private void renderSegmentBetween(MatrixStack matrices, VertexConsumer buffer, float alpha, Vec3d start, Vec3d end, float thickness) {
        Vec3d direction = end.subtract(start);
        double length = direction.length();
        if (length < 0.0001) {
            return;
        }

        double horizontal = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float yaw = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.max(0.0001, horizontal)));

        matrices.push();
        matrices.translate(start.x, start.y, start.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        renderSegment(matrices, buffer, alpha, 0.00F, 0.00F, (float) length, thickness);
        matrices.pop();
    }

    private float getAlpha(SpyderControllerEntity entity, float tickDelta) {
        float age = entity.age + tickDelta;
        return switch (entity.getSpyderState()) {
            case ENTERING -> MathHelper.clamp(age / 28.0F, 0.0F, 0.95F);
            case EXITING -> MathHelper.clamp(1.0F - ((age - 178.0F) / 40.0F), 0.0F, 0.95F);
            case DESPAWNED -> 0.0F;
            default -> 0.95F;
        };
    }

    private void renderCuboid(Matrix4f pos, Matrix3f normal, VertexConsumer buffer, int light, float alpha,
                              float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        renderFace(pos, normal, buffer, light, alpha, minX, minY, maxZ, maxX, maxY, maxZ, 0.0f, 0.0f, 1.0f);
        renderFace(pos, normal, buffer, light, alpha, maxX, minY, minZ, minX, maxY, minZ, 0.0f, 0.0f, -1.0f);
        renderFace(pos, normal, buffer, light, alpha, maxX, minY, maxZ, maxX, maxY, minZ, 1.0f, 0.0f, 0.0f);
        renderFace(pos, normal, buffer, light, alpha, minX, minY, minZ, minX, maxY, maxZ, -1.0f, 0.0f, 0.0f);
        renderHorizontalFace(pos, normal, buffer, light, alpha, minX, maxY, minZ, maxX, maxY, maxZ, 0.0f, 1.0f, 0.0f);
        renderHorizontalFace(pos, normal, buffer, light, alpha, minX, minY, maxZ, maxX, minY, minZ, 0.0f, -1.0f, 0.0f);
    }

    private void renderFace(Matrix4f pos, Matrix3f normal, VertexConsumer buffer, int light, float alpha,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float nx, float ny, float nz) {
        vertex(buffer, pos, normal, x0, y0, z0, 0.0f, 1.0f, light, alpha, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y0, z1, 1.0f, 1.0f, light, alpha, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y1, z1, 1.0f, 0.0f, light, alpha, nx, ny, nz);
        vertex(buffer, pos, normal, x0, y1, z0, 0.0f, 0.0f, light, alpha, nx, ny, nz);
    }

    private void renderHorizontalFace(Matrix4f pos, Matrix3f normal, VertexConsumer buffer, int light, float alpha,
                                      float x0, float y, float z0, float x1, float y1, float z1,
                                      float nx, float ny, float nz) {
        vertex(buffer, pos, normal, x0, y, z0, 0.0f, 1.0f, light, alpha, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y, z0, 1.0f, 1.0f, light, alpha, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y1, z1, 1.0f, 0.0f, light, alpha, nx, ny, nz);
        vertex(buffer, pos, normal, x0, y1, z1, 0.0f, 0.0f, light, alpha, nx, ny, nz);
    }

    private void vertex(VertexConsumer buffer, Matrix4f pos, Matrix3f normal, float x, float y, float z, float u, float v, int light, float alpha,
                        float nx, float ny, float nz) {
        buffer.vertex(pos, x, y, z)
                .color(255, 255, 255, Math.max(1, (int) (alpha * 255.0F)))
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, nx, ny, nz)
                .next();
    }

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private record ClawGroundDebug(
            boolean hit,
            Vec3d rayStart,
            Vec3d rayEnd,
            Vec3d hitPos,
            double rayLength,
            double tipToGround,
            double penetration
    ) {
    }
}
