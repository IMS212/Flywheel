package com.jozufozu.flywheel.backend.instancing;

import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.api.instance.TickableInstance;
import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.batching.BatchingEngine;
import com.jozufozu.flywheel.backend.instancing.blockentity.BlockEntityInstanceManager;
import com.jozufozu.flywheel.backend.instancing.entity.EntityInstanceManager;
import com.jozufozu.flywheel.backend.instancing.instancing.InstancingEngine;
import com.jozufozu.flywheel.config.FlwEngine;
import com.jozufozu.flywheel.core.Contexts;
import com.jozufozu.flywheel.core.shader.WorldProgram;
import com.jozufozu.flywheel.event.BeginFrameEvent;
import com.jozufozu.flywheel.event.RenderLayerEvent;
import com.jozufozu.flywheel.util.ClientLevelExtension;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * A manager class for a single world where instancing is supported.
 * <p>
 *     The material manager is shared between the different instance managers.
 * </p>
 */
public class InstanceWorld {
	protected final Engine engine;
	protected final InstanceManager<Entity> entityInstanceManager;
	protected final InstanceManager<BlockEntity> blockEntityInstanceManager;

	protected final ParallelTaskEngine taskEngine;

	public InstanceWorld(LevelAccessor levelAccessor) {
		Level world = (Level) levelAccessor;

		this.taskEngine = new ParallelTaskEngine("Flywheel " + world.dimension().location());
		this.taskEngine.startWorkers();

		FlwEngine engine = Backend.getEngine();

		switch (engine) {
		case INSTANCING -> {
			InstancingEngine<WorldProgram> manager = InstancingEngine.builder(Contexts.WORLD)
					.build();

			entityInstanceManager = new EntityInstanceManager(manager);
			blockEntityInstanceManager = new BlockEntityInstanceManager(manager);

			manager.addListener(entityInstanceManager);
			manager.addListener(blockEntityInstanceManager);
			this.engine = manager;
		}
		case BATCHING -> {
			this.engine = new BatchingEngine();
			entityInstanceManager = new EntityInstanceManager(this.engine);
			blockEntityInstanceManager = new BlockEntityInstanceManager(this.engine);
		}
		default -> throw new IllegalArgumentException("Unknown engine type");
		}
	}

	public InstanceManager<Entity> getEntityInstanceManager() {
		return entityInstanceManager;
	}

	public InstanceManager<BlockEntity> getBlockEntityInstanceManager() {
		return blockEntityInstanceManager;
	}

	/**
	 * Free all acquired resources and invalidate this instance world.
	 */
	public void delete() {
		taskEngine.stopWorkers();
		engine.delete();
		entityInstanceManager.detachLightListeners();
		blockEntityInstanceManager.detachLightListeners();
	}

	/**
	 * Get ready to render a frame.
	 * <p>
	 *     Check and shift the origin coordinate.
	 *     <br>
	 *     Call {@link DynamicInstance#beginFrame()} on all instances in this world.
	 * </p>
	 */
	public void beginFrame(BeginFrameEvent event) {
		engine.beginFrame(event.getCamera());

		taskEngine.syncPoint();

		blockEntityInstanceManager.beginFrame(taskEngine, event.getCamera());
		entityInstanceManager.beginFrame(taskEngine, event.getCamera());
	}

	/**
	 * Tick the renderers after the game has ticked:
	 * <p>
	 *     Call {@link TickableInstance#tick()} on all instances in this world.
	 * </p>
	 */
	public void tick() {
		Minecraft mc = Minecraft.getInstance();
		Entity renderViewEntity = mc.cameraEntity != null ? mc.cameraEntity : mc.player;

		if (renderViewEntity == null) return;

		blockEntityInstanceManager.tick(taskEngine, renderViewEntity.getX(), renderViewEntity.getY(), renderViewEntity.getZ());
		entityInstanceManager.tick(taskEngine, renderViewEntity.getX(), renderViewEntity.getY(), renderViewEntity.getZ());
	}

	/**
	 * Draw the given layer.
	 */
	public void renderLayer(RenderLayerEvent event) {
		taskEngine.syncPoint();
		event.stack.pushPose();
		event.stack.translate(-event.camX, -event.camY, -event.camZ);
		engine.render(taskEngine, event);
		event.stack.popPose();
	}

	/**
	 * Instantiate all the necessary instances to render the given world.
	 */
	public void loadEntities(ClientLevel world) {
		// Block entities are loaded while chunks are baked.
		// Entities are loaded with the world, so when chunks are reloaded they need to be re-added.
		ClientLevelExtension.cast(world)
				.flywheel$getAllLoadedEntities()
				.forEach(entityInstanceManager::add);
	}

}
