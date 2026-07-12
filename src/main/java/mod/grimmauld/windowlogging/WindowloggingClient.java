package mod.grimmauld.windowlogging;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

@Environment(EnvType.CLIENT)
public class WindowloggingClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.putBlock(Windowlogging.WINDOW_IN_A_BLOCK, ChunkSectionLayer.CUTOUT);

		BlockEntityRenderers.register(Windowlogging.WINDOW_IN_A_BLOCK_TILE_ENTITY, WindowInABlockTileEntityRenderer::new);

		WindowBlockColor.registerFor(Windowlogging.WINDOW_IN_A_BLOCK);

		ModelLoadingPlugin.register(new WindowloggingModelLoadingPlugin());
	}
}
