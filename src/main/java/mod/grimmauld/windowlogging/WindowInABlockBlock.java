package mod.grimmauld.windowlogging;

import com.mojang.logging.annotations.MethodsReturnNonnullByDefault;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

@MethodsReturnNonnullByDefault
public class WindowInABlockBlock extends IronBarsBlock implements EntityBlock {

	public WindowInABlockBlock(ResourceKey<Block> key) {
		super(Properties.ofFullCopy(Blocks.STONE).noOcclusion().setId(key));
	}

	@Environment(EnvType.CLIENT)
	public static BlockPos breakingBlock = new BlockPos(0,0,0);
	@Environment(EnvType.CLIENT)
	public static Integer breakingSide = -1;

	public BlockState destroyedState;

	public void cleanupDestroyedState() {
		destroyedState = null;
	}

	@Override
	public void playerDestroy(@NonNull Level level, @NonNull Player player, @NonNull BlockPos blockPos, @NonNull BlockState blockState, @Nullable BlockEntity blockEntity, @NonNull ItemStack itemStack) {
		if (destroyedState == null) return;
		destroyedState.getBlock().playerDestroy(level, player, blockPos, destroyedState, level.getBlockEntity(blockPos), itemStack);
		destroyedState = null;
	}

	public void destroy(BlockPos blockPos, BlockState blockState2, BlockEntity blockEntity, Level level, Player player) {

		if (!(blockState2.getBlock() instanceof WindowInABlockBlock)) return;
		if (!(blockEntity instanceof WindowInABlockTileEntity wte)) return;

		destroyedState = getLookedAtEmbeddedBlock(level, player, blockPos, wte);

		BlockState windowState = wte.getWindowBlock();
		BlockState partialState = wte.getPartialBlock();

		if (destroyedState == null) return;

		BlockState restoreState;

		if (destroyedState.equals(windowState)) {
			restoreState = partialState;
		} else {
			restoreState = windowState;
		}

		level.setBlock(blockPos, restoreState, Block.UPDATE_ALL);
	}

	public static boolean hitContains(BlockState subState, BlockGetter level, BlockPos pos, Vec3 relativeHit) {
		for (AABB bb : subState.getShape(level, pos).toAabbs())
			if (bb.inflate(1e-7).contains(relativeHit)) // account for small error, do not remove!
				return true;
		return false;
	}

	@Override
	public boolean canBeReplaced(@NonNull BlockState state, @NonNull BlockPlaceContext useContext) {
		return false;
	}

	@Override
	public float getDestroyProgress(@NonNull BlockState blockState, @NonNull Player player, @NonNull BlockGetter blockGetter, @NonNull BlockPos blockPos) {
		WindowInABlockTileEntity wte = getTileEntity(blockGetter, blockPos);
		BlockState lookedAtBlock = getLookedAtEmbeddedBlock((Level) blockGetter, player, blockPos, wte);
		if (player.level().isClientSide()) {
			// if we start breaking a different side of the same block, reset breaking progress
			if (breakingBlock == blockPos && breakingSide != (lookedAtBlock == wte.getWindowBlock() ? 1 : 0)) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.gameMode == null) {
					System.err.println("gameMode is null");
				} else {
					mc.schedule(() -> {
						mc.gameMode.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, blockPos, Direction.DOWN));
						BlockHitResult blockHitResult = (BlockHitResult) mc.hitResult;
						mc.gameMode.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, blockPos, blockHitResult.getDirection()));
						mc.gameMode.destroyProgress = 0.0F;
						mc.gameMode.destroyTicks = 0.0F;
					});
				}
				return 0.0F;
			}
			breakingBlock = blockPos;
			breakingSide = lookedAtBlock == wte.getWindowBlock() ? 1 : 0;
		}
		if (lookedAtBlock == null) {
			return super.getDestroyProgress(blockState, player, blockGetter, blockPos);
		}
		return lookedAtBlock.getDestroyProgress(player, blockGetter, blockPos);
	}

	public ItemStack getCloneItemStackWithPlayer(@NonNull Player player, @NonNull LevelReader level, @NonNull BlockPos blockPos, boolean includeData) {
		BlockState lookedAtBlock = getLookedAtEmbeddedBlock(player.level(), player, blockPos, getTileEntity(level, blockPos));
		if (lookedAtBlock == null) {
			BlockState surroundingState = getSurroundingBlockState(level, blockPos);
			return surroundingState.getBlock().getCloneItemStack(level, blockPos, surroundingState, includeData);
		}
		return lookedAtBlock.getCloneItemStack(level, blockPos, includeData);
	}

	@Override
	public ItemStack getCloneItemStack(@NonNull LevelReader level, @NonNull BlockPos blockPos, @NonNull BlockState state, boolean includeData) {
		if (level.isClientSide()) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || mc.level == null) {
				BlockState surroundingState = getSurroundingBlockState(level, blockPos);
				return surroundingState.getBlock().getCloneItemStack(level, blockPos, surroundingState, includeData);
			}
			return getCloneItemStackWithPlayer(mc.player, level, blockPos, includeData);
		} else {
			BlockState surroundingState = getSurroundingBlockState(level, blockPos);
			return surroundingState.getBlock().getCloneItemStack(level, blockPos, surroundingState, includeData);
		}
	}

	public List<ItemStack> dropHelper(@NonNull BlockState block, LootParams.@NonNull Builder builder) {
		LootParams.Builder newBuilder = new LootParams.Builder(builder.getLevel());
		newBuilder.withParameter(LootContextParams.ORIGIN, builder.getParameter(LootContextParams.ORIGIN));
		ItemStack tool = builder.getOptionalParameter(LootContextParams.TOOL);
		if (tool != null) newBuilder.withParameter(LootContextParams.TOOL, tool);
		Entity entity = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (entity != null) newBuilder.withOptionalParameter(LootContextParams.THIS_ENTITY, entity);
		return block.getDrops(newBuilder);
	}

	@Override
	public List<ItemStack> getDrops(@NonNull BlockState state, LootParams.@NonNull Builder builder) {
		Entity entity = builder.getOptionalParameter(LootContextParams.THIS_ENTITY);
		BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
		if (blockEntity instanceof WindowInABlockTileEntity wte) {
			if (entity instanceof Player player) {
				BlockState embeddedBlock = getLookedAtEmbeddedBlock(builder.getLevel(), player, wte.getBlockPos(), wte);
				if (embeddedBlock != null) {
					return dropHelper(embeddedBlock, builder);
				}
			}
			List<ItemStack> combinedDrops = dropHelper(state, builder);
			combinedDrops.addAll(dropHelper(state, builder));
			return combinedDrops;
		}
		System.err.println("Cannot get drops for state " + state);
		System.err.println("Please report this issue to the mod developer!");
		return dropHelper(state, builder);
	}

	public VoxelShape getCombinedShape(@NonNull BlockState state, @NonNull BlockGetter worldIn, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		VoxelShape shape1 = getSurroundingBlockState(worldIn, pos).getShape(worldIn, pos, context);
		VoxelShape shape2 = getWindowBlockState(worldIn, pos).getShape(worldIn, pos, context);
		return Shapes.or(shape1, shape2);
	}

	public VoxelShape getCombinedShape(@NonNull BlockState state, @NonNull BlockGetter worldIn, @NonNull BlockPos pos) {
		VoxelShape shape1 = getSurroundingBlockState(worldIn, pos).getShape(worldIn, pos);
		VoxelShape shape2 = getWindowBlockState(worldIn, pos).getShape(worldIn, pos);
		return Shapes.or(shape1, shape2);
	}

	@Override
	public VoxelShape getShape(@NonNull BlockState state, @NonNull BlockGetter worldIn, @NonNull BlockPos pos, @NonNull CollisionContext context) {
		return getCombinedShape(state, worldIn, pos, context);
	}

	@Override
	public VoxelShape getCollisionShape(@NonNull BlockState state, @NonNull BlockGetter worldIn, @NonNull BlockPos pos,
	                                    @NonNull CollisionContext context) {
		return getCombinedShape(state, worldIn, pos, context);
	}

	@Nullable
	private BlockState getLookedAtEmbeddedBlock(Level level, Player player, BlockPos pos,
	                                            WindowInABlockTileEntity wte) {

		double reach = player.getAttributeValue(Attributes.BLOCK_INTERACTION_RANGE);

		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getViewVector(1.0F).scale(reach));

		BlockHitResult hit = level.clip(new ClipContext(
				start,
				end,
				ClipContext.Block.OUTLINE,
				ClipContext.Fluid.NONE,
				player
		));

		if (!hit.getBlockPos().equals(pos))
			return null;

		Vec3 relative = hit.getLocation()
				.subtract(Vec3.atLowerCornerOf(pos));

		if (hitContains(wte.getWindowBlock(), level, pos, relative))
			return wte.getWindowBlock();

		if (hitContains(wte.getPartialBlock(), level, pos, relative))
			return wte.getPartialBlock();

		return null;
	}

	@Environment(EnvType.CLIENT)
	public VoxelShape getRaycastedShape(BlockGetter blockGetter, BlockPos blockPos) {

		WindowInABlockTileEntity wte = getTileEntity(blockGetter, blockPos);

		return getRaycastedShape(blockGetter, blockPos, wte);
	}

	@Environment(EnvType.CLIENT)
	public VoxelShape getRaycastedShape(BlockGetter blockGetter, BlockPos blockPos, WindowInABlockTileEntity wte) {

		if (wte == null)
			return Shapes.empty();

		Minecraft minecraft = Minecraft.getInstance();

		if (minecraft.hitResult instanceof BlockHitResult hit
				&& hit.getBlockPos().equals(blockPos)) {

			Vec3 relative = hit.getLocation()
					.subtract(Vec3.atLowerCornerOf(blockPos));

			if (hitContains(wte.getWindowBlock(), blockGetter, blockPos, relative)) {
				wte.hoveredBlock = wte.getWindowBlock();
				return wte.getWindowBlock().getShape(blockGetter, blockPos);
			}

			if (hitContains(wte.getPartialBlock(), blockGetter, blockPos, relative)) {
				wte.hoveredBlock = wte.getPartialBlock();
				return wte.getPartialBlock().getShape(blockGetter, blockPos);
			}
		}

		return Shapes.or(
				wte.getWindowBlock().getShape(blockGetter, blockPos),
				wte.getPartialBlock().getShape(blockGetter, blockPos)
		);
	}

	@Override
	public BlockState updateShape(@NonNull BlockState stateIn, @NonNull LevelReader level, @NonNull ScheduledTickAccess tickAccess,
	                              @NonNull BlockPos currentPos, @NonNull Direction facing, @NonNull BlockPos facingPos,
	                              @NonNull BlockState facingState, @NonNull RandomSource random) {
		WindowInABlockTileEntity wte = getTileEntity(level, currentPos);
		if (wte == null)
			return stateIn;
		BlockState windowNeighborState = resolveWindowNeighborState(level, facingPos, facingState);
		wte.setWindowBlock(
			wte.getWindowBlock().updateShape(level, tickAccess, currentPos, facing, facingPos, windowNeighborState, random));
		BlockState blockState =
			wte.getPartialBlock().updateShape(level, tickAccess, currentPos, facing, facingPos, facingState, random);
		if (blockState.getBlock() instanceof CrossCollisionBlock) {
			for (BooleanProperty side : Arrays.asList(CrossCollisionBlock.EAST, CrossCollisionBlock.NORTH, CrossCollisionBlock.SOUTH,
				CrossCollisionBlock.WEST))
				blockState = blockState.setValue(side, false);
			wte.setPartialBlock(blockState);
		}
		wte.requestModelDataUpdate();

		return stateIn;
	}

	static BlockState resolveWindowNeighborState(BlockGetter level, BlockPos neighborPos, BlockState rawNeighborState) {
		if (rawNeighborState.getBlock() instanceof WindowInABlockBlock) {
			BlockEntity neighborBe = level.getBlockEntity(neighborPos);
			if (neighborBe instanceof WindowInABlockTileEntity neighborTe)
				return neighborTe.getWindowBlock();
		}
		return rawNeighborState;
	}

	public BlockState getSurroundingBlockState(BlockGetter reader, BlockPos pos) {
		WindowInABlockTileEntity wte = getTileEntity(reader, pos);
		if (wte != null)
			return wte.getPartialBlock();
		return Blocks.AIR.defaultBlockState();
	}

	public BlockState getWindowBlockState(BlockGetter reader, BlockPos pos) {
		WindowInABlockTileEntity wte = getTileEntity(reader, pos);
		if (wte != null)
			return wte.getWindowBlock();
		return Blocks.AIR.defaultBlockState();
	}

	@Override
	public SoundType getSoundType(@NonNull BlockState blockState) {
		if (Thread.currentThread().getName().equals("Render thread")) { // only run on client
			if (blockState.getBlock() instanceof WindowInABlockBlock wbb) {
				Minecraft mc = Minecraft.getInstance();
				if (mc.level != null && mc.hitResult != null) {
					WindowInABlockTileEntity wte = wbb.getTileEntity(mc.level, ((BlockHitResult) mc.hitResult).getBlockPos());
					if (wte != null) {
						return wte.hoveredBlock.getSoundType();
					}
				}
			}
		}
		return super.getSoundType(blockState);
	}

	@Override
	public boolean skipRendering(@NonNull BlockState state, @NonNull BlockState adjacentBlockState, @NonNull Direction side) {
		return false;
	}

	@Nullable
	public WindowInABlockTileEntity getTileEntity(BlockGetter world, BlockPos pos) {
		BlockEntity blockEntity = world.getBlockEntity(pos);
		if (blockEntity instanceof WindowInABlockTileEntity wte)
			return wte;
		return null;
	}

	@Override
	@Nullable
	public BlockEntity newBlockEntity(@NonNull BlockPos pos, @NonNull BlockState blockState) {
		return new WindowInABlockTileEntity(pos, blockState);
	}
}
