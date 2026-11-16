package net.teogemini.nova.server;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Keeps legacy per-player receipts readable and records the one shared world bonus chest. */
public final class NovaStarterRewardsData extends SavedData {
    public static final Codec<NovaStarterRewardsData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(UUIDUtil.STRING_CODEC, Codec.BOOL).optionalFieldOf("players", Map.of())
                    .forGetter((NovaStarterRewardsData data) -> data.players),
            Codec.BOOL.optionalFieldOf("worldInitialized", false).forGetter(data -> data.worldInitialized),
            Codec.BOOL.optionalFieldOf("worldPending", false).forGetter(data -> data.worldPending),
            BlockPos.CODEC.optionalFieldOf("origin").forGetter(data -> data.origin),
            BlockPos.CODEC.optionalFieldOf("worldChest").forGetter(data -> data.worldChest),
            Codec.BOOL.optionalFieldOf("announced", false).forGetter(data -> data.announced)
    ).apply(instance, NovaStarterRewardsData::new));
    public static final SavedDataType<NovaStarterRewardsData> TYPE = new SavedDataType<>(
            "robot_nova_starter_rewards", NovaStarterRewardsData::new, CODEC, null);

    private final Map<UUID, Boolean> players;
    private boolean worldInitialized;
    private boolean worldPending;
    private Optional<BlockPos> origin;
    private Optional<BlockPos> worldChest;
    private boolean announced;

    public NovaStarterRewardsData() { this(Map.of(), false, false, Optional.empty(), Optional.empty(), false); }
    private NovaStarterRewardsData(Map<UUID, Boolean> players, boolean initialized, boolean pending,
                                   Optional<BlockPos> origin, Optional<BlockPos> chest, boolean announced) {
        this.players = new HashMap<>(players);
        this.worldInitialized = initialized;
        this.worldPending = pending;
        this.origin = origin.map(BlockPos::immutable);
        this.worldChest = chest.map(BlockPos::immutable);
        this.announced = announced;
    }
    public static NovaStarterRewardsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }
    public Optional<BlockPos> origin() { return origin; }
    public Optional<BlockPos> worldChest() { return worldChest; }
    public boolean worldPending() { return worldPending; }
    public boolean announced() { return announced; }

    public void beginWorldGift(BlockPos spawn) {
        if (worldInitialized) return;
        worldInitialized = true;
        // Do not award a second kit to worlds that already received the earlier per-player kit.
        if (players.containsValue(true)) { setDirty(); return; }
        origin = Optional.of(spawn.immutable());
        worldPending = true;
        setDirty();
    }
    public void migrateLegacy(BlockPos spawn) {
        if (!worldInitialized) beginWorldGift(spawn);
    }
    public void completeWorldGift(BlockPos pos) {
        worldInitialized = true;
        worldPending = false;
        worldChest = Optional.of(pos.immutable());
        setDirty();
    }
    public void markAnnounced() { announced = true; setDirty(); }
}
