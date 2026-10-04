package net.teogemini.nova.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CarvedPumpkinBlock;
import net.teogemini.nova.entity.NovaSummoning;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CarvedPumpkinBlock.class)
public abstract class NovaSummoningMixin {
    @Inject(method = "trySpawnGolem", at = @At("HEAD"), cancellable = true)
    private void nova$summon(Level level, BlockPos head, CallbackInfo ci) {
        if (!NovaSummoning.matches(level, head)) return;
        if (level instanceof ServerLevel server) NovaSummoning.summon(server, head);
        // A matching NOVA structure must never fall through to another golem recipe.
        ci.cancel();
    }

    @Inject(method = "canSpawnGolem", at = @At("HEAD"), cancellable = true)
    private void nova$allowDispenser(LevelReader level, BlockPos head,
                                    CallbackInfoReturnable<Boolean> cir) {
        if (NovaSummoning.matchesBase(level, head)) cir.setReturnValue(true);
    }
}
