package mod.grimmauld.windowlogging.mixin;

import mod.grimmauld.windowlogging.WindowInABlockBlock;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {

	@Shadow
	@Final
	private Minecraft minecraft;

	// need to predict on the client too
	@Redirect(
			method = "destroyBlock",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"
			)
	)
	private boolean redirectSetBlock(Level level, BlockPos blockPos, BlockState fluidBlockState, int i) {
		BlockState blockState = level.getBlockState(blockPos);
		if (blockState.getBlock() instanceof WindowInABlockBlock wbb) {
			wbb.destroy(blockPos, blockState, level.getBlockEntity(blockPos), level, minecraft.player);
			return true;
		}
		return level.setBlock(blockPos, fluidBlockState, i);
	}

	@Inject(method = "startDestroyBlock", at = @At("HEAD"))
	public void injectStartDestroyBlock(BlockPos blockPos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (!(minecraft.level.getBlockState(blockPos).getBlock() instanceof WindowInABlockBlock)) {
			WindowInABlockBlock.breakingBlock = blockPos;
			WindowInABlockBlock.breakingSide = -1;
		}
	}
}
