package mod.grimmauld.windowlogging.mixin;

import mod.grimmauld.windowlogging.WindowInABlockBlock;
import mod.grimmauld.windowlogging.WindowInABlockTileEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Environment(EnvType.CLIENT)
@Mixin(ClientLevel.class)
public class ClientLevelMixin {

	@Redirect(
			method = "addDestroyBlockEffect",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
			)
	)
	private VoxelShape redirectGetShapeDestroyBlockEffect(BlockState instance, BlockGetter blockGetter, BlockPos blockPos) {
		return getShape(instance, blockGetter, blockPos);
	}

	@Redirect(
			method = "addBreakingBlockEffect",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
			)
	)
	private VoxelShape redirectGetShapeBreakingBlockEffect(BlockState instance, BlockGetter blockGetter, BlockPos blockPos) {
		return getShape(instance, blockGetter, blockPos);
	}

	@Unique
	private VoxelShape getShape(BlockState instance, BlockGetter blockGetter, BlockPos blockPos) {
		if (instance.getBlock() instanceof WindowInABlockBlock wbb) {
			WindowInABlockTileEntity wte = wbb.getTileEntity(blockGetter, blockPos);
			if (wte != null && wte.hoveredBlock != Blocks.AIR.defaultBlockState()) {
				return wte.hoveredBlock.getShape(blockGetter, blockPos);
			}
			return wbb.getCombinedShape(instance, blockGetter, blockPos);
		} else {
			return instance.getShape(blockGetter, blockPos);
		}
	}
}
