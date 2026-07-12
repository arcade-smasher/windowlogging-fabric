package mod.grimmauld.windowlogging;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * Adds just what {@link WindowInABlockTileEntityRenderer} needs beyond the base render state: the
 * already-resolved "partial block" block entity (if any), captured once during
 * {@code extractRenderState} rather than re-resolved during {@code submit} - matching how this
 * newer rendering API splits "gather state" from "submit for drawing" (potentially on different
 * threads/times).
 */
@Environment(EnvType.CLIENT)
public class WindowInABlockRenderState extends BlockEntityRenderState {
	@Nullable
	public BlockEntity partialBlockEntity;
	public float partialTick;
}
