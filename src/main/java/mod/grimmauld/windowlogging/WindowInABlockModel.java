package mod.grimmauld.windowlogging;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockview.v2.FabricBlockView;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

@Environment(EnvType.CLIENT)
@MethodsReturnNonnullByDefault
public class WindowInABlockModel implements BlockStateModel {
	private static final BlockRenderDispatcher DISPATCHER = Minecraft.getInstance().getBlockRenderer();
	private static final Direction[] DIRECTIONS_PLUS_NULL = {
		null, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST
	};

	private final BlockStateModel wrapped;

	public WindowInABlockModel(BlockStateModel wrapped) {
		this.wrapped = wrapped;
	}

	@Override
	public void collectParts(@NonNull RandomSource randomSource, @NonNull List<BlockModelPart> list) {
		// the wrapped (vanilla air) model's parts are an accurate fallback if context-free as
		// this method is only reached by callers that bypass FabricBlockStateModel#emitQuads entirely
		wrapped.collectParts(randomSource, list);
	}

	@Override
	public TextureAtlasSprite particleIcon() {
		return wrapped.particleIcon();
	}

	@Override
	public TextureAtlasSprite particleSprite(@NonNull BlockAndTintGetter blockView, @NonNull BlockPos pos, @NonNull BlockState state) {
		WindowInABlockTileEntity wte = renderDataOf(blockView, pos);
		if (wte == null)
			return wrapped.particleIcon();
		return DISPATCHER.getBlockModel(wte.getPartialBlock()).particleIcon();
	}

	@Override
	public void emitQuads(@NonNull QuadEmitter emitter, @NonNull BlockAndTintGetter blockView, @NonNull BlockPos pos, @NonNull BlockState state,
	                      @NonNull RandomSource random, @NonNull Predicate<Direction> cullTest) {
		WindowInABlockTileEntity wte = renderDataOf(blockView, pos);
		if (wte == null) {
			// Fall back to whatever the wrapped (vanilla air) model would have emitted -
			// mirrors FabricBlockStateModel's own default emitQuads implementation.
			for (BlockModelPart part : wrapped.collectParts(random))
				part.emitQuads(emitter, cullTest);
			return;
		}

		BlockState partialState = wte.getPartialBlock();
		BlockState windowState = wte.getWindowBlock();

		random.setSeed(state.getSeed(pos));
		ChunkSectionLayer partialLayer = ItemBlockRenderTypes.getChunkRenderType(partialState);
		for (BlockModelPart part : DISPATCHER.getBlockModel(partialState).collectParts(random))
			emitPart(emitter, part, null, blockView, pos, partialLayer);

		// Reseed before the second, independent sub-model per FabricBlockStateModel's guidance
		// (see MultiPartModel for the vanilla example this follows).
		random.setSeed(state.getSeed(pos));
		ChunkSectionLayer windowLayer = ItemBlockRenderTypes.getChunkRenderType(windowState);
		for (BlockModelPart part : DISPATCHER.getBlockModel(windowState).collectParts(random))
			emitPart(emitter, part, partialState, blockView, pos, windowLayer);
	}

	private void emitPart(QuadEmitter emitter, BlockModelPart part, @Nullable BlockState cullAgainst,
	                      BlockAndTintGetter blockView, BlockPos pos, ChunkSectionLayer layer) {
		for (Direction bucket : DIRECTIONS_PLUS_NULL) {
			for (BakedQuad quad : part.getQuads(bucket)) {
				// cullAgainst == null means "this is the partial block itself" - render as-is.
				// Non-null means "this is the window pane" - apply the original culling/nudge.
				if (cullAgainst != null && hasSolidSide(cullAgainst, blockView, pos, quad.direction()))
					continue;

				emitter.fromBakedQuad(quad);
				// Each source block (partial vs. window) keeps its own natural render layer
				// (e.g. SOLID for stone, TRANSLUCENT for stained glass) instead of the whole
				// block being forced into one shared layer - see WindowloggingClient's javadoc
				// for why that broke translucent panes specifically.
				emitter.renderLayer(layer);
				if (cullAgainst != null)
					fightZfighting(emitter, quad.direction());
				emitter.cullFace(bucket).emit();
			}
		}
	}

	/**
	 * Nudges the quad very slightly along its face normal to avoid Z-fighting between the window
	 * pane's texture and a coplanar face of the partial block.
	 * <p>
	 * Ported note: the original Forge code mutated the shared, cached {@code BakedQuad}'s raw
	 * vertex {@code int[]} data <em>in place</em>, every single time the quad was rendered - since
	 * that array is the same baked instance reused on every frame (and shared across every block
	 * of this type in the world), repeated mutation would keep nudging it further and further away
	 * each frame, compounding indefinitely rather than applying a fixed offset once. That's a
	 * latent bug in the original. This port instead nudges the emitter's already-decoded,
	 * per-call-local vertex positions (via {@link QuadEmitter}, which is safe to mutate - it's
	 * thread-local/transient per the Fabric Rendering API's own documentation), leaving the
	 * underlying baked quad untouched.
	 * <p>
	 * <b>Verify before relying on this:</b> this assumes {@code QuadEmitter}/{@code QuadView}
	 * expose read accessors for a vertex's position (e.g. {@code x(int)}/{@code y(int)}/
	 * {@code z(int)}) alongside the {@code pos(int, float, float, float)} setter seen in the
	 * {@code QuadEmitter} source - that read side wasn't shown to me directly, so double-check the
	 * exact getter names against {@code QuadView} in your local Fabric API jar.
	 */
	private static void fightZfighting(QuadEmitter emitter, Direction dir) {
		Vec3i n = dir.getUnitVec3i();
		float dx = n.getX() / 512f;
		float dy = n.getY() / 512f;
		float dz = n.getZ() / 512f;
		for (int v = 0; v < 4; ++v)
			emitter.pos(v, emitter.x(v) - dx, emitter.y(v) - dy, emitter.z(v) - dz);
	}

	private static boolean hasSolidSide(BlockState state, BlockGetter worldIn, BlockPos pos, Direction side) {
		return !state.is(BlockTags.LEAVES) && Block.isFaceFull(state.getBlockSupportShape(worldIn, pos), side);
	}

	@Nullable
	private static WindowInABlockTileEntity renderDataOf(BlockAndTintGetter blockView, BlockPos pos) {
		if (!(blockView instanceof FabricBlockView view))
			return null;
		Object data = view.getBlockEntityRenderData(pos);
		return data instanceof WindowInABlockTileEntity wte ? wte : null;
	}
}
