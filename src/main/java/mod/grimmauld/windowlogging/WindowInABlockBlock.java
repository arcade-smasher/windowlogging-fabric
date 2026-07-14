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
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
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
		super(Properties.of().noOcclusion().setId(key).strength(2.0F, 3.0F));
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
	public ItemStack getCloneItemStack(@NonNull LevelReader level, @NonNull BlockPos blockPos, @NonNull BlockState state, boolean includeData) {
		BlockState surroundingState = getSurroundingBlockState(level, blockPos);
		return surroundingState.getBlock().getCloneItemStack(level, blockPos, surroundingState, includeData);
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
			List<ItemStack> combinedDrops = dropHelper(wte.getWindowBlock(), builder);
			combinedDrops.addAll(dropHelper(wte.getPartialBlock(), builder));
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

	@Override
	public BlockState updateShape(@NonNull BlockState stateIn, @NonNull LevelReader level, @NonNull ScheduledTickAccess tickAccess,
	                              @NonNull BlockPos currentPos, @NonNull Direction facing, @NonNull BlockPos facingPos,
	                              @NonNull BlockState facingState, @NonNull RandomSource random) {
		WindowInABlockTileEntity wte = getTileEntity(level, currentPos);
		if (wte == null)
			return stateIn;
//		BlockState windowNeighborState = resolveWindowNeighborState(level, facingPos, facingState);
		wte.setWindowBlock(
			wte.getWindowBlock().updateShape(level, tickAccess, currentPos, facing, facingPos, facingState, random));
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
						return wte.getPartialBlock().getSoundType();
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
