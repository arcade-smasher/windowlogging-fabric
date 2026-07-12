package mod.grimmauld.windowlogging;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelModifier;

@Environment(EnvType.CLIENT)
public class WindowloggingModelLoadingPlugin implements ModelLoadingPlugin {
	@Override
	public void initialize(Context pluginContext) {
		pluginContext.modifyBlockModelAfterBake().register(ModelModifier.WRAP_PHASE, (model, context) -> {
			if (!context.state().is(Windowlogging.WINDOW_IN_A_BLOCK))
				return model;
			return new WindowInABlockModel(model);
		});
	}
}
