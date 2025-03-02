package dev.engine_room.flywheel.lib.model.baked;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

class UniversalMeshEmitter implements VertexConsumer {
	private final Reference2ReferenceMap<RenderType, MeshEmitter> emitterMap;
	private final WrapperModel wrapperModel = new WrapperModel();

	@UnknownNullability
	private RenderType defaultLayer;
	@UnknownNullability
	private BufferBuilder currentDelegate;

    UniversalMeshEmitter(Reference2ReferenceMap<RenderType, MeshEmitter> emitterMap) {
		this.emitterMap = emitterMap;
    }

	public void prepare(RenderType defaultLayer) {
		this.defaultLayer = defaultLayer;
	}

	public void clear() {
		wrapperModel.setWrapped(null);
	}

	public BakedModel wrapModel(BakedModel model) {
		wrapperModel.setWrapped(model);
		return wrapperModel;
	}

	private void prepareForGeometry(RenderMaterial material) {
		BlendMode blendMode = material.blendMode();
		RenderType layer = blendMode == BlendMode.DEFAULT ? defaultLayer : blendMode.blockRenderLayer;
		boolean shade = !material.disableDiffuse();
		currentDelegate = emitterMap.get(layer).getBuffer(shade);
	}

	@Override
	public VertexConsumer addVertex(float x, float y, float z) {
		currentDelegate.addVertex(x, y, z);
		return this;
	}

	@Override
	public VertexConsumer setColor(int red, int green, int blue, int alpha) {
		currentDelegate.setColor(red, green, blue, alpha);
		return this;
	}

	@Override
	public VertexConsumer setUv(float u, float v) {
		currentDelegate.setUv(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv1(int u, int v) {
		currentDelegate.setUv1(u, v);
		return this;
	}

	@Override
	public VertexConsumer setUv2(int u, int v) {
		currentDelegate.setUv2(u, v);
		return this;
	}

	@Override
	public VertexConsumer setNormal(float x, float y, float z) {
		currentDelegate.setNormal(x, y, z);
		return this;
	}

	@Override
	public void addVertex(float x, float y, float z, int color, float u, float v, int packedOverlay, int packedLight, float normalX, float normalY, float normalZ) {
		currentDelegate.addVertex(x, y, z, color, u, v, packedOverlay, packedLight, normalX, normalY, normalZ);
	}

	@Override
	public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay) {
		currentDelegate.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
	}

	@Override
	public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float red, float green, float blue, float alpha, int[] lightmap, int packedOverlay, boolean readAlpha) {
		currentDelegate.putBulkData(pose, quad, brightness, red, green, blue, alpha, lightmap, packedOverlay, readAlpha);
	}

	private class WrapperModel implements BakedModel {
		private final QuadTransform quadTransform = quad -> {
			UniversalMeshEmitter.this.prepareForGeometry(quad.material());
			return true;
		};
		private @Nullable BakedModel wrapped;

		public void setWrapped(@Nullable BakedModel wrapped) {
			this.wrapped = wrapped;
		}

		@Override
		public boolean isVanillaAdapter() {
			return false;
		}

		@Override
		public void emitBlockQuads(QuadEmitter emitter, BlockAndTintGetter blockView, BlockState state, BlockPos pos, Supplier<RandomSource> randomSupplier, Predicate<@Nullable Direction> cullTest) {
			emitter.pushTransform(quadTransform);
			BakedModel.super.emitBlockQuads(emitter, blockView, state, pos, randomSupplier, cullTest);
			emitter.popTransform();
		}

		@Override
		public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
			return wrapped.getQuads(state, direction, random);
		}

		@Override
		public boolean useAmbientOcclusion() {
			return wrapped.useAmbientOcclusion();
		}

		@Override
		public boolean isGui3d() {
			return wrapped.isGui3d();
		}

		@Override
		public boolean usesBlockLight() {
			return wrapped.usesBlockLight();
		}

		@Override
		public TextureAtlasSprite getParticleIcon() {
			return wrapped.getParticleIcon();
		}

		@Override
		public ItemTransforms getTransforms() {
			return wrapped.getTransforms();
		}
	}
}
