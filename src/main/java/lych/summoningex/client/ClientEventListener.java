package lych.summoningex.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import lych.summoningex.SummoningExtensions;
import lych.summoningex.world.Area;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SummoningExtensions.MOD_ID, value = Dist.CLIENT)
public class ClientEventListener {
    @SubscribeEvent
    public static void renderAreas(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }

            Vec3 cp = event.getCamera().getPosition();
            double cx = cp.x;
            double cy = cp.y;
            double cz = cp.z;

            ClientAreaManager manager = ClientAreaManager.getInstanceIn(level.dimension());

            if (!manager.showsAreas()) {
                return;
            }

            for (Area area : manager.getAreas().values()) {
                if (!area.shouldRender(cp)) {
                    continue;
                }

                MultiBufferSource.BufferSource src = Minecraft.getInstance().renderBuffers().bufferSource();
                VertexConsumer consumer = src.getBuffer(RenderType.lines());
                PoseStack stack = event.getPoseStack();

                stack.pushPose();
                LevelRenderer.renderLineBox(stack,
                        consumer,
                        area.asAABB().move(-cx, -cy, -cz),
                        (area.getColor() >> 16 & 0xFF) / 255f,
                        (area.getColor() >> 8 & 0xFF) / 255f,
                        (area.getColor() & 0xFF) / 255f,
                        1);
                stack.popPose();
            }
        }
    }
}
