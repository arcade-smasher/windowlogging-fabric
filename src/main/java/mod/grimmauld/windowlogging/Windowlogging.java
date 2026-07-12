package mod.grimmauld.windowlogging;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Windowlogging implements ModInitializer {
	public static final String MODID = "windowlogging";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public static final TagKey<Block> WINDOWABLE = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
			Identifier.fromNamespaceAndPath(MODID, "windowable"));
	public static final TagKey<Block> WINDOW = TagKey.create(net.minecraft.core.registries.Registries.BLOCK,
			Identifier.fromNamespaceAndPath(MODID, "window"));

	public static final ResourceKey<Block> WINDOW_IN_A_BLOCK_KEY =
			ResourceKey.create(net.minecraft.core.registries.Registries.BLOCK,
					Identifier.fromNamespaceAndPath(MODID, "window_in_a_block"));

	public static final WindowInABlockBlock WINDOW_IN_A_BLOCK =
			Registry.register(BuiltInRegistries.BLOCK, Identifier.fromNamespaceAndPath(MODID, "window_in_a_block"),
					new WindowInABlockBlock(WINDOW_IN_A_BLOCK_KEY));

	public static final BlockEntityType<WindowInABlockTileEntity> WINDOW_IN_A_BLOCK_TILE_ENTITY =
			Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
					Identifier.fromNamespaceAndPath(MODID, "window_in_a_block"),
					FabricBlockEntityTypeBuilder.create(WindowInABlockTileEntity::new, WINDOW_IN_A_BLOCK).build());

	@Override
	public void onInitialize() {
		UseBlockCallback.EVENT.register(EventListener::onUseBlock);
	}
}
