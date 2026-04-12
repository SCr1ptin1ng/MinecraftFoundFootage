package com.sp.entity.client.renderer;

import com.sp.SPBRevamped;
import com.sp.entity.custom.FacelingEntity;
import com.sp.init.ModModelLayers;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class FacelingRenderer extends MobEntityRenderer<FacelingEntity, PlayerEntityModel<FacelingEntity>> {
    private static final Identifier DEFAULT_TEXTURE = new Identifier(SPBRevamped.MOD_ID, "textures/entity/faceling/faceling.png");

    public FacelingRenderer(EntityRendererFactory.Context context) {
        super(context, new PlayerEntityModel<>(context.getPart(ModModelLayers.FACELING), false), 0.5F);
    }

    @Override
    public Identifier getTexture(FacelingEntity entity) {
        return DEFAULT_TEXTURE;
    }

    public static TexturedModelData getTexturedModelData() {
        return TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, false), 64, 64);
    }
}
