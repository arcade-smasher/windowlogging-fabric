package mod.grimmauld.windowlogging.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import mod.grimmauld.windowlogging.WindowInABlockBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public class ServerPlayerGameModeMixin {

	@Shadow
	protected ServerLevel level;

	@Shadow
	@Final
	protected ServerPlayer player;

	@Inject(
			method = "destroyBlock",
			at = @At(
					value = "RETURN",
					ordinal = 4
			)
	)
	private void cleanupDestroyBlock(BlockPos blockPos, CallbackInfoReturnable<Boolean> cir) {
		if (level.getBlockState(blockPos).getBlock() instanceof WindowInABlockBlock wbb) {
			wbb.cleanupDestroyedState();
		}
	}

	@Redirect(
			method = "destroyBlock",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerPlayer;hasCorrectToolForDrops(Lnet/minecraft/world/level/block/state/BlockState;)Z"
			)
	)
	private boolean redirectHasCorrectToolForDrops(ServerPlayer instance, BlockState blockState) {
		boolean result = instance.hasCorrectToolForDrops(blockState);
		System.out.println(result);
		return result;
	}

	@Redirect(
			method = "destroyBlock",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/server/level/ServerLevel;removeBlock(Lnet/minecraft/core/BlockPos;Z)Z"
			)
	)
	private boolean redirectRemoveBlock(ServerLevel level, BlockPos blockPos, boolean b) {
		BlockState blockState = level.getBlockState(blockPos);
		if (blockState.getBlock() instanceof WindowInABlockBlock wbb) {
			wbb.destroy(blockPos, blockState, level.getBlockEntity(blockPos), level, player);
			return true;
		}
		return level.removeBlock(blockPos, b);
	}
}
