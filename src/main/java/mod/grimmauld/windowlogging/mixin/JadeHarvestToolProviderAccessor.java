package mod.grimmauld.windowlogging.mixin;

import com.google.common.cache.Cache;
import com.google.common.collect.ImmutableList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import snownee.jade.addon.harvest.HarvestToolProvider;

@Mixin(HarvestToolProvider.class)
public interface JadeHarvestToolProviderAccessor {
	@Accessor("resultCache")
	Cache<BlockState, ImmutableList<ItemStack>> resultCache();

	@Accessor("resultCache")
	void resultCache(Cache<BlockState, ImmutableList<ItemStack>> resultCache);
}
