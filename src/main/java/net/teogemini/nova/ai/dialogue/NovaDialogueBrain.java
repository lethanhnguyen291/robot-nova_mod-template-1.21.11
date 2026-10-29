package net.teogemini.nova.ai.dialogue;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Small, deterministic dialogue rules with Vietnamese slang normalization and multi-turn context. */
public final class NovaDialogueBrain {
    public static final int MAX_INPUT = 240;
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern SEPARATORS = Pattern.compile("[^a-z0-9]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private static final Map<String, String> TOKEN_ALIASES = Map.ofEntries(
            Map.entry("ko", "khong"),
            Map.entry("k", "khong"),
            Map.entry("hong", "khong"),
            Map.entry("hok", "khong"),
            Map.entry("hem", "khong"),
            Map.entry("khum", "khong"),
            Map.entry("dc", "duoc"),
            Map.entry("bik", "biet"),
            Map.entry("bit", "biet"),
            Map.entry("j", "gi"),
            Map.entry("bn", "bao nhieu"),
            Map.entry("bnh", "bao nhieu"),
            Map.entry("nhiu", "bao nhieu"),
            Map.entry("bnhiu", "bao nhieu"),
            Map.entry("vs", "voi"),
            Map.entry("nc", "noi chuyen"),
            Map.entry("nch", "noi chuyen"),
            Map.entry("nv", "nhiem vu"),
            Map.entry("vk", "vu khi"),
            Map.entry("dk", "dieu khien"),
            Map.entry("hp", "mau"),
            Map.entry("lv", "cap"),
            Map.entry("lvl", "cap"),
            Map.entry("exp", "kinh nghiem"),
            Map.entry("xp", "kinh nghiem"),
            Map.entry("tks", "cam on"),
            Map.entry("thx", "cam on"),
            Map.entry("ty", "cam on"),
            Map.entry("sr", "xin loi"),
            Map.entry("sry", "xin loi"),
            Map.entry("mk", "minh"),
            Map.entry("mik", "minh")
    );

    private static final List<String> COMMAND_PREFIXES = List.of(
            "nova oi ", "nova a ", "nova ", "robot oi ", "robot ",
            "chu nhan bao ", "e ", "nay ", "oi ", "hay ", "ban oi ", "ban ",
            "em oi ", "em ", "con ", "may ", "lam on ", "vui long ",
            "cho minh ", "cho tui ", "cho toi ", "giup minh ", "giup tui ", "giup toi "
    );

    private static final List<String> COMMAND_SUFFIXES = List.of(
            " giup minh", " giup tui", " giup toi", " dum minh", " dum tui", " dum toi",
            " gium minh", " gium tui", " gium toi", " voi minh", " voi tui", " voi toi",
            " cung minh", " cung tui", " cung toi", " cho minh", " cho tui", " cho toi",
            " mot chut", " mot ti", " mot xiu", " luon", " ngay", " nhe", " nha",
            " nhen", " nghen", " nao", " cai", " coi", " thu", " di", " nova", " ban", " em", " a"
    );

    public enum Intent {
        HELLO, GOODBYE, STATUS, WHY, BATTERY, HEALTH, INVENTORY, EXPERIENCE,
        EQUIPMENT, CHEST, ENVIRONMENT,
        PERSONALITY, MEMORY, NEED, THANKS, PRAISE, APOLOGY, COMFORT, HELP, UNKNOWN, SMALLTALK,
        FOLLOW, FREE, WAIT, SIT, STAND, WORK_ON, WORK_OFF, WAVE, CELEBRATE, APOLOGIZE, NOD, PROUD;

        public boolean command() { return ordinal() >= FOLLOW.ordinal(); }
    }

    /** Facts supplied by the server, never inferred from the player's wording. */
    public record Facts(String personality, int bond, int battery, String health,
                        String status, String blocker, String need, int occupiedSlots,
                        String experience, String totals, List<String> memories,
                        String profession, String tool, String chest, String environment,
                        String owner, boolean swordNova, boolean working, boolean injured) {
        public Facts { memories = List.copyOf(memories); }

        public Facts(String personality, int bond, int battery, String health,
                     String status, String blocker, String need, int occupiedSlots,
                     String experience, String totals, List<String> memories) {
            this(personality, bond, battery, health, status, blocker, need, occupiedSlots,
                    experience, totals, memories, "Trợ lý NOVA", "Tay không", "Chưa liên kết rương",
                    "Bình thường", "bạn", false, false, false);
        }
    }

    /** Short context belongs to one player and one robot, and expires after 60 seconds. */
    public static final class Conversation {
        private String language="vi_vn";
        public String language() { return language; }
        public void language(String value) {
            if (!language.equals(value)) { language=value; topic=null; choices.clear(); }
        }
        private Intent topic;
        private long topicUntil;
        private final Map<String, Integer> choices = new LinkedHashMap<>();
        public Intent topic(long now) { return now < topicUntil ? topic : null; }
        public void remember(Intent intent, long now) {
            if (intent != Intent.UNKNOWN && intent != Intent.HELP) {
                topic = intent;
                topicUntil = now + 1200;
            }
        }
        String pick(String id, List<String> options) {
            int choice = choices.getOrDefault(id, 0);
            String answer = options.get(Math.floorMod(choice, options.size()));
            if (!choices.containsKey(id) && choices.size() >= 128) choices.remove(choices.keySet().iterator().next());
            choices.put(id, (choice + 1) & 0xffff);
            return answer;
        }
    }

    private NovaDialogueBrain() {}

    public static String clean(String text) {
        if (text == null) return "";
        StringBuilder out = new StringBuilder();
        text.codePoints().filter(c -> !Character.isISOControl(c)
                && Character.getType(c) != Character.FORMAT && c != 0x00a7)
                .limit(MAX_INPUT).forEach(out::appendCodePoint);
        String result = out.toString().strip();
        if (result.length() > MAX_INPUT) {
            int end = Character.isHighSurrogate(result.charAt(MAX_INPUT - 1)) ? MAX_INPUT - 1 : MAX_INPUT;
            result = result.substring(0, end);
        }
        return result;
    }

    public static String normalize(String text) {
        String plain = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("")
                .toLowerCase(Locale.ROOT).replace('đ', 'd');
        String stripped = SPACES.matcher(SEPARATORS.matcher(plain).replaceAll(" ").strip()).replaceAll(" ");
        if (stripped.isEmpty()) return "";
        String[] tokens = stripped.split(" ");
        StringBuilder out = new StringBuilder(stripped.length() + 8);
        for (String token : tokens) {
            String mapped = TOKEN_ALIASES.getOrDefault(token, token);
            if (!out.isEmpty()) out.append(' ');
            out.append(mapped);
        }
        return out.toString();
    }

    /** Only harmless address/politeness words are trimmed; negation is preserved. */
    static String commandForm(String text) {
        String command = text;
        for (int pass = 0; pass < 4; pass++) {
            String before = command;
            for (String prefix : COMMAND_PREFIXES)
                if (command.startsWith(prefix)) command = command.substring(prefix.length()).stripLeading();
            if (before.equals(command)) break;
        }
        for (int pass = 0; pass < 4; pass++) {
            String before = command;
            for (String suffix : COMMAND_SUFFIXES)
                if (command.endsWith(suffix)) command = command.substring(0, command.length() - suffix.length()).stripTrailing();
            if (before.equals(command)) break;
        }
        return command;
    }

    public static NovaDialogueCatalog.Match match(String input, Conversation context, long now) {
        var catalog = NovaDialogueLoader.current(context.language());
        String cleaned = clean(input), text = catalog.language().normalize(cleaned);
        if (text.startsWith("nova oi ")) text = text.substring(8);
        else if (text.startsWith("nova a ")) text = text.substring(7);
        else if (text.startsWith("nova ")) text = text.substring(5);
        if (text.isBlank()) return catalog.defaultMatch(Intent.UNKNOWN);
        Intent topic = context.topic(now);
        String followup = catalog.followup(text);
        if (!followup.isEmpty()) {
            Intent result = Intent.UNKNOWN;
            if (topic != null) result = switch (followup) {
                case "why" -> topic == Intent.BATTERY ? Intent.BATTERY
                        : topic == Intent.HEALTH ? Intent.HEALTH
                        : topic == Intent.EQUIPMENT ? Intent.EQUIPMENT
                        : topic == Intent.CHEST ? Intent.CHEST
                        : Intent.WHY;
                case "need" -> Intent.NEED;
                case "memory" -> topic == Intent.MEMORY ? Intent.MEMORY : Intent.STATUS;
                case "more" -> topic == Intent.STATUS ? Intent.EQUIPMENT
                        : topic == Intent.EQUIPMENT ? Intent.CHEST
                        : topic == Intent.BATTERY ? Intent.HEALTH
                        : topic == Intent.MEMORY ? Intent.MEMORY
                        : Intent.STATUS;
                default -> Intent.UNKNOWN;
            };
            else if (followup.equals("need")) result = Intent.NEED;
            else if (followup.equals("why")) result = Intent.WHY;
            else if (followup.equals("more")) result = Intent.STATUS;
            return catalog.defaultMatch(result);
        }
        boolean quoted = cleaned.chars().anyMatch(c -> c == '"' || c == 96 || c == '“' || c == '”' || c == '‘' || c == '’');
        for (int i=0;i<cleaned.length();i++) if (cleaned.charAt(i)==39
                && !(i>0 && i+1<cleaned.length() && Character.isLetter(cleaned.charAt(i-1)) && Character.isLetter(cleaned.charAt(i+1)))) quoted=true;
        return catalog.find(text, !quoted && !cleaned.contains("?"));
    }

    public static Intent understand(String input, Conversation context, long now) {
        return match(input, context, now).intent();
    }

    public static String answer(Intent intent, Facts facts, Conversation context, long now) {
        return answer(NovaDialogueLoader.current(context.language()).defaultMatch(intent), facts, context, now);
    }

    public static String answer(NovaDialogueCatalog.Match match, Facts facts, Conversation context, long now) {
        if (match.intent().command()) throw new IllegalArgumentException("Commands require a server action result");
        context.remember(match.intent(), now);
        return match.catalog().render(match.answerId(), facts, context, "");
    }

    public static String actionAnswer(NovaDialogueCatalog.Match match, Facts facts, Conversation context,
                                      long now, NovaDialogueActions.Result result) {
        context.remember(match.intent(), now);
        String id = result.responseOverride().isEmpty() ? match.answerId() : result.responseOverride();
        return match.catalog().render(id, facts, context, result.detail());
    }
}
