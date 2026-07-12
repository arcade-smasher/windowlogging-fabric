package mod.grimmauld.windowlogging;

import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;
import mod.grimmauld.windowlogging.mixin.JadeHarvestToolProviderAccessor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class JadeIntegration {

	public static Cache<BlockState, ImmutableList<ItemStack>> getHarvestBlockProviderCache() {
		return ((JadeHarvestToolProviderAccessor) snownee.jade.addon.harvest.HarvestToolProvider.INSTANCE).resultCache();
	}
}
