package mod.grimmauld.windowlogging.mixin;

import mod.grimmauld.windowlogging.JadeIntegration;
import mod.grimmauld.windowlogging.WindowInABlockBlock;
import mod.grimmauld.windowlogging.WindowInABlockTileEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Environment(EnvType.CLIENT)
@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

	@Unique
	private String hoveredBlockCache;

	@Unique
	private int jadeLoaded = -1;

	@Redirect(
			method = "extractBlockOutline",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/world/level/block/state/BlockState;getShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/phys/shapes/CollisionContext;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
			)
	)
	private VoxelShape redirectGetShapeBlockOutline(BlockState instance, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
		if (instance.getBlock() instanceof WindowInABlockBlock wbb) {
			WindowInABlockTileEntity wte = wbb.getTileEntity(blockGetter, blockPos);
			if (wte != null) {
				if (jadeLoaded == -1) jadeLoaded = FabricLoader.getInstance().isModLoaded("jade") ? 1 : 0;
				if (jadeLoaded == 1) {
					String tmpStr = wte.hoveredBlock.toString();
					if (!Objects.equals(hoveredBlockCache, tmpStr)) {
						JadeIntegration.getHarvestBlockProviderCache().invalidate(instance);
					}
					hoveredBlockCache = tmpStr;
				}
				return wbb.getRaycastedShape(blockGetter, blockPos, wte);
			}
		}
		return instance.getShape(blockGetter, blockPos, collisionContext);
	}
}
