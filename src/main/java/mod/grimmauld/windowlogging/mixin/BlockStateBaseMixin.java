package mod.grimmauld.windowlogging.mixin;

import mod.grimmauld.windowlogging.WindowInABlockBlock;
import mod.grimmauld.windowlogging.WindowInABlockTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// TODO: implement requiresCorrectToolForDrops handling for WindowInABlockBlock embedded block
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

	@Shadow
	public abstract Block getBlock();

	@Inject(method = "requiresCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void requiresCorrectToolForDrops(CallbackInfoReturnable<Boolean> cir) {
		if (Thread.currentThread().getName().equals("Render thread") && getBlock() instanceof WindowInABlockBlock wbb) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.hitResult != null && mc.level != null) {
				WindowInABlockTileEntity wte = wbb.getTileEntity(mc.level, ((BlockHitResult) (mc.hitResult)).getBlockPos());
				cir.setReturnValue(wte.hoveredBlock.requiresCorrectToolForDrops());
			}
			cir.cancel();
		}
	}
}
