package com.sp.entity.client.renderer;

import com.sp.entity.custom.SpyderControllerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class SpyderControllerRenderer extends EntityRenderer<SpyderControllerEntity> {
    private static final Identifier TEXTURE = new Identifier("spb-revamped", "textures/block/void_block.png");
    private static final int FULL_BRIGHT = 0x00F000F0;
    private static final float LEG_SEGMENT_A = 0.32F;
    private static final float LEG_SEGMENT_B = 0.34F;
    private static final float LEG_SEGMENT_C = 0.29F;
    private static final float LEG_SEGMENT_D = 0.20F;

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
        float age = entity.age + tickDelta;
        float stride = entity.getStrideProgress();
        float motion = entity.getMovementAmount();
        float bodyBob = MathHelper.sin(age * 0.12F) * 0.0035F;

        matrices.translate(0.0F, 0.30F + bodyBob, 0.0F);
        matrices.scale(1.5F, 1.5F, 1.5F);

        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        renderCuboid(pos, normal, buffer, FULL_BRIGHT, alpha, -0.16F, -0.05F, -0.22F, 0.16F, 0.05F, 0.20F);
        renderCuboid(pos, normal, buffer, FULL_BRIGHT, alpha, -0.19F, -0.055F, 0.17F, 0.19F, 0.055F, 0.53F);
        renderCuboid(pos, normal, buffer, FULL_BRIGHT, alpha, -0.075F, -0.045F, -0.46F, 0.075F, 0.045F, -0.24F);

        Vec3d up = safeNormal(entity.getSurfaceNormal(), new Vec3d(0.0, 1.0, 0.0));
        Vec3d forward = safeProjectedForward(entity.getSurfaceForward(), up);
        Vec3d right = safeNormal(up.crossProduct(forward), new Vec3d(1.0, 0.0, 0.0));

        for (int i = 0; i < entity.getLegCount(); i++) {
            Vec3d root = entity.getLegRootOffset(i);
            Vec3d target = toLocal(entity.getLegTarget(i).subtract(entity.getPos()), right, up, forward);
            renderLeg(matrices, vertexConsumers, alpha, (float) root.x, (float) root.y - 0.02F, (float) root.z, target, i % 2 != 0, motion);
        }

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    private void applySurfaceBasis(MatrixStack matrices, SpyderControllerEntity entity) {
        Vec3d up = entity.getSurfaceNormal().normalize();
        Vec3d forward = entity.getSurfaceForward().normalize();
        if (up.lengthSquared() < 0.0001) {
            up = new Vec3d(0.0, 1.0, 0.0);
        }
        if (forward.lengthSquared() < 0.0001) {
            forward = new Vec3d(0.0, 0.0, 1.0);
        }

        forward = forward.subtract(up.multiply(forward.dotProduct(up))).normalize();
        if (forward.lengthSquared() < 0.0001) {
            forward = new Vec3d(0.0, 0.0, 1.0);
        }

        Vec3d right = up.crossProduct(forward).normalize();
        if (right.lengthSquared() < 0.0001) {
            right = new Vec3d(1.0, 0.0, 0.0);
        }

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

    private void renderLeg(MatrixStack matrices, VertexConsumerProvider vertexConsumers, float alpha,
                           float x, float y, float z, Vec3d target, boolean mirrored, float motion) {
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayer.getEntityTranslucent(TEXTURE));
        Vec3d direction = target.subtract(x, y, z);
        if (direction.lengthSquared() < 0.0001) {
            direction = new Vec3d(mirrored ? -0.6 : 0.6, -0.35, 0.2);
        }

        float yawDeg = (float) Math.toDegrees(Math.atan2(direction.x, direction.z));
        float horizontal = (float) Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float pitchDeg = (float) -Math.toDegrees(Math.atan2(direction.y, Math.max(0.0001, horizontal)));
        float totalReach = LEG_SEGMENT_A + LEG_SEGMENT_B + LEG_SEGMENT_C + LEG_SEGMENT_D - 0.03F;
        float targetDistance = MathHelper.clamp((float) direction.length(), 0.10F, totalReach);
        float firstChainLength = LEG_SEGMENT_B + LEG_SEGMENT_C + LEG_SEGMENT_D;
        float firstOffset = degreesAcos((LEG_SEGMENT_A * LEG_SEGMENT_A + targetDistance * targetDistance - firstChainLength * firstChainLength)
                / (2.0F * LEG_SEGMENT_A * targetDistance));
        float upperPitch = pitchDeg + firstOffset + 2.0F;

        Vec3d afterFemur = segmentEndpoint(LEG_SEGMENT_A, upperPitch);
        Vec3d remainingTarget = new Vec3d(0.0, direction.y - afterFemur.y, horizontal - afterFemur.z);
        float remainingDistance = MathHelper.clamp((float) remainingTarget.length(), 0.06F, LEG_SEGMENT_B + LEG_SEGMENT_C + LEG_SEGMENT_D - 0.01F);
        float secondChainLength = LEG_SEGMENT_C + LEG_SEGMENT_D;
        float secondBasePitch = (float) -Math.toDegrees(Math.atan2(remainingTarget.y, Math.max(0.0001, remainingTarget.z)));
        float secondOffset = degreesAcos((LEG_SEGMENT_B * LEG_SEGMENT_B + remainingDistance * remainingDistance - secondChainLength * secondChainLength)
                / (2.0F * LEG_SEGMENT_B * remainingDistance));
        float secondWorldPitch = secondBasePitch + secondOffset + 8.0F;
        float midPitch = secondWorldPitch - upperPitch;

        Vec3d afterTibiaRelative = segmentEndpoint(LEG_SEGMENT_B, secondWorldPitch);
        Vec3d remainingForLower = new Vec3d(
                0.0,
                remainingTarget.y - afterTibiaRelative.y,
                remainingTarget.z - afterTibiaRelative.z
        );
        float thirdWorldPitch = (float) -Math.toDegrees(Math.atan2(remainingForLower.y, Math.max(0.0001, remainingForLower.z))) + 10.0F;
        float lowerPitch = thirdWorldPitch - secondWorldPitch;

        Vec3d afterMetatarsusRelative = segmentEndpoint(LEG_SEGMENT_C, thirdWorldPitch);
        Vec3d remainingForClaw = new Vec3d(
                0.0,
                remainingForLower.y - afterMetatarsusRelative.y,
                remainingForLower.z - afterMetatarsusRelative.z
        );
        float clawWorldPitch = (float) -Math.toDegrees(Math.atan2(remainingForClaw.y, Math.max(0.0001, remainingForClaw.z)));
        float clawSurfaceBias = MathHelper.lerp(0.65F, clawWorldPitch, 90.0F);
        float clawPitch = MathHelper.clamp(clawSurfaceBias - thirdWorldPitch, -20.0F, 80.0F);

        float splay = (mirrored ? -1.0F : 1.0F) * 4.0F;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yawDeg));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(splay));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(upperPitch));

        renderSegment(matrices, buffer, alpha, 0.00F, 0.00F, LEG_SEGMENT_A, 0.020F);
        matrices.translate(0.0F, 0.0F, LEG_SEGMENT_A);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(midPitch));

        renderSegment(matrices, buffer, alpha, 0.00F, 0.00F, LEG_SEGMENT_B, 0.016F);
        matrices.translate(0.0F, 0.0F, LEG_SEGMENT_B);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lowerPitch));

        renderSegment(matrices, buffer, alpha, 0.00F, 0.00F, LEG_SEGMENT_C, 0.012F);
        matrices.translate(0.0F, 0.0F, LEG_SEGMENT_C);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(clawPitch));

        renderSegment(matrices, buffer, alpha, 0.00F, 0.00F, LEG_SEGMENT_D, 0.008F);
        matrices.pop();
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

    private float degreesAcos(float value) {
        return (float) Math.toDegrees(Math.acos(MathHelper.clamp(value, -1.0F, 1.0F)));
    }

    private Vec3d segmentEndpoint(float length, float worldPitchDeg) {
        double radians = Math.toRadians(worldPitchDeg);
        double y = -Math.sin(radians) * length;
        double z = Math.cos(radians) * length;
        return new Vec3d(0.0, y, z);
    }

    private void renderSegment(MatrixStack matrices, VertexConsumer buffer, float alpha, float x, float y, float z, float thickness) {
        MatrixStack.Entry entry = matrices.peek();
        renderCuboid(entry.getPositionMatrix(), entry.getNormalMatrix(), buffer, FULL_BRIGHT, alpha,
                x - thickness, y - thickness, 0.0F,
                x + thickness, y + thickness, z);
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
}
