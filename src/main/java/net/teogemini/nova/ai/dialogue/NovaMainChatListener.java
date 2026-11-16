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
 * to a specific Robot NOVA companion, or broadcasts /NOVA:[CÂU RA LỆNH] to all owned NOVAs.
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

    // Matches "/NOVA:[CÂU RA LỆNH]" or "NOVA:[CÂU RA LỆNH]" in chat
    private static final Pattern CHAT_BROADCAST_PATTERN = Pattern.compile("(?i)^/?nova\\s*:\\s*(.*)$");
    // Matches command packets where leading '/' was already stripped by the client: "NOVA:[CÂU RA LỆNH]" or "NOVA [CÂU RA LỆNH]"
    private static final Pattern CMD_PACKET_BROADCAST_PATTERN = Pattern.compile("(?i)^nova(?:\\s*:\\s*|\\s+|$)(.*)$");
    private static final Pattern BROADCAST_ALL_PREFIX = Pattern.compile("^(?:tat ca|dong loat|toan bo|all)\\s+");

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

    /**
     * Called from NovaCommandBroadcastMixin when a player types "/NOVA:[CÂU RA LỆNH]" in the chat bar.
     * Returns true if the command was handled as a NOVA broadcast so Brigadier does not reject it.
     */
    public static boolean tryHandleCommandPacket(ServerPlayer player, String rawCommand) {
        if (player == null || rawCommand == null) return false;
        String trimmed = rawCommand.strip();
        Matcher matcher = CMD_PACKET_BROADCAST_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return false;
        }
        String commandPart = matcher.group(1) != null ? matcher.group(1).strip() : "";
        executeBroadcast(player, commandPart);
        return true;
    }

    /**
     * Checks if a normal chat message uses the "NOVA:[CÂU RA LỆNH]" or "/NOVA:[CÂU RA LỆNH]" broadcast format.
     */
    public static boolean tryHandleBroadcastChat(ServerPlayer player, String rawContent) {
        if (player == null || rawContent == null) return false;
        String trimmed = rawContent.strip();
        Matcher matcher = CHAT_BROADCAST_PATTERN.matcher(trimmed);
        if (!matcher.matches()) {
            return false;
        }
        String commandPart = matcher.group(1) != null ? matcher.group(1).strip() : "";
        executeBroadcast(player, commandPart);
        return true;
    }

    private static void executeBroadcast(ServerPlayer player, String rawCommandPart) {
        if (!player.isAlive() || player.isSpectator()) return;

        String commandPart = rawCommandPart == null ? "" : rawCommandPart.strip();
        // Strip surrounding brackets if player typed /NOVA:[ngồi xuống]
        while (commandPart.length() >= 2 && commandPart.startsWith("[") && commandPart.endsWith("]")) {
            commandPart = commandPart.substring(1, commandPart.length() - 1).strip();
        }

        List<NovaEntity> allNovas = player.level().getEntitiesOfClass(
                NovaEntity.class,
                player.getBoundingBox().inflate(128.0),
                n -> n.isAlive() && n.isTame() && n.isOwnedBy(player) && !n.isDormant() && !n.isNoAi()
        );
        allNovas.sort(Comparator.comparingDouble(n -> n.distanceToSqr(player)));

        if (allNovas.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("[NOVA] Không có NOVA nào đang hoạt động ở gần để nhận lệnh.")
                            .withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        if (commandPart.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("[NOVA] Hãy nhập câu ra lệnh sau /NOVA: (VD: /NOVA:ngồi xuống, /NOVA:đi theo, /NOVA:làm việc)")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        String normCmd = normalize(commandPart);
        Matcher allMatcher = BROADCAST_ALL_PREFIX.matcher(normCmd);
        String strippedCmd = commandPart;
        if (allMatcher.find()) {
            normCmd = normCmd.substring(allMatcher.end()).strip();
            strippedCmd = normCmd;
        }

        int count = allNovas.size();
        for (NovaEntity nova : allNovas) {
            nova.getLookControl().setLookAt(player, 30.0f, 30.0f);
        }

        // Special broadcast commands: rally / construction control
        if (normCmd.equals("tap hop") || normCmd.equals("tap trung") || normCmd.equals("den day")) {
            for (NovaEntity nova : allNovas) {
                nova.setNovaMoveMode(NovaEntity.MODE_FOLLOW);
                nova.setNovaSitRequested(false);
                nova.startRally(player.blockPosition(), player.blockPosition());
            }
            allNovas.get(0).sounds().dialogue(NovaDialogueBrain.Intent.FOLLOW, false);
            player.displayClientMessage(
                    Component.literal("[NOVA ×" + count + "] Toàn bộ NOVA đang đồng loạt tập hợp về vị trí của bạn!")
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
            return;
        }

        if (normCmd.equals("dung xay") || normCmd.equals("tam dung xay")) {
            int affected = 0;
            for (NovaEntity nova : allNovas) {
                if (nova.construction().unfinished()) {
                    nova.construction().pause();
                    affected++;
                }
            }
            player.displayClientMessage(
                    Component.literal("[NOVA ×" + count + "] Đã tạm dừng xây dựng" + (affected > 0 ? " (" + affected + " NOVA)." : "."))
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
            return;
        }

        if (normCmd.equals("tiep tuc xay")) {
            int affected = 0;
            for (NovaEntity nova : allNovas) {
                if (nova.construction().unfinished()) {
                    nova.construction().resume();
                    affected++;
                }
            }
            player.displayClientMessage(
                    Component.literal("[NOVA ×" + count + "] Đã tiếp tục xây dựng" + (affected > 0 ? " (" + affected + " NOVA)." : "."))
                            .withStyle(ChatFormatting.GREEN),
                    true
            );
            return;
        }

        if (normCmd.equals("huy xay")) {
            int affected = 0;
            for (NovaEntity nova : allNovas) {
                if (nova.construction().unfinished()) {
                    nova.construction().cancel();
                    affected++;
                }
            }
            player.displayClientMessage(
                    Component.literal("[NOVA ×" + count + "] Đã hủy công trình xây dựng" + (affected > 0 ? " (" + affected + " NOVA)." : "."))
                            .withStyle(ChatFormatting.GOLD),
                    true
            );
            return;
        }

        long now = player.level().getServer().overworld().getGameTime();
        var conv = new NovaDialogueBrain.Conversation();
        String language = NovaDialogueLoader.select("auto", player.clientInformation().language());
        conv.language(language);

        String cleanInput = NovaDialogueBrain.clean(strippedCmd);
        var match = NovaDialogueBrain.match(cleanInput, conv, now);
        var intent = match.intent();

        if (intent == NovaDialogueBrain.Intent.HELLO) {
            intent = NovaDialogueBrain.Intent.WAVE;
        }

        if (intent.command()) {
            int successCount = 0;
            String firstBlocker = "";
            for (NovaEntity nova : allNovas) {
                var result = NovaDialogueActions.execute(nova, intent, language);
                if (result.responseOverride().isEmpty()) {
                    successCount++;
                } else if (firstBlocker.isEmpty() && !result.detail().isEmpty()) {
                    firstBlocker = result.detail();
                }
            }
            allNovas.get(0).sounds().dialogue(intent, false);

            String summary = switch (intent) {
                case FOLLOW -> "Đồng loạt chuyển sang chế độ: Đi theo chủ nhân!";
                case FREE -> "Đồng loạt chuyển sang chế độ: Tự do!";
                case WAIT -> "Đồng loạt chuyển sang chế độ: Đứng chờ tại chỗ!";
                case SIT -> "Toàn bộ NOVA đã đồng loạt ngồi xuống!";
                case STAND -> "Toàn bộ NOVA đã đồng loạt đứng dậy!";
                case WORK_ON -> firstBlocker.isEmpty() || successCount > 0
                        ? "Đồng loạt BẬT chế độ Giao việc (" + successCount + "/" + count + " sẵn sàng)!"
                        : "Đã bật Giao việc nhưng cần kiểm tra: " + firstBlocker;
                case WORK_OFF -> "Đồng loạt TẮT chế độ Giao việc!";
                case WAVE -> "Toàn bộ NOVA đồng loạt vẫy tay chào bạn!";
                case CELEBRATE -> "Toàn bộ NOVA đồng loạt nhảy ăn mừng!";
                case APOLOGIZE -> "Toàn bộ NOVA đồng loạt cúi đầu xin lỗi!";
                case NOD -> "Toàn bộ NOVA đồng loạt gật đầu xác nhận!";
                case PROUD -> "Toàn bộ NOVA đồng loạt ưỡn ngực tự hào!";
                default -> "Đã đồng loạt thực hiện mệnh lệnh!";
            };

            player.displayClientMessage(
                    Component.literal("[NOVA ×" + count + "] " + summary)
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        } else if (intent != NovaDialogueBrain.Intent.UNKNOWN) {
            for (NovaEntity nova : allNovas) {
                NovaDialogueFacts.react(nova, intent);
            }
            allNovas.get(0).sounds().dialogue(intent, false);
            String answer = NovaDialogueBrain.answer(match, NovaDialogueFacts.read(allNovas.get(0), language), conv, now);
            player.displayClientMessage(
                    Component.literal("[NOVA ×" + count + "] " + answer)
                            .withStyle(ChatFormatting.AQUA),
                    true
            );
        } else {
            player.displayClientMessage(
                    Component.literal("[NOVA] Chưa rõ lệnh \"" + commandPart + "\". (Thử: ngồi xuống, đứng dậy, đi theo, tự do, đứng chờ, làm việc, nghỉ làm, chào, ăn mừng)")
                            .withStyle(ChatFormatting.YELLOW),
                    true
            );
        }
    }

    private static void onChatMessage(PlayerChatMessage message, ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) return;

        String rawContent = message.signedContent();
        if (rawContent == null || rawContent.isBlank()) return;

        // Handle /NOVA:[CÂU RA LỆNH] or NOVA:[CÂU RA LỆNH] broadcast without replying in chat
        if (tryHandleBroadcastChat(player, rawContent)) {
            return;
        }

        long now = player.level().getServer().overworld().getGameTime();

        // Find alive, tamed, owned, active Novas within 32 blocks
        List<NovaEntity> nearbyNovas = player.level().getEntitiesOfClass(
                NovaEntity.class,
                player.getBoundingBox().inflate(32.0),
                n -> n.isAlive() && n.isTame() && n.isOwnedBy(player) && !n.isDormant() && !n.isNoAi()
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
