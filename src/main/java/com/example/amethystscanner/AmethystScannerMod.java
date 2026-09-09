package com.example.amethystscanner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenRegistry;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ChunkDataS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AmethystScannerMod implements ClientModInitializer {
    public static final String ID = "amethystscanner";

    public static final KeyBinding OPEN_GUI = new KeyBinding(
            "key.amethystscanner.open",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_H,
            "key.categories.amethystscanner"
    );

    private static final Set<BlockPos> blockPositions =
            ConcurrentHashMap.newKeySet();

    private static final Map<Long, ChunkDataS2CPacket> chunkDataCache =
            new HashMap<>();

    public static final VertexBuffer WIREFRAME_BUFFER =
            new VertexBuffer();

    @Override
    public void onInitializeClient() {
        ClientLifecycleEvents.CLIENT_STARTING.register(client -> {

            // Register keybindings
            KeyBindingHelper.registerKeyBinding(OPEN_GUI);

            ScreenRegistry.register(
                    AmethystScannerScreen.class,
                    (context, player, id) ->
                            new AmethystScannerScreen()
            );

            // Initialize packet listener and world rendering
            PacketListener.init();
            WorldRenderEventsImpl.init();
        });
    }

    public static class AmethystScannerScreen extends Screen {

        public AmethystScannerScreen() {
            super(Text.of("Amethyst Scanner"));
        }

        @Override
        protected void init() {
            super.init();
        }

        @Override
        public void render(
                DrawContext context,
                int mouseX,
                int mouseY,
                float delta
        ) {
            super.render(context, mouseX, mouseY, delta);

            context.drawTextWithShadow(
                    textRenderer,
                    "Amethyst Scanner",
                    10,
                    10,
                    0xFFFFFF
            );
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }
    }

    public static class PacketListener {

        public static void init() {
            ClientSidePacketRegistry.INSTANCE.register(
                    AmethystScannerMod.ID,
                    PacketListener::handlePacket
            );
        }

        private static void handlePacket(
                PacketByteBuf buf,
                ClientWorld world
        ) {
            // Read packet type
            int packetType = buf.readVarInt();

            switch (packetType) {

                case 0:
                    // BlockUpdateS2CPacket
                    BlockUpdateS2CPacket packet =
                            new BlockUpdateS2CPacket(buf);

                    handleBlockUpdate(packet, world);
                    break;

                case 1:
                    // ChunkDataS2CPacket
                    ChunkDataS2CPacket chunkPacket =
                            new ChunkDataS2CPacket(buf);

                    chunkDataCache.put(
                            chunkPacket.getChunkX()
                                    + chunkPacket.getChunkZ(),
                            chunkPacket
                    );
                    break;

                // Add more packet types as needed
            }
        }

        private static void handleBlockUpdate(
                BlockUpdateS2CPacket packet,
                ClientWorld world
        ) {
            BlockPos pos = packet.getPos();

            if (world.getBlockState(pos).getBlock()
                    == Blocks.BUDDING_AMETHYST) {

                blockPositions.add(pos);
            }
        }
    }

    public static class WorldRenderEventsImpl
            implements WorldRenderEvents {

        public static void init() {
            WorldRenderEvents.AFTER_TRANSLUCENT.register(
                    WorldRenderEventsImpl::renderWireframes
            );
        }

        private static void renderWireframes(
                WorldRenderContext context
        ) {
            VertexConsumerProvider.Immediate immediate =
                    context.getBufferBuilders()
                            .getEntityVertexConsumers();

            VertexConsumer vertexConsumer =
                    immediate.getBuffer(
                            WireframeRenderer.WIREFRAME_RENDERLAYER
                    );

            if (blockPositions != null) {

                for (BlockPos pos : blockPositions) {

                    Box box =
                            new Box(pos).expand(1, 1, 1);

                    if (box.intersects(
                            context.getCamera().getFrustum()
                    )) {
                        WireframeRenderer.drawWireframe(
                                box,
                                vertexConsumer
                        );
                    }
                }
            }

            immediate.draw();
        }
    }
}
