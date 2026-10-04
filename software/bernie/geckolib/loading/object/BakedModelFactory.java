package software.bernie.geckolib.loading.object;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jspecify.annotations.Nullable;
import software.bernie.geckolib.cache.model.BakedGeoModel;
import software.bernie.geckolib.cache.model.GeoBone;
import software.bernie.geckolib.cache.model.GeoQuad;
import software.bernie.geckolib.cache.model.GeoVertex;
import software.bernie.geckolib.cache.model.cuboid.CuboidGeoBone;
import software.bernie.geckolib.cache.model.cuboid.GeoCube;
import software.bernie.geckolib.loading.definition.geometry.GeometryDescription;
import software.bernie.geckolib.loading.json.raw.*;
import software.bernie.geckolib.util.GeckoLibUtil;
import software.bernie.geckolib.util.JsonUtil;

import java.util.List;
import java.util.Map;
import net.minecraft.class_2350;
import net.minecraft.class_243;

/**
 * Base interface for a factory of {@link BakedGeoModel} objects
 * <p>
 * Handled by default by GeckoLib, but custom implementations may be added by other mods for special needs
 */
public interface BakedModelFactory {
	Map<String, BakedModelFactory> FACTORIES = new Object2ObjectOpenHashMap<>(1);
	BakedModelFactory DEFAULT_FACTORY = new Builtin();

	/**
	 * Construct the output model from the given {@link GeometryTree}
	 */
	BakedGeoModel constructGeoModel(GeometryTree geometryTree);

	/**
	 * Construct a {@link GeoBone} from the relevant raw input data
	 *
	 * @param boneStructure The {@code BoneStructure} comprising the structure of the bone and its children
	 * @param properties The loaded properties for the model
	 * @param parent The parent bone for this bone, or null if a top-level bone
	 */
	GeoBone constructBone(BoneStructure boneStructure, GeometryDescription properties, @Nullable GeoBone parent);

	/**
	 * Construct a {@link GeoCube} from the relevant raw input data
	 *
	 * @param cube The raw {@code Cube} comprising the structure and properties of the cube
	 * @param properties The loaded properties for the model
	 * @param boneInflation The inflation value assigned to the bone this cube belongs to
	 */
	GeoCube constructCube(Cube cube, GeometryDescription properties, float boneInflation);

	/**
	 * Builtin method to construct the quad list from the various vertices and related data, to make it easier
	 * <p>
	 * Vertices have already been mirrored here if {@code mirror} is true
	 */
	default @Nullable GeoQuad[] buildQuads(UVUnion uvUnion, VertexSet vertices, Cube cube, float textureWidth, float textureHeight, boolean mirror) {
		@Nullable GeoQuad[] quads = new GeoQuad[6];

		quads[0] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, class_2350.field_11039);
		quads[1] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, class_2350.field_11034);
		quads[2] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, class_2350.field_11043);
		quads[3] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, class_2350.field_11035);
		quads[4] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, class_2350.field_11036);
		quads[5] = buildQuad(vertices, cube, uvUnion, textureWidth, textureHeight, mirror, class_2350.field_11033);

		return quads;
	}

	/**
	 * Build an individual quad
	 */
	default @Nullable GeoQuad buildQuad(VertexSet vertices, Cube cube, UVUnion uvUnion, float textureWidth, float textureHeight, boolean mirror, class_2350 direction) {
		return uvUnion.uvData().map(boxUvs -> {
			double[] uvSize = cube.size();
			class_243 uvSizeVec = new class_243(Math.floor(uvSize[0]), Math.floor(uvSize[1]), Math.floor(uvSize[2]));
			double[][] uvData = switch(direction) {
				case field_11039 -> new double[][] {
						new double[] {boxUvs[0] + uvSizeVec.field_1350 + uvSizeVec.field_1352, boxUvs[1] + uvSizeVec.field_1350},
						new double[] {uvSizeVec.field_1350, uvSizeVec.field_1351}
				};
				case field_11034 -> new double[][] {
						new double[] { boxUvs[0], boxUvs[1] + uvSizeVec.field_1350 },
						new double[] { uvSizeVec.field_1350, uvSizeVec.field_1351 }
				};
				case field_11043 -> new double[][] {
						new double[] {boxUvs[0] + uvSizeVec.field_1350, boxUvs[1] + uvSizeVec.field_1350},
						new double[] {uvSizeVec.field_1352, uvSizeVec.field_1351}
				};
				case field_11035 -> new double[][] {
						new double[] {boxUvs[0] + uvSizeVec.field_1350 + uvSizeVec.field_1352 + uvSizeVec.field_1350, boxUvs[1] + uvSizeVec.field_1350},
						new double[] {uvSizeVec.field_1352, uvSizeVec.field_1351 }
				};
				case field_11036 -> new double[][] {
						new double[] {boxUvs[0] + uvSizeVec.field_1350, boxUvs[1]},
						new double[] {uvSizeVec.field_1352, uvSizeVec.field_1350}
				};
				case field_11033 -> new double[][] {
						new double[] {boxUvs[0] + uvSizeVec.field_1350 + uvSizeVec.field_1352, boxUvs[1] + uvSizeVec.field_1350},
						new double[] {uvSizeVec.field_1352, -uvSizeVec.field_1350}
				};
			};

			return GeoQuad.build(vertices.verticesForQuad(direction, true, mirror || cube.mirror() == Boolean.TRUE), uvData[0], uvData[1], FaceUV.Rotation.NONE, textureWidth, textureHeight, mirror, direction);
		}, uvFaces -> {
			FaceUV faceUV = uvFaces.fromDirection(direction);

			if (faceUV == null)
				return null;

			return GeoQuad.build(vertices.verticesForQuad(direction, false, mirror || cube.mirror() == Boolean.TRUE), faceUV.uv(), faceUV.uvSize(),
								 faceUV.uvRotation(), textureWidth, textureHeight, mirror, direction);
		});
	}

	static BakedModelFactory getForNamespace(String namespace) {
		return FACTORIES.getOrDefault(namespace, DEFAULT_FACTORY);
	}

	/**
	 * Register a custom {@link BakedModelFactory} to handle loading models in a custom way
	 * <p>
	 * <b><u>MUST be called during mod construct</u></b>
	 * <p>
	 * It is recommended you don't call this directly, and instead call it via {@link GeckoLibUtil#addCustomBakedModelFactory}
	 *
	 * @param namespace The namespace (modid) to register the factory for
	 * @param factory The factory responsible for model loading under the given namespace
	 */
	static void register(String namespace, BakedModelFactory factory) {
		FACTORIES.put(namespace, factory);
	}

	final class Builtin implements BakedModelFactory {
		@Override
		public BakedGeoModel constructGeoModel(GeometryTree geometryTree) {
			List<GeoBone> bones = new ObjectArrayList<>();

			for (BoneStructure boneStructure : geometryTree.topLevelBones().values()) {
				bones.add(constructBone(boneStructure, geometryTree.properties(), null));
			}

			return new BakedGeoModel(bones.toArray(new GeoBone[0]), ModelProperties.fromDescription(geometryTree.properties()));
		}

		@Override
		public GeoBone constructBone(BoneStructure boneStructure, GeometryDescription properties, @Nullable GeoBone parent) {
			Bone bone = boneStructure.self();
            class_243 pivot = JsonUtil.arrayToVec(bone.pivot());
            class_243 rotation = JsonUtil.arrayToVec(bone.rotation());
            GeoBone[] childBones = new GeoBone[boneStructure.children().size()];
            GeoCube[] cubes = new GeoCube[bone.cubes().length];
            GeoBone newBone = new CuboidGeoBone(parent, bone.name(), childBones, cubes, (float)-pivot.field_1352, (float)pivot.field_1351, (float)pivot.field_1350,
                                                (float)Math.toRadians(-rotation.field_1352), (float)Math.toRadians(-rotation.field_1351), (float)Math.toRadians(rotation.field_1350));

            for (int i = 0; i < bone.cubes().length; i++) {
                cubes[i] = constructCube(bone.cubes()[i], properties, bone.inflate() == null ? 0f : bone.inflate().floatValue());
            }

            int i = 0;

            for (BoneStructure child : boneStructure.children().values()) {
                childBones[i++] = constructBone(child, properties, newBone);
            }

			return newBone;
		}

		@Override
		public GeoCube constructCube(Cube cube, GeometryDescription properties, float boneInflation) {
			boolean mirror = cube.mirror() == Boolean.TRUE;
			double inflate = cube.inflate() != null ? cube.inflate() / 16f : boneInflation / 16f;
			class_243 size = JsonUtil.arrayToVec(cube.size());
			class_243 origin = JsonUtil.arrayToVec(cube.origin());
			class_243 rotation = JsonUtil.arrayToVec(cube.rotation());
			class_243 pivot = JsonUtil.arrayToVec(cube.pivot());
			origin = new class_243(-(origin.field_1352 + size.field_1352) / 16d, origin.field_1351 / 16d, origin.field_1350 / 16d);
			class_243 vertexSize = size.method_18805(1 / 16d, 1 / 16d, 1 / 16d);

			pivot = pivot.method_18805(-1, 1, 1);
			rotation = new class_243(Math.toRadians(-rotation.field_1352), Math.toRadians(-rotation.field_1351), Math.toRadians(rotation.field_1350));
			@Nullable GeoQuad[] quads = buildQuads(cube.uv(), new VertexSet(origin, vertexSize, inflate), cube, (float)properties.textureWidth(), (float)properties.textureHeight(), mirror);

			return new GeoCube(quads, pivot, rotation, size);
		}
	}

	/**
	 * Holder class to make it easier to store and refer to vertices for a given cube
	 */
	record VertexSet(GeoVertex bottomLeftBack, GeoVertex bottomRightBack, GeoVertex topLeftBack, GeoVertex topRightBack,
                     GeoVertex topLeftFront, GeoVertex topRightFront, GeoVertex bottomLeftFront, GeoVertex bottomRightFront) {
		public VertexSet(class_243 origin, class_243 vertexSize, double inflation) {
			this(
					new GeoVertex(origin.field_1352 - inflation, origin.field_1351 - inflation, origin.field_1350 - inflation),
					new GeoVertex(origin.field_1352 - inflation, origin.field_1351 - inflation, origin.field_1350 + vertexSize.field_1350 + inflation),
					new GeoVertex(origin.field_1352 - inflation, origin.field_1351 + vertexSize.field_1351 + inflation, origin.field_1350 - inflation),
					new GeoVertex(origin.field_1352 - inflation, origin.field_1351 + vertexSize.field_1351 + inflation, origin.field_1350 + vertexSize.field_1350 + inflation),
					new GeoVertex(origin.field_1352 + vertexSize.field_1352 + inflation, origin.field_1351 + vertexSize.field_1351 + inflation, origin.field_1350 - inflation),
					new GeoVertex(origin.field_1352 + vertexSize.field_1352 + inflation, origin.field_1351 + vertexSize.field_1351 + inflation, origin.field_1350 + vertexSize.field_1350 + inflation),
					new GeoVertex(origin.field_1352 + vertexSize.field_1352 + inflation, origin.field_1351 - inflation, origin.field_1350 - inflation),
					new GeoVertex(origin.field_1352 + vertexSize.field_1352 + inflation, origin.field_1351 - inflation, origin.field_1350 + vertexSize.field_1350 + inflation));
		}

		/**
		 * Returns the normal vertex array for a west-facing quad
		 */
		public GeoVertex[] quadWest() {
			return new GeoVertex[] {this.topRightBack, this.topLeftBack, this.bottomLeftBack, this.bottomRightBack};
		}

		/**
		 * Returns the normal vertex array for an east-facing quad
		 */
		public GeoVertex[] quadEast() {
			return new GeoVertex[] {this.topLeftFront, this.topRightFront, this.bottomRightFront, this.bottomLeftFront};
		}

		/**
		 * Returns the normal vertex array for a north-facing quad
		 */
		public GeoVertex[] quadNorth() {
			return new GeoVertex[] {this.topLeftBack, this.topLeftFront, this.bottomLeftFront, this.bottomLeftBack};
		}

		/**
		 * Returns the normal vertex array for a south-facing quad
		 */
		public GeoVertex[] quadSouth() {
			return new GeoVertex[] {this.topRightFront, this.topRightBack, this.bottomRightBack, this.bottomRightFront};
		}

		/**
		 * Returns the normal vertex array for a top-facing quad
		 */
		public GeoVertex[] quadUp() {
			return new GeoVertex[] {this.topRightBack, this.topRightFront, this.topLeftFront, this.topLeftBack};
		}

		/**
		 * Returns the normal vertex array for a bottom-facing quad
		 */
		public GeoVertex[] quadDown() {
			return new GeoVertex[] {this.bottomLeftBack, this.bottomLeftFront, this.bottomRightFront, this.bottomRightBack};
		}

		/**
		 * Return the vertex array relevant to the quad being built, taking into account mirroring and quad type
		 */
		public GeoVertex[] verticesForQuad(class_2350 direction, boolean boxUv, boolean mirror) {
			return switch (direction) {
				case field_11039 -> mirror ? quadEast() : quadWest();
				case field_11034 -> mirror ? quadWest() : quadEast();
				case field_11043 -> quadNorth();
				case field_11035 -> quadSouth();
				case field_11036 -> mirror && !boxUv ? quadDown() : quadUp();
				case field_11033 -> mirror && !boxUv ? quadUp() : quadDown();
			};
		}
	}
}
