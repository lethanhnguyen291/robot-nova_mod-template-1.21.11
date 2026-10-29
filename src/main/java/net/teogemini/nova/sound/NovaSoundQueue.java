package net.teogemini.nova.sound;

/** At most one short-lived voice cue waits; repeated clicks never build a backlog. */
public final class NovaSoundQueue {
    private int cue = -1, priority;
    private long expires;

    public boolean offer(int id, int importance, long now, int lifetime) {
        peek(now);
        if (importance <= 0 || lifetime <= 0 || cue == id || cue >= 0 && priority > importance) return false;
        cue = id;
        priority = importance;
        expires = now + lifetime;
        return true;
    }

    public int peek(long now) {
        if (cue >= 0 && now >= expires) clear();
        return cue;
    }

    public void clear() { cue = -1; priority = 0; }
}
