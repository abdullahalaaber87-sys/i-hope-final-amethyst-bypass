package com.example.amethystscanner;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;

import org.lwjgl.glfw.GLFW;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AmethystScannerMod implements ClientModInitializer {

    public static final String ID = "amethystscanner";

    private static final KeyBinding.Category CATEGORY =
            KeyBinding.Category.create(
                    Identifier.of(ID, "main")
            );

    public static final KeyBinding OPEN_GUI =
            KeyBindingHelper.registerKeyBinding(
                    new KeyBinding(
                            "key.amethystscanner.open",
                            InputUtil.Type.KEYSYM,
                            GLFW.GLFW_KEY_H,
                            CATEGORY
                    )
            );

    private static final Set<BlockPos> BLOCK_POSITIONS =
            ConcurrentHashMap.newKeySet();

    private int scanCooldown = 0;

    @Override
    public void onInitializeClient() {

        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            while (OPEN_GUI.wasPressed()) {
                client.setScreen(new AmethystScannerScreen());
            }

            if (client.player == null || client.world == null) {
                BLOCK_POSITIONS.clear();
                return;
            }

            scanCooldown++;

            if (scanCooldown >= 20) {
                scanCooldown = 0;
                scanNearbyChunks(client);
            }
        });
    }

    private static void scanNearbyChunks(MinecraftClient client) {

        if (client.player == null || client.world == null) {
            return;
        }

        BLOCK_POSITIONS.clear();

        int playerChunkX = client.player.getChunkPos().x;
        int playerChunkZ = client.player.getChunkPos().z;

        int radius = 4;

        for (int chunkX = playerChunkX - radius;
             chunkX <= playerChunkX + radius;
             chunkX++) {

            for (int chunkZ = playerChunkZ - radius;
                 chunkZ <= playerChunkZ + radius;
                 chunkZ++) {

                WorldChunk chunk =
                        client.world.getChunk(chunkX, chunkZ);

                scanChunk(chunk);
            }
        }
    }

    private static void scanChunk(WorldChunk chunk) {

        int startX = chunk.getPos().getStartX();
        int startZ = chunk.getPos().getStartZ();

        int bottomY = chunk.getBottomY();
        int topY = chunk.getTopYInclusive();

        BlockPos.Mutable mutable =
                new BlockPos.Mutable();

        for (int x = 0; x < 16; x++) {

            for (int z = 0; z < 16; z++) {

                for (int y = bottomY; y <= topY; y++) {

                    mutable.set(
                            startX + x,
                            y,
                            startZ + z
                    );

                    if (chunk.getBlockState(mutable).isOf(
                            Blocks.AMETHYST_CLUSTER
                    )) {

                        BLOCK_POSITIONS.add(
                                mutable.toImmutable()
                        );
                    }
                }
            }
        }
    }

    public static Set<BlockPos> getDetectedBlocks() {
        return BLOCK_POSITIONS;
    }

    public static class AmethystScannerScreen extends Screen {

        protected AmethystScannerScreen() {
            super(Text.literal("Amethyst Scanner"));
        }

        @Override
        protected void init() {
        }

        @Override
        public void render(
                DrawContext context,
                int mouseX,
                int mouseY,
                float delta
        ) {

            super.render(context, mouseX, mouseY, delta);

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("Amethyst Scanner"),
                    width / 2,
                    30,
                    0xFFFFFF
            );

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal(
                            "Detected: "
                                    + BLOCK_POSITIONS.size()
                                    + " Amethyst Clusters"
                    ),
                    width / 2,
                    55,
                    0xFF5555
            );

            context.drawCenteredTextWithShadow(
                    textRenderer,
                    Text.literal("Press H to open this menu"),
                    width / 2,
                    75,
                    0xAAAAAA
            );
        }

        @Override
        public boolean shouldCloseOnEsc() {
            return true;
        }

        @Override
        public boolean shouldPause() {
            return false;
        }
    }
}
