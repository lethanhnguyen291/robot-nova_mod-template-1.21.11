package net.teogemini.nova.ai.dialogue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Bounded, transient conversations; no references to entities or stored transcripts. */
public final class NovaDialogueSessions {
    public static final int LIMIT = 256;
    private final Map<UUID, Session> sessions = new LinkedHashMap<>();

    public static final class Session {
        public String languagePreference="auto";
        public final UUID robot, token = UUID.randomUUID();
        public final String dimension;
        public final NovaDialogueBrain.Conversation context = new NovaDialogueBrain.Conversation();
        private long expires, nextMessage;
        private final long opened;
        private int sequence;
        private Session(UUID robot, String dimension, long now) {
            this.robot = robot;
            this.dimension = dimension;
            opened = now;
            expires = now + 6000;
        }
        public boolean accept(int requestedSequence, long now) {
            if (now >= expires || now < nextMessage || requestedSequence <= sequence || requestedSequence < 1) return false;
            sequence = requestedSequence;
            nextMessage = now + 20;
            expires = now + 6000;
            return true;
        }
    }

    public Session open(UUID player, UUID robot, String dimension, long now) {
        Session old = sessions.get(player);
        if (old != null && now < old.opened + 20) return null;
        sessions.entrySet().removeIf(e -> now >= e.getValue().expires);
        if (!sessions.containsKey(player) && sessions.size() >= LIMIT)
            sessions.remove(sessions.keySet().iterator().next());
        Session session = new Session(robot, dimension, now);
        sessions.put(player, session);
        return session;
    }

    public Session find(UUID player, UUID robot, UUID token, String dimension, long now) {
        Session s = sessions.get(player);
        if (s == null) return null;
        if (now >= s.expires) { sessions.remove(player); return null; }
        return s.robot.equals(robot) && s.token.equals(token) && s.dimension.equals(dimension) ? s : null;
    }
    public void disconnect(UUID player) { sessions.remove(player); }
    public int size() { return sessions.size(); }
}
