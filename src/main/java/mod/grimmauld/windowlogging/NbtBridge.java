package mod.grimmauld.windowlogging;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;

final class NbtBridge {
	private NbtBridge() {
	}

	static ValueInput toValueInput(HolderLookup.Provider registries, CompoundTag tag) {
		return TagValueInput.create(ProblemReporter.DISCARDING, registries, tag);
	}
}
