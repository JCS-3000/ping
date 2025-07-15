package org.jcs.egm.dimension;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.jcs.egm.dimension.SoulRealmChunkGenerator;

/**
 * Handles registration of the Soul Realm dimension and its chunk generator.
 */
public class ModDimensions {

    // ResourceKey for the Soul Realm dimension
    public static final ResourceKey<Level> SOUL_REALM = ResourceKey.create(
            Registries.DIMENSION,
            new ResourceLocation("egm", "soul_realm")
    );

    // DeferredRegister for custom ChunkGenerator codecs (mod id: "egm")
    public static final DeferredRegister<Codec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, "egm");

    // Register the SoulRealmChunkGenerator codec
    public static final RegistryObject<Codec<? extends ChunkGenerator>> SOUL_REALM_CHUNK_GENERATOR =
            CHUNK_GENERATORS.register("soul_realm_chunk_generator", () -> SoulRealmChunkGenerator.CODEC);

    public static void register(IEventBus eventBus) {
        CHUNK_GENERATORS.register(eventBus);
    }
}
