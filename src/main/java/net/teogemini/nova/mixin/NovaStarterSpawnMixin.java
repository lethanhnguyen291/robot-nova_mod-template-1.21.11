package net.teogemini.nova.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.storage.ServerLevelData;
import net.teogemini.nova.server.NovaStarterKit;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class NovaStarterSpawnMixin {
    @Inject(method = "setInitialSpawn", at = @At("RETURN"))
    private static void nova$initialBonusChest(ServerLevel level, ServerLevelData data, boolean bonusChest,
                                               boolean debug, LevelLoadListener listener, CallbackInfo ci) {
        if (!debug) NovaStarterKit.onInitialSpawn(level, data.getRespawnData().pos());
    }
}
