package mod.grimmauld.windowlogging.mixin;

import mod.grimmauld.windowlogging.JadeIntegration;
import mod.grimmauld.windowlogging.WindowInABlockBlock;
import mod.grimmauld.windowlogging.WindowInABlockTileEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(Tool.class)
public class ToolMixin {

	@Inject(method = "isCorrectForDrops", at = @At("HEAD"), cancellable = true)
	private void isCorrectForDrops(BlockState blockState, CallbackInfoReturnable<Boolean> cir) {
		if (Thread.currentThread().getName().equals("Render thread") && blockState.getBlock() instanceof WindowInABlockBlock wbb) {
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null || mc.hitResult == null) return;
			WindowInABlockTileEntity wte = wbb.getTileEntity(mc.level, ((BlockHitResult) mc.hitResult).getBlockPos());
			if (wte == null) return;
			Tool tool = (Tool) (Object) this;
			for (Tool.Rule rule : tool.rules()) {
				if (rule.correctForDrops().isPresent() && wte.hoveredBlock.is(rule.blocks())) {
					cir.setReturnValue(rule.correctForDrops().get());
					cir.cancel();
					return;
				}
			}

			cir.setReturnValue(false);
			cir.cancel();
		}
	}
}
