package mod.grimmauld.windowlogging.mixin;

import mod.grimmauld.windowlogging.WindowInABlockBlock;
import mod.grimmauld.windowlogging.WindowInABlockTileEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public class PlayerMixin {

	@Inject(method = "hasCorrectToolForDrops", at = @At("HEAD"), cancellable = true)
	private void hasCorrectToolForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
		if (!(blockState.getBlock() instanceof WindowInABlockBlock wbb)) return;
		Player player = (Player) (Object) this;
		Level level = player.level();
		if (!level.isClientSide() && wbb.destroyedState != null) {
			cir.setReturnValue(!wbb.destroyedState.requiresCorrectToolForDrops()
					|| player.getInventory().getSelectedItem().isCorrectToolForDrops(wbb.destroyedState));
			cir.cancel();
		}
	}
}
