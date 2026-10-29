package net.teogemini.nova.sound;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Tick-based limits: no background thread, queue, or search for nearby entities. */
public final class NovaSoundGate {
    private final long[] next;
    private long voiceUntil = Long.MIN_VALUE, mechanicalUntil = Long.MIN_VALUE;

    public NovaSoundGate(int cueCount) {
        next = new long[cueCount];
        Arrays.fill(next, Long.MIN_VALUE);
    }

    public boolean cooldownReady(int cue, long now) { return now >= next[cue]; }

    public boolean ready(int cue, long now, boolean voice) {
        return cooldownReady(cue, now) && now >= (voice ? voiceUntil : mechanicalUntil);
    }

    public boolean take(int cue, long now, int cooldown, int duration, boolean voice) {
        if (!ready(cue, now, voice)) return false;
        next[cue] = now + cooldown;
        if (voice) voiceUntil = now + duration;
        else mechanicalUntil = now + duration;
        return true;
    }

    public void quietUntil(long until) {
        voiceUntil = Math.max(voiceUntil, until);
        mechanicalUntil = Math.max(mechanicalUntil, until);
    }

    /** Each 32 x 32 block cell has its own one-second budget. Old cells expire. */
    public static final class Areas {
        public static final int MAX_CELLS = 1024;
        private final Map<Long, Window> cells = new HashMap<>();
        private long nextCleanup = Long.MIN_VALUE;

        public boolean take(long cell, long now, boolean voice) {
            if (now >= nextCleanup) {
                cells.values().removeIf(w -> now - w.start >= 40);
                nextCleanup = now + 200;
            }
            Window w = cells.get(cell);
            if (w == null) {
                if (cells.size() >= MAX_CELLS) return false;
                w = new Window(now);
                cells.put(cell, w);
            }
            if (now - w.start >= 20 || now < w.start) {
                w.start = now;
                w.voices = w.mechanical = 0;
            }
            if (voice) {
                if (w.voices >= 3) return false;
                w.voices++;
            } else {
                if (w.mechanical >= 10) return false;
                w.mechanical++;
            }
            return true;
        }

        public int size() { return cells.size(); }

        private static final class Window {
            long start;
            int voices, mechanical;
            Window(long start) { this.start = start; }
        }
    }
}
