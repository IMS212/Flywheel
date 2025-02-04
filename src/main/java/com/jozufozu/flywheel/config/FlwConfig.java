package com.jozufozu.flywheel.config;

import org.apache.commons.lang3.tuple.Pair;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.BooleanValue;
import net.minecraftforge.common.ForgeConfigSpec.EnumValue;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

public class FlwConfig {

	private static final FlwConfig INSTANCE = new FlwConfig();

	public final ClientConfig client;

	public FlwConfig() {
		Pair<ClientConfig, ForgeConfigSpec> client = new ForgeConfigSpec.Builder().configure(ClientConfig::new);

		this.client = client.getLeft();

		ModLoadingContext.get()
				.registerConfig(ModConfig.Type.CLIENT, client.getRight());
	}

	public static FlwConfig get() {
		return INSTANCE;
	}

	public FlwEngine getEngine() {
		return client.engine.get();
	}

	public boolean debugNormals() {
		return client.debugNormals.get();
	}

	public boolean limitUpdates() {
		return client.limitUpdates.get();
	}

	public static void init() {
	}

	public static class ClientConfig {
		public final EnumValue<FlwEngine> engine;
		public final BooleanValue debugNormals;
		public final BooleanValue limitUpdates;

		public ClientConfig(ForgeConfigSpec.Builder builder) {
			engine = builder.comment("Enable or disable the entire engine")
					.defineEnum("backend", FlwEngine.INSTANCING);

			debugNormals = builder.comment("Enable or disable a debug overlay that colors pixels by their normal")
					.define("debugNormals", false);

			limitUpdates = builder.comment("Enable or disable instance update limiting with distance.")
					.define("limitUpdates", true);
		}
	}
}
