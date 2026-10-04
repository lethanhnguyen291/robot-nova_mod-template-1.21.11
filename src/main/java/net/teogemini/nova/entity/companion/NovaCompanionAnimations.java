package net.teogemini.nova.entity.companion;

import net.teogemini.nova.entity.NovaEntity;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

public final class NovaCompanionAnimations {
    private NovaCompanionAnimations() {}
    public static void register(NovaEntity nova, AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<NovaEntity>("companion_gesture",6,state -> {
            if (nova.isCarryingChest() || nova.isPerformingAction() || nova.getWorkAction()!=NovaEntity.WORK_NONE)
                return PlayState.STOP;
            if (nova.getCompanionState()==NovaCompanion.RESTING)
                return state.setAndContinue(RawAnimation.begin().thenLoop("animation.nova.companion_sleep"));
            return PlayState.STOP;
        }).triggerableAnim("nod",RawAnimation.begin().thenPlay("animation.nova.companion_nod"))
          .triggerableAnim("inspect",RawAnimation.begin().thenPlay("animation.nova.companion_inspect"))
          .triggerableAnim("point",RawAnimation.begin().thenPlay("animation.nova.companion_point"))
          .triggerableAnim("proud",RawAnimation.begin().thenPlay("animation.nova.companion_proud"))
          .triggerableAnim("rain",RawAnimation.begin().thenPlay("animation.nova.companion_rain")));
    }
}
