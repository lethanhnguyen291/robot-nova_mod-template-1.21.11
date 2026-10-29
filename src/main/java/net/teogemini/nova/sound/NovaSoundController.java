package net.teogemini.nova.sound;

import java.util.Map;
import java.util.WeakHashMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.sound.NovaSounds.Cue;

/** All world effects originate on the server, so a client never plays the same cue twice. */
public final class NovaSoundController {
    public static final float VOICE_VOLUME = 1.0F;
    public static final float MECHANICAL_VOLUME = 1.0F;
    private static final Map<ServerLevel, NovaSoundGate.Areas> AREAS = new WeakHashMap<>();
    private final NovaEntity nova;
    private static final Cue[] CUES = Cue.values();
    private final NovaSoundGate gate = new NovaSoundGate(CUES.length);
    private final NovaSoundQueue pending = new NovaSoundQueue();
    private boolean wasPanicking;
    private boolean bootPending, initialized, wasResting;
    private int lastFace = -1;
    private long nextIdle;

    public NovaSoundController(NovaEntity nova) { this.nova = nova; }

    public static void register() {
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> AREAS.clear());
    }

    public void spawned() { bootPending = true; }

    public void damaged() {
        pending.clear();
        gate.quietUntil(nova.level().getGameTime() + 25);
    }

    public boolean play(Cue cue) {
        if (!(nova.level() instanceof ServerLevel server) || !nova.isAlive()
                || nova.isRemoved() || nova.isSilent()) return false;
        long now = server.getGameTime();
        if (emit(cue, server, now)) return true;
        if (cue.voice && gate.cooldownReady(cue.ordinal(), now))
            pending.offer(cue.ordinal(), cue.priority(), now, 60);
        return false;
    }

    private boolean emit(Cue cue, ServerLevel server, long now) {
        if (!gate.ready(cue.ordinal(), now, cue.voice)) return false;
        int x = nova.blockPosition().getX() >> 5, z = nova.blockPosition().getZ() >> 5;
        long cell = ((long) x << 32) ^ (z & 0xffffffffL);
        if (!AREAS.computeIfAbsent(server, s -> new NovaSoundGate.Areas()).take(cell, now, cue.voice)) return false;
        gate.take(cue.ordinal(), now, cue.cooldown, cue.duration, cue.voice);
        if (pending.peek(now) == cue.ordinal()) pending.clear();
        float pitch = 0.96F + nova.getRandom().nextFloat() * 0.08F;
        nova.playSound(cue.event, cue.volume * (cue.voice ? VOICE_VOLUME : MECHANICAL_VOLUME), pitch);
        return true;
    }

    /** Intent comes from the server dialogue result, independent of the chosen language. */
    public void dialogue(net.teogemini.nova.ai.dialogue.NovaDialogueBrain.Intent intent, boolean blocked) {
        if (blocked) { play(Cue.HELP); return; }
        if (intent.command()) {
            switch (intent) {
                case WAVE, CELEBRATE -> { }
                case APOLOGIZE -> play(Cue.SAD);
                default -> play(Cue.ACK);
            }
            return;
        }
        play(switch (intent) {
            case HELLO -> Cue.GREET;
            case THANKS -> Cue.THANKS;
            case APOLOGY -> Cue.SAD;
            case COMFORT -> Cue.COMFORT;
            case PRAISE -> Cue.CELEBRATE;
            case UNKNOWN -> Cue.THINK;
            case GOODBYE -> Cue.ACK;
            default -> Cue.CHAT;
        });
    }

    public void tick() {
        if (!(nova.level() instanceof ServerLevel server) || !nova.isAlive() || nova.isRemoved()) return;
        long now = server.getGameTime();
        boolean resting = nova.isInSittingPose();
        if (!initialized) {
            initialized = true;
            wasResting = resting;
            lastFace = nova.getNovaFace();
            nextIdle = now + 400 + nova.getRandom().nextInt(401);
        }
        if (bootPending) {
            bootPending = false;
            play(Cue.BOOT);
        }
        int queued = pending.peek(now);
        if (queued >= 0) {
            if (nova.isSilent() || CUES[queued] == Cue.FEAR && !nova.companion().panicking()) pending.clear();
            else emit(CUES[queued], server, now);
        }
        if (nova.isNoAi()) return;
        boolean panicking = nova.companion().panicking();
        if (panicking && !wasPanicking) play(Cue.FEAR);
        wasPanicking = panicking;
        if (resting != wasResting) {
            wasResting = resting;
            play(resting ? Cue.SLEEP : Cue.WAKE);
        }
        if (nova.tickCount % 20 != 0) return;
        int face = nova.getNovaFace();
        if (face == 39 && lastFace != 39 && !resting && nova.hurtTime == 0) play(Cue.THINK);
        lastFace = face;
        if (now < nextIdle) return;
        nextIdle = now + 400 + nova.getRandom().nextInt(401);
        if (!resting && !nova.work().inventoryOpen() && !nova.isPerformingAction()
                && !nova.isCarryingChest() && !nova.companion().panicking()
                && nova.getWorkAction() == NovaEntity.WORK_NONE && nova.work().energy() > 100
                && nova.onGround() && nova.getNavigation().isDone()) play(Cue.IDLE);
    }
}
