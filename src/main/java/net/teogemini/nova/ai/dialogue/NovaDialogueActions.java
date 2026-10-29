package net.teogemini.nova.ai.dialogue;

import net.teogemini.nova.ai.dialogue.NovaDialogueBrain.Intent;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.entity.NovaWorkSupport;

/** Fixed action handlers. JSON chooses an existing intent; it cannot execute arbitrary code. */
public final class NovaDialogueActions {
    public record Result(String responseOverride, String detail) {
        public static Result ok() { return new Result("", ""); }
    }
    private NovaDialogueActions() {}

    public static Result execute(NovaEntity nova, Intent intent) { return execute(nova,intent,"vi_vn"); }

    public static Result execute(NovaEntity nova, Intent intent, String language) {
        if (!intent.command()) throw new IllegalArgumentException("Not an action: " + intent);
        if (nova.level().isClientSide()) throw new IllegalStateException("Dialogue actions require the server");
        if (nova.isNoAi()) return new Result("SYS_NO_AI", "");
        if (nova.companion().panicking() && intent != Intent.WORK_OFF) return new Result("SYS_PANIC", "");
        return switch (intent) {
            case FOLLOW -> move(nova, NovaEntity.MODE_FOLLOW);
            case FREE -> move(nova, NovaEntity.MODE_FREE);
            case WAIT -> move(nova, NovaEntity.MODE_WAIT);
            case SIT -> sit(nova, true);
            case STAND -> sit(nova, false);
            case WORK_ON -> work(nova, true, language);
            case WORK_OFF -> work(nova, false, language);
            case WAVE -> gesture(nova, NovaEntity.ACTION_WAVE);
            case CELEBRATE -> gesture(nova, NovaEntity.ACTION_CELEBRATE);
            case APOLOGIZE -> apologize(nova);
            case NOD -> companionGesture(nova, "nod", 0);
            case PROUD -> companionGesture(nova, "proud", 5);
            default -> throw new IllegalArgumentException("Unsupported action: " + intent);
        };
    }

    private static Result move(NovaEntity nova, int mode) {
        nova.setNovaMoveMode(mode);
        return Result.ok();
    }
    private static Result sit(NovaEntity nova, boolean sitting) {
        nova.setNovaSitRequested(sitting);
        return Result.ok();
    }
    private static Result work(NovaEntity nova, boolean enabled, String language) {
        nova.companion().ownerCommand();
        nova.setNovaWorkEnabled(enabled);
        nova.clearWorkAnimation();
        nova.getNavigation().stop();
        if (!enabled) return Result.ok();
        nova.setNovaMoveMode(NovaEntity.MODE_FREE);
        if (nova.isSwordNova()) {
            NovaWorkSupport.ensureOffhandSword(nova);
        }
        String blocker = NovaDialogueFacts.read(nova,language).blocker();
        return blocker.isEmpty() ? Result.ok() : new Result("SYS_WORK_PENDING", blocker);
    }
    private static Result gesture(NovaEntity nova, int action) {
        if (nova.isCarryingChest() || nova.isPerformingAction() || !nova.onGround() || nova.isInWater())
            return new Result("SYS_ACTION_BLOCKED", "");
        nova.startNovaAction(action);
        return nova.isPerformingAction() ? Result.ok() : new Result("SYS_NOT_STARTED", "");
    }
    private static Result apologize(NovaEntity nova) {
        if (nova.isCarryingChest() || nova.isPerformingAction())
            return new Result("SYS_ACTION_BLOCKED", "");
        nova.startApologizeAction();
        return Result.ok();
    }
    private static Result companionGesture(NovaEntity nova, String name, int face) {
        if (nova.isCarryingChest() || nova.isPerformingAction())
            return new Result("SYS_ACTION_BLOCKED", "");
        nova.companion().gesture(name);
        nova.showNovaFace(face, 45);
        return Result.ok();
    }
}
