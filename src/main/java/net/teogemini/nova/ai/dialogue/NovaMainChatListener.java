package net.teogemini.nova.ai.dialogue;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.teogemini.nova.entity.NovaEntity;

/**
 * Listens to main Minecraft chat bar messages and routes voice commands/conversations
 * to a specific Robot NOVA companion.
 */
public final class NovaMainChatListener {
    private static final Map<UUID, ActiveChatSession> SESSIONS = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT_TICKS = 900L; // 45 seconds timeout for follow-up talk

    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern NON_ALPHANUM = Pattern.compile("[^a-z0-9]+");
    private static final Pattern MULTI_SPACES = Pattern.compile("\\s+");
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^(?:nova|robot|bot)\\s*([1-9]\\d*)(?:\\s*(?:oi|a|nhe|nha|nhen|nghen))?[\\s,:-]*(.*)$");
    private static final Pattern GENERIC_PATTERN = Pattern.compile("^(?:nova|robot|bot)(?:\\s*(?:oi|a|nhe|nha|nhen|nghen))?[\\s,:-]*(.*)$");
    private static final Pattern LEADING_PARTICLES = Pattern.compile("^(?:oi|a|nhe|nha|nhen|nghen|cho|giup|dum|gium)[\\s,:-]*");

    public static final class ActiveChatSession {
        public final UUID robotUuid;
        public long expiresAtGameTime;
        public final NovaDialogueBrain.Conversation conversation;

        public ActiveChatSession(UUID robotUuid, long expiresAtGameTime, NovaDialogueBrain.Conversation conversation) {
            this.robotUuid = robotUuid;
            this.expiresAtGameTime = expiresAtGameTime;
            this.conversation = conversation;
        }
    }

    private record TargetResolution(NovaEntity target, String commandText, boolean explicitCall) {}

    private NovaMainChatListener() {}

    public static void register() {
        ServerMessageEvents.CHAT_MESSAGE.register((message, player, params) -> {
            onChatMessage(message, player);
        });

        ServerLifecycleEvents.SERVER_STOPPED.register(server -> SESSIONS.clear());

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            SESSIONS.remove(handler.player.getUUID());
        });
    }

    private static void onChatMessage(PlayerChatMessage message, ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;

        String rawContent = message.signedContent();
        if (rawContent == null || rawContent.isBlank()) return;

        long now = player.level().getServer().overworld().getGameTime();

        // Find alive, tamed, owned Novas within 32 blocks
        List<NovaEntity> nearbyNovas = player.level().getEntitiesOfClass(
                NovaEntity.class,
                player.getBoundingBox().inflate(32.0),
                n -> n.isAlive() && n.isTame() && n.isOwnedBy(player) && !n.isNoAi()
        );
        if (nearbyNovas.isEmpty()) return;

        // Sort by distance to player
        nearbyNovas.sort(Comparator.comparingDouble(n -> n.distanceToSqr(player)));

        // Retrieve active session if still valid
        ActiveChatSession currentSession = SESSIONS.get(player.getUUID());
        if (currentSession != null && now >= currentSession.expiresAtGameTime) {
            SESSIONS.remove(player.getUUID());
            currentSession = null;
        }

        TargetResolution resolution = resolveTarget(rawContent, nearbyNovas, currentSession, now);
        if (resolution == null) return;

        NovaEntity targetNova = resolution.target;
        String commandText = resolution.commandText;

        // Look at owner when interacting
        targetNova.getLookControl().setLookAt(player, 30.0f, 30.0f);

        // Ensure session exists or switch to target
        if (currentSession == null || !currentSession.robotUuid.equals(targetNova.getUUID())) {
            var conv = new NovaDialogueBrain.Conversation();
            conv.language(NovaDialogueLoader.select("auto", player.clientInformation().language()));
            currentSession = new ActiveChatSession(targetNova.getUUID(), now + SESSION_TIMEOUT_TICKS, conv);
        }

        String language = currentSession.conversation.language();
        String cleanInput = NovaDialogueBrain.clean(commandText);
        if (cleanInput.isBlank()) {
            cleanInput = "chao";
        }

        var match = NovaDialogueBrain.match(cleanInput, currentSession.conversation, now);
        var intent = match.intent();
        String answer;

        if (intent.command()) {
            var result = NovaDialogueActions.execute(targetNova, intent, language);
            answer = NovaDialogueBrain.actionAnswer(match, NovaDialogueFacts.read(targetNova, language),
                    currentSession.conversation, now, result);
            targetNova.sounds().dialogue(intent, !result.responseOverride().isEmpty());
        } else {
            answer = NovaDialogueBrain.answer(match, NovaDialogueFacts.read(targetNova, language),
                    currentSession.conversation, now);
            targetNova.sounds().dialogue(intent, false);
            NovaDialogueFacts.react(targetNova, intent);
        }

        // Format name tag
        String displayName;
        if (targetNova.hasCustomName()) {
            displayName = targetNova.getCustomName().getString();
        } else if (nearbyNovas.size() > 1) {
            int idx = nearbyNovas.indexOf(targetNova) + 1;
            displayName = "NOVA #" + idx;
        } else {
            displayName = "NOVA";
        }

        // Send styled response directly to owner's chat
        player.sendSystemMessage(
                Component.literal("[" + displayName + "] ")
                        .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD)
                        .append(Component.literal(answer).withStyle(ChatFormatting.WHITE))
        );

        // Manage session expiry or close
        if (intent == NovaDialogueBrain.Intent.GOODBYE) {
            SESSIONS.remove(player.getUUID());
        } else {
            currentSession.expiresAtGameTime = now + SESSION_TIMEOUT_TICKS;
            SESSIONS.put(player.getUUID(), currentSession);
        }
    }

    private static TargetResolution resolveTarget(String rawText, List<NovaEntity> nearbyNovas,
                                                  ActiveChatSession currentSession, long now) {
        String normText = normalize(rawText);
        if (normText.isEmpty()) return null;

        // 1. Check custom names (Name Tag)
        for (NovaEntity nova : nearbyNovas) {
            if (nova.hasCustomName()) {
                String customName = nova.getCustomName().getString().trim();
                String normName = normalize(customName);
                if (!normName.isEmpty()) {
                    if (normText.equals(normName)) {
                        return new TargetResolution(nova, "chao", true);
                    } else if (normText.startsWith(normName + " ")) {
                        String leftover = normText.substring(normName.length()).trim();
                        leftover = stripLeadingParticles(leftover);
                        return new TargetResolution(nova, leftover.isEmpty() ? "chao" : leftover, true);
                    }
                }
            }
        }

        // 2. Check numbered call ("Nova 1", "Nova 2", "Robot 1", "Bot 2")
        Matcher numMatcher = NUMBERED_PATTERN.matcher(normText);
        if (numMatcher.matches()) {
            try {
                int index = Integer.parseInt(numMatcher.group(1)) - 1;
                if (index >= 0 && index < nearbyNovas.size()) {
                    NovaEntity target = nearbyNovas.get(index);
                    String leftover = stripLeadingParticles(numMatcher.group(2).trim());
                    return new TargetResolution(target, leftover.isEmpty() ? "chao" : leftover, true);
                }
            } catch (NumberFormatException ignored) {}
        }

        // 3. Check generic call ("Nova", "Robot", "Bot")
        Matcher genMatcher = GENERIC_PATTERN.matcher(normText);
        if (genMatcher.matches()) {
            NovaEntity target = null;
            if (currentSession != null) {
                target = findNovaByUuid(currentSession.robotUuid, nearbyNovas);
            }
            if (target == null) {
                target = nearbyNovas.get(0); // Closest
            }
            String leftover = stripLeadingParticles(genMatcher.group(1).trim());
            return new TargetResolution(target, leftover.isEmpty() ? "chao" : leftover, true);
        }

        // 4. Follow-up in ongoing active session (without repeating name)
        if (currentSession != null) {
            NovaEntity activeNova = findNovaByUuid(currentSession.robotUuid, nearbyNovas);
            if (activeNova != null) {
                var testMatch = NovaDialogueBrain.match(rawText, currentSession.conversation, now);
                // Only intercept if message matches a valid intent (do not swallow regular player chat)
                if (testMatch.intent() != NovaDialogueBrain.Intent.UNKNOWN) {
                    return new TargetResolution(activeNova, rawText, false);
                }
            }
        }

        return null;
    }

    private static NovaEntity findNovaByUuid(UUID uuid, List<NovaEntity> list) {
        for (NovaEntity n : list) {
            if (n.getUUID().equals(uuid)) return n;
        }
        return null;
    }

    private static String stripLeadingParticles(String text) {
        if (text == null) return "";
        String s = text.stripLeading();
        Matcher m = LEADING_PARTICLES.matcher(s);
        if (m.find()) {
            s = s.substring(m.end()).stripLeading();
        }
        return s;
    }

    private static String normalize(String text) {
        if (text == null) return "";
        String plain = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("")
                .toLowerCase(Locale.ROOT).replace('đ', 'd');
        String stripped = MULTI_SPACES.matcher(NON_ALPHANUM.matcher(plain).replaceAll(" ").strip()).replaceAll(" ");
        return stripped;
    }
}
