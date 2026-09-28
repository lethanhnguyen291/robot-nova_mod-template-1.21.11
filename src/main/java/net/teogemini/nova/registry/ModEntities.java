package net.teogemini.nova.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.entity.NovaEntity;

public final class ModEntities {
    private static final ResourceKey<EntityType<?>> NOVA_KEY =
            ResourceKey.create(
                    Registries.ENTITY_TYPE,
                    ROBOTNOVA_MOD.id("nova")
            );

    public static final EntityType<NovaEntity> NOVA = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            NOVA_KEY,
            EntityType.Builder.<NovaEntity>of(
                            NovaEntity::new,
                            MobCategory.CREATURE
                    )
                    .sized(0.8F, 1.2F)
                    .eyeHeight(0.8F)
                    .clientTrackingRange(8)
                    .build(NOVA_KEY)
    );

    public static void register() {
        FabricDefaultAttributeRegistry.register(
                NOVA,
                NovaEntity.createAttributes()
        );
    }

    private ModEntities() {
    }
}