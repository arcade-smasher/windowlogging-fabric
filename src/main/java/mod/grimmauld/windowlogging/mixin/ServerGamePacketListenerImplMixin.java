package mod.grimmauld.windowlogging.mixin;

import mod.grimmauld.windowlogging.WindowInABlockBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

// for some reason they don't give the player as an argument, so a mixin is needed
// to redirect pick block so we pick the correct block that the player is looking at (don't just default to surroundingBlock)
@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {

	@Shadow
	public ServerPlayer player;

	@Redirect(
			method = "handlePickItemFromBlock",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getCloneItemStack(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;Z)Lnet/minecraft/world/item/ItemStack;"
			)
	)
	private ItemStack redirectGetCloneItemStack(BlockState instance, LevelReader serverLevel, BlockPos blockPos, boolean bl) {
		if (instance.getBlock() instanceof WindowInABlockBlock block) {
			return block.getCloneItemStackWithPlayer(player, serverLevel, blockPos, bl);
		}
		return instance.getCloneItemStack(serverLevel, blockPos, bl);
	}
}
