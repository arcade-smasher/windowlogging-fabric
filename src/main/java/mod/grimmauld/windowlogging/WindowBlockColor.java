package mod.grimmauld.windowlogging;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.ColorProviderRegistry;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

@Environment(EnvType.CLIENT)
public class WindowBlockColor implements BlockColor {
	public static void registerFor(WindowInABlockBlock block) {
		ColorProviderRegistry.BLOCK.register(new WindowBlockColor(), block);
	}

	@Override
	public int getColor(BlockState state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int color) {
		if (!(state.getBlock() instanceof WindowInABlockBlock wbb) || world == null || pos == null)
			return -1;
		BlockState surrounding = wbb.getSurroundingBlockState(world, pos);
		BlockColor provider = ColorProviderRegistry.BLOCK.get(surrounding.getBlock());
		if (provider == null)
			return -1;
		return provider.getColor(surrounding, world, pos, color);
	}
}
