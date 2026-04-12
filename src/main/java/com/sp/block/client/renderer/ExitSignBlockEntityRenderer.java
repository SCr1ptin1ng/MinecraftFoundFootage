package com.sp.block.client.renderer;

import com.sp.block.custom.ExitSignBlock;
import com.sp.block.entity.ExitSignBlockEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class ExitSignBlockEntityRenderer implements BlockEntityRenderer<ExitSignBlockEntity> {
    private static final Identifier DIFFUSE_TEXTURE = new Identifier("spb-revamped", "textures/entity/exits_sign/exits_sign.png");
    private static final Identifier EMISSION_TEXTURE = new Identifier("spb-revamped", "textures/entity/exits_sign/exits_sign_emission.png");
    private static final int FULL_BRIGHT = 0x00F000F0;

    public ExitSignBlockEntityRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public void render(ExitSignBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        Direction facing = entity.getCachedState().get(ExitSignBlock.FACING);

        matrices.push();
        matrices.translate(0.5f, 0.5f, 0.5f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.translate(-0.5f, -0.5f, -0.5f);

        renderModel(matrices, vertexConsumers, light, overlay);
        matrices.pop();
    }

    private void renderModel(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f pos = entry.getPositionMatrix();
        Matrix3f normal = entry.getNormalMatrix();

        VertexConsumer diffuse = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(DIFFUSE_TEXTURE));
        VertexConsumer emissive = vertexConsumers.getBuffer(RenderLayer.getEyes(EMISSION_TEXTURE));

        // Blockbench element 1: main sign body, updated to the latest authored model bounds.
        renderCuboid(pos, normal, diffuse, light, overlay,
                -3f / 16f, 0f / 16f, 11f / 16f,
                16f / 16f, 10f / 16f, 13f / 16f,
                0f, 0.03125f, 6.96875f, 7.59375f, 0,
                0f, 0f, 0.75f, 7.59375f, 0,
                0.03125f, 0f, 6.9375f, 7.5625f, 0,
                0f, 0f, 0.71875f, 7.5625f, 0,
                0.03125f, 0f, 0.625f, 0.0625f, 0,
                0.125f, 0f, 6.5f, 0.78125f, 0);
        renderCuboid(pos, normal, emissive, FULL_BRIGHT, overlay,
                -3f / 16f, 0f / 16f, 11f / 16f,
                16f / 16f, 10f / 16f, 13f / 16f,
                0f, 0.03125f, 6.96875f, 7.59375f, 0,
                0f, 0f, 0.75f, 7.59375f, 0,
                0.03125f, 0f, 6.9375f, 7.5625f, 0,
                0f, 0f, 0.71875f, 7.5625f, 0,
                0.03125f, 0f, 0.625f, 0.0625f, 0,
                0.125f, 0f, 6.5f, 0.78125f, 0);

        // Blockbench element 2: upper housing. Missing faces are intentionally redirected to diffuse UVs.
        renderCuboid(pos, normal, diffuse, light, overlay,
                -3f / 16f, 10f / 16f, 11f / 16f,
                16f / 16f, 14f / 16f, 16f / 16f,
                0f, 0f, 6.875f, 2f, 0,
                0f, 0f, 1f, 1.40625f, 0,
                0.03125f, 0.1875f, 0.625f, 0.3125f, 0,
                0f, 0f, 0.65625f, 0.90625f, 0,
                0.03125f, 0f, 7f, 1.65625f, 0,
                0.03125f, 0.5f, 0.625f, 1f, 0);
        renderCuboid(pos, normal, emissive, FULL_BRIGHT, overlay,
                -3f / 16f, 10f / 16f, 11f / 16f,
                16f / 16f, 14f / 16f, 16f / 16f,
                0f, 0f, 6.875f, 2f, 0,
                0f, 0f, 1f, 1.40625f, 0,
                0.03125f, 0.1875f, 0.625f, 0.3125f, 0,
                0f, 0f, 0.65625f, 0.90625f, 0,
                0.03125f, 0f, 7f, 1.65625f, 0,
                0.03125f, 0.5f, 0.625f, 1f, 0);
    }

    private void renderCuboid(Matrix4f pos, Matrix3f normal, VertexConsumer buffer, int light, int overlay,
                              float minX, float minY, float minZ,
                              float maxX, float maxY, float maxZ,
                              float northU0, float northV0, float northU1, float northV1, int northRotation,
                              float eastU0, float eastV0, float eastU1, float eastV1, int eastRotation,
                              float southU0, float southV0, float southU1, float southV1, int southRotation,
                              float westU0, float westV0, float westU1, float westV1, int westRotation,
                              float upU0, float upV0, float upU1, float upV1, int upRotation,
                              float downU0, float downV0, float downU1, float downV1, int downRotation) {
        renderFace(pos, normal, buffer, light, overlay, minX, minY, maxZ, maxX, maxY, maxZ, northU0, northV0, northU1, northV1, northRotation, 0.0f, 0.0f, 1.0f);
        renderFace(pos, normal, buffer, light, overlay, maxX, minY, minZ, minX, maxY, minZ, southU0, southV0, southU1, southV1, southRotation, 0.0f, 0.0f, -1.0f);
        renderFace(pos, normal, buffer, light, overlay, maxX, minY, maxZ, maxX, maxY, minZ, eastU0, eastV0, eastU1, eastV1, eastRotation, 1.0f, 0.0f, 0.0f);
        renderFace(pos, normal, buffer, light, overlay, minX, minY, minZ, minX, maxY, maxZ, westU0, westV0, westU1, westV1, westRotation, -1.0f, 0.0f, 0.0f);
        renderHorizontalFace(pos, normal, buffer, light, overlay, minX, maxY, minZ, maxX, maxY, maxZ, upU0, upV0, upU1, upV1, upRotation, 0.0f, 1.0f, 0.0f);
        renderHorizontalFace(pos, normal, buffer, light, overlay, minX, minY, maxZ, maxX, minY, minZ, downU0, downV0, downU1, downV1, downRotation, 0.0f, -1.0f, 0.0f);
    }

    private void renderFace(Matrix4f pos, Matrix3f normal, VertexConsumer buffer, int light, int overlay,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float u0, float v0, float u1, float v1, int rotation,
                            float nx, float ny, float nz) {
        float[][] uv = rotateUvs(u0 / 16.0f, v0 / 16.0f, u1 / 16.0f, v1 / 16.0f, rotation);
        vertex(buffer, pos, normal, x0, y0, z0, uv[0][0], uv[0][1], light, overlay, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y0, z1, uv[1][0], uv[1][1], light, overlay, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y1, z1, uv[2][0], uv[2][1], light, overlay, nx, ny, nz);
        vertex(buffer, pos, normal, x0, y1, z0, uv[3][0], uv[3][1], light, overlay, nx, ny, nz);
    }

    private void renderHorizontalFace(Matrix4f pos, Matrix3f normal, VertexConsumer buffer, int light, int overlay,
                                      float x0, float y, float z0, float x1, float y1, float z1,
                                      float u0, float v0, float u1, float v1, int rotation,
                                      float nx, float ny, float nz) {
        float[][] uv = rotateUvs(u0 / 16.0f, v0 / 16.0f, u1 / 16.0f, v1 / 16.0f, rotation);
        vertex(buffer, pos, normal, x0, y, z0, uv[0][0], uv[0][1], light, overlay, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y, z0, uv[1][0], uv[1][1], light, overlay, nx, ny, nz);
        vertex(buffer, pos, normal, x1, y1, z1, uv[2][0], uv[2][1], light, overlay, nx, ny, nz);
        vertex(buffer, pos, normal, x0, y1, z1, uv[3][0], uv[3][1], light, overlay, nx, ny, nz);
    }

    private float[][] rotateUvs(float u0, float v0, float u1, float v1, int rotation) {
        float[][] uv = new float[][]{
                {u0, v1},
                {u1, v1},
                {u1, v0},
                {u0, v0}
        };
        int turns = Math.floorMod(rotation / 90, 4);
        for (int i = 0; i < turns; i++) {
            uv = new float[][]{
                    uv[3],
                    uv[0],
                    uv[1],
                    uv[2]
            };
        }
        return uv;
    }

    private void vertex(VertexConsumer buffer, Matrix4f pos, Matrix3f normal, float x, float y, float z, float u, float v, int light, int overlay, float nx, float ny, float nz) {
        buffer.vertex(pos, x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(normal, nx, ny, nz)
                .next();
    }
}
