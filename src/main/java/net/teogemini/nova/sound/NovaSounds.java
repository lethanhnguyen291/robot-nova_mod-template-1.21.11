package net.teogemini.nova.sound;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.teogemini.nova.ROBOTNOVA_MOD;

/** Short, mono sound effects. Assets can be replaced without changing the AI. */
public final class NovaSounds {
    private NovaSounds() {}

    public enum Cue {
        BOOT("boot", .65F, 40, 26, true),
        IDLE("idle", .28F, 400, 18, true),
        GREET("greet", .55F, 60, 20, true),
        CELEBRATE("celebrate", .60F, 80, 28, true),
        ACK("ack", .40F, 15, 10, true),
        CHAT("chat", .42F, 25, 22, true),
        CURIOUS("curious", .35F, 100, 18, true),
        HURT("hurt", .55F, 8, 12, true),
        DEATH("death", .65F, 40, 30, true),
        STEP("step", .22F, 5, 4, false),
        WORK("work", .25F, 10, 6, false),
        RECHARGE("recharge", .55F, 16, 18, true),
        LOW_BATTERY("low_battery", .48F, 1200, 24, true),
        REPAIR("repair", .22F, 100, 8, false),
        COMPLETE("complete", .48F, 200, 20, true),
        HELP("help", .42F, 300, 20, true),
        LEVEL_UP("level_up", .60F, 40, 28, true),
        SLEEP("sleep", .30F, 80, 20, true),
        WAKE("wake", .35F, 80, 16, true),
        TAME("tame", .58F, 100, 28, true),
        THANKS("thanks", .43F, 45, 20, true),
        SAD("sad", .36F, 60, 24, true),
        COMFORT("comfort", .36F, 60, 24, true),
        FEAR("fear", .42F, 160, 22, true),
        THINK("think", .30F, 100, 18, true),
        EQUIP("equip", .28F, 10, 5, false),
        LINK("link", .45F, 30, 18, true),
        CHEST_LIFT("chest_lift", .33F, 15, 9, false),
        CHEST_DROP("chest_drop", .33F, 15, 8, false),
        BAG_OPEN("bag_open", .25F, 8, 5, false),
        BAG_CLOSE("bag_close", .23F, 8, 5, false),
        ATTACK("attack", .30F, 8, 6, false),
        WORK_CHOP("work_chop", .25F, 10, 6, false),
        WORK_MINE("work_mine", .26F, 10, 6, false),
        WORK_DIG("work_dig", .24F, 10, 6, false),
        WORK_FARM("work_farm", .22F, 10, 6, false),
        WORK_PLANT("work_plant", .22F, 10, 6, false),
        RALLY("rally", .48F, 60, 22, true),
        DISMISS("dismiss", .40F, 40, 18, true),
        POWER_DOWN("power_down", .48F, 80, 30, true),
        REACTIVATE("reactivate", .56F, 60, 30, true),
        DORMANT_PLACE("dormant_place", .36F, 10, 9, false),
        DORMANT_PICKUP("dormant_pickup", .36F, 10, 10, false),
        DORMANT_HIT("dormant_hit", .28F, 8, 6, false),
        POSE_OFF("pose_off", .27F, 8, 8, false),
        POSE_GUARD("pose_guard", .27F, 8, 7, false),
        POSE_SIT("pose_sit", .27F, 8, 8, false),
        POSE_WAVE("pose_wave", .27F, 8, 8, false),
        POSE_FIGHT("pose_fight", .28F, 8, 8, false),
        REPAIR_METAL("repair_metal", .35F, 15, 12, false),
        RECHARGE_FULL("recharge_full", .54F, 40, 26, true),
        GIFT("gift", .38F, 100, 20, true),
        SOCIAL_HELLO("social_hello", .32F, 200, 20, true),
        RAIN("rain", .28F, 600, 20, true),
        BUILD_PLACE("build_place", .22F, 12, 6, false),
        SCAFFOLD_PLACE("scaffold_place", .22F, 10, 6, false),
        SCAFFOLD_REMOVE("scaffold_remove", .22F, 10, 6, false),
        SCAFFOLD_CLIMB("scaffold_climb", .18F, 12, 6, false);

        public final String path;
        public final float volume;
        public final int cooldown, duration;
        public final boolean voice;
        public final SoundEvent event;

        Cue(String path, float volume, int cooldown, int duration, boolean voice) {
            this.path = path;
            this.volume = volume;
            this.cooldown = cooldown;
            this.duration = duration;
            this.voice = voice;
            this.event = SoundEvent.createVariableRangeEvent(ROBOTNOVA_MOD.id("entity.nova." + path));
        }

        public boolean allowed(boolean dormant) {
            if (!dormant) return true;
            return switch (this) {
                case POWER_DOWN, DORMANT_PLACE, DORMANT_PICKUP, DORMANT_HIT,
                        POSE_OFF, POSE_GUARD, POSE_SIT, POSE_WAVE, POSE_FIGHT -> true;
                default -> false;
            };
        }

        public int priority() {
            return switch (this) {
                case IDLE, CURIOUS, THINK, SLEEP, WAKE -> 0;
                case BOOT, TAME, RECHARGE, LOW_BATTERY, HELP, LEVEL_UP, FEAR, POWER_DOWN, REACTIVATE, RECHARGE_FULL -> 3;
                default -> 2;
            };
        }
    }

    public static void register() {
        for (Cue cue : Cue.values()) {
            Registry.register(BuiltInRegistries.SOUND_EVENT,
                    ROBOTNOVA_MOD.id("entity.nova." + cue.path), cue.event);
        }
    }
}
