package mod.grimmauld.windowlogging;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockview.v2.RenderDataBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@MethodsReturnNonnullByDefault
public class WindowInABlockTileEntity extends BlockEntity implements RenderDataBlockEntity {
	private BlockState partialBlock = Blocks.AIR.defaultBlockState();
	private BlockState windowBlock = Blocks.AIR.defaultBlockState();
	private CompoundTag partialBlockTileData = new CompoundTag();
	private BlockEntity partialBlockTileEntity = null;
	@Environment(EnvType.CLIENT)
	public BlockState hoveredBlock = Blocks.AIR.defaultBlockState();

	public WindowInABlockTileEntity(BlockPos pos, BlockState blockState) {
		super(Windowlogging.WINDOW_IN_A_BLOCK_TILE_ENTITY, pos, blockState);
		setPartialBlockTileData(new CompoundTag());
	}

	public CompoundTag getPartialBlockTileData() {
		return partialBlockTileData;
	}

	public void setPartialBlockTileData(CompoundTag partialBlockTileData) {
		this.partialBlockTileData = partialBlockTileData;
	}

	@Override
	protected void loadAdditional(@NonNull ValueInput input) {
		super.loadAdditional(input);
		partialBlock = input.read("PartialBlock", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		windowBlock = input.read("WindowBlock", BlockState.CODEC).orElse(Blocks.AIR.defaultBlockState());
		setPartialBlockTileData(input.read("PartialData", CompoundTag.CODEC).orElse(new CompoundTag()));
		requestModelDataUpdate();
	}

	@Override
	protected void saveAdditional(@NonNull ValueOutput output) {
		super.saveAdditional(output);
		output.store("PartialBlock", BlockState.CODEC, getPartialBlock());
		output.store("WindowBlock", BlockState.CODEC, getWindowBlock());
		output.store("PartialData", net.minecraft.nbt.CompoundTag.CODEC, partialBlockTileData);
	}

	public void updateWindowConnections() {
		if (level == null)
			return;
		for (Direction side : Direction.values()) {
			BlockPos offsetPos = worldPosition.relative(side);
			BlockState windowNeighborState =
					WindowInABlockBlock.resolveWindowNeighborState(level, offsetPos, level.getBlockState(offsetPos));
			windowBlock = getWindowBlock().updateShape(level, level, worldPosition, side, offsetPos,
					windowNeighborState, level.getRandom());
		}
		level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 2 | 16);
		setChanged();
	}

	@Override
	public Object getRenderData() {
		return this;
	}

	/**
	 * Forge cached {@code ModelData} per block entity and required an explicit
	 * {@code requestModelDataUpdate()} call any time it changed, invalidating that cache.
	 * Fabric's {@link RenderDataBlockEntity#getRenderData()} is instead re-queried by the renderer
	 * on every frame, so there is no cache to invalidate - this method is kept as a no-op purely so
	 * existing call sites (which mirror the upstream Forge code closely) don't need touching.
	 * {@code setChanged()} still needs to be called separately wherever persisted state changes.
	 */
	public void requestModelDataUpdate() {}

	public BlockState getPartialBlock() {
		return partialBlock;
	}

	public void setPartialBlock(BlockState partialBlock) {
		this.partialBlock = partialBlock;
	}

	public BlockState getWindowBlock() {
		return windowBlock;
	}

	public void setWindowBlock(BlockState windowBlock) {
		this.windowBlock = windowBlock;
	}

	@Override
	public CompoundTag getUpdateTag(HolderLookup.@NonNull Provider registries) {
		return saveWithoutMetadata(registries);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Nullable
	public BlockEntity getPartialBlockTileEntityIfPresent() {
		if (!(getPartialBlock() instanceof EntityBlock entityBlock) || level == null)
			return null;
		if (partialBlockTileEntity == null) {
			try {
				partialBlockTileEntity = entityBlock.newBlockEntity(worldPosition, partialBlock);
				if (partialBlockTileEntity != null) {
					partialBlockTileEntity.setBlockState(getPartialBlock());
					partialBlockTileEntity.loadWithComponents(
						NbtBridge.toValueInput(level.registryAccess(), partialBlockTileData));
					partialBlockTileEntity.setLevel(level);
				}
			} catch (Exception e) {
				partialBlockTileEntity = null;
			}
		}
		return partialBlockTileEntity;
	}
}
