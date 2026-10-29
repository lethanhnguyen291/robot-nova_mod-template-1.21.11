package net.teogemini.nova.ai.dialogue;

import com.google.gson.*;
import java.util.*;
import java.util.regex.Pattern;
import net.teogemini.nova.ai.dialogue.NovaDialogueBrain.*;

/** Immutable, validated dialogue data; prepared once, never read from disk per message. */
public final class NovaDialogueCatalog {
    private static final Pattern ID = Pattern.compile("[a-zA-Z0-9_.-]{1,64}");
    private static final Pattern VARIABLE = Pattern.compile("\\{([a-z_]+)}");
    private static final Set<String> VARIABLES = Set.of(
            "personality", "bond", "battery", "health",
            "status", "blocker", "need", "occupied_slots",
            "experience", "totals", "memory", "detail",
            "profession", "tool", "chest", "environment", "owner"
    );
    private static final Set<String> CONDITIONS = Set.of(
            "battery_low", "has_blocker", "no_memories",
            "sword_nova", "working", "injured", "high_bond"
    );
    private static final Set<String> NEGATIONS = Set.of("khong", "dung", "chua", "ngung", "thoi", "tat", "bo");
    private static final Set<String> STOP_WORDS = Set.of(
            "la", "va", "cua", "cho", "voi", "minh", "tui", "toi", "ban", "nova",
            "oi", "nhe", "nha", "di", "vậy", "vay", "thi", "ma", "o", "tai", "trong"
    );

    public record Entry(String id, Intent intent, String match, int priority, List<String> questions, String answer) {}
    public record Variant(String when, List<String> lines) {}
    public record Answer(List<String> lines, List<Variant> variants) {}
    public record Match(NovaDialogueCatalog catalog, Entry entry, boolean uncertain) {
        public Intent intent() { return entry.intent(); }
        public String questionId() { return entry.id(); }
        public String answerId() { return uncertain ? "SYS_AMBIGUOUS" : entry.answer(); }
    }
    private final NovaDialogueLanguage language;
    public NovaDialogueLanguage language() { return language; }
    private final List<Entry> entries;
    private final Map<String, Answer> answers;
    private final Map<Intent, Entry> defaults;
    private final Map<String, Entry> commands;
    private final Map<String, Entry> exactQuestions;
    private final Map<String, String> followups;

    private NovaDialogueCatalog(List<Entry> entries, Map<String, Answer> answers, Map<String, String> followups, NovaDialogueLanguage language) {
        this.language=language;
        this.entries = List.copyOf(entries);
        this.answers = Map.copyOf(answers);
        var defaults = new EnumMap<Intent, Entry>(Intent.class);
        var commands = new HashMap<String, Entry>();
        var exact = new HashMap<String, Entry>();
        for (Entry e : entries) {
            defaults.putIfAbsent(e.intent(), e);
            for (String q : e.questions()) {
                Map<String, Entry> index = e.intent().command() ? commands : exact;
                String key = e.intent().command() ? language.commandForm(q) : q;
                Entry previous = index.putIfAbsent(key, e);
                if (previous != null && !previous.id().equals(e.id()))
                    throw new IllegalArgumentException("Cau trung y het: '" + q + "' trong " + previous.id() + " va " + e.id());
            }
        }
        for (Intent intent : Intent.values())
            require(defaults.containsKey(intent), "Thieu nhom intent " + intent);
        for (String id : List.of("SYS_AMBIGUOUS", "SYS_NO_AI", "SYS_PANIC", "SYS_ACTION_BLOCKED",
                "SYS_NOT_STARTED", "SYS_WORK_PENDING")) require(answers.containsKey(id), "Thieu cau he thong " + id);
        this.defaults = Map.copyOf(defaults);
        this.commands = Map.copyOf(commands);
        this.exactQuestions = Map.copyOf(exact);
        this.followups = Map.copyOf(followups);
    }

    public int size() { return entries.size(); }
    public List<Entry> entries() { return entries; }
    public String followup(String text) { return followups.getOrDefault(text, ""); }
    public Match defaultMatch(Intent intent) { return new Match(this, defaults.get(intent), false); }
    private Match unknown(boolean uncertain) { return new Match(this, defaults.get(Intent.UNKNOWN), uncertain); }

    public Match find(String input, boolean allowCommands) {
        String cmdForm = language.commandForm(input);
        if (allowCommands) {
            Entry command = commands.get(cmdForm);
            if (command != null) return new Match(this, command, false);
        }
        Entry exact = exactQuestions.get(input);
        if (exact != null) return new Match(this, exact, false);
        Entry exactCmdForm = exactQuestions.get(cmdForm);
        if (exactCmdForm != null) return new Match(this, exactCmdForm, false);

        // Preserve existing Vietnamese matching; other languages execute explicit commands only.
        if (allowCommands && language.code.equals("vi_vn")) {
            String[] inputTokens = cmdForm.split(" ");
            if (inputTokens.length <= 7) {
                boolean inputHasNegation = false;
                for (String t : inputTokens) if (NEGATIONS.contains(t)) { inputHasNegation = true; break; }
                String paddedCmd = " " + cmdForm + " ";
                Entry bestCmd = null;
                int bestCmdLen = -1;
                boolean cmdTied = false;
                for (Entry e : entries) {
                    if (!e.intent().command()) continue;
                    for (String q : e.questions()) {
                        String qForm = language.commandForm(q);
                        if (qForm.length() < 4) continue;
                        boolean qHasNegation = false;
                        for (String qt : qForm.split(" ")) if (NEGATIONS.contains(qt)) { qHasNegation = true; break; }
                        if (inputHasNegation != qHasNegation) continue;
                        if (paddedCmd.contains(" " + qForm + " ")) {
                            if (qForm.length() > bestCmdLen) {
                                bestCmd = e;
                                bestCmdLen = qForm.length();
                                cmdTied = false;
                            } else if (qForm.length() == bestCmdLen && bestCmd != null && bestCmd.intent() != e.intent()) {
                                cmdTied = true;
                            }
                        }
                    }
                }
                if (bestCmd != null && !cmdTied) return new Match(this, bestCmd, false);
            }
        }

        // Tier 2: Weighted substring phrase matching
        Entry best = null;
        int score = -1;
        boolean tied = false;
        String padded = " " + input + " ";
        for (Entry e : entries) {
            if (e.intent().command() || !e.match().equals("contains")) continue;
            int maxPhraseLen = -1;
            int matchCount = 0;
            for (String q : e.questions()) {
                if (language.containsPhrase(input,q)) {
                    matchCount++;
                    if (q.length() > maxPhraseLen) maxPhraseLen = q.length();
                }
            }
            if (maxPhraseLen >= 0) {
                int candidate = e.priority() * 1000 + maxPhraseLen * 10 + Math.min(9, matchCount);
                if (candidate > score) {
                    best = e;
                    score = candidate;
                    tied = false;
                } else if (candidate == score && best != null && best.intent() != e.intent()) {
                    tied = true;
                }
            }
        }
        if (best != null) return tied ? unknown(true) : new Match(this, best, false);

        // Tier 3: Smart Keyword/Token Overlap with Per-Word Fuzzy Matching for natural questions
        String[] rawTokens = input.split(" ");
        if (rawTokens.length >= 2 && input.length() <= 200 && !language.unspaced()) {
            Entry bestKeywordEntry = null;
            int bestKeywordScore = 0;
            for (Entry e : entries) {
                if (e.intent().command()) continue;
                for (String q : e.questions()) {
                    String[] qTokens = q.split(" ");
                    if (qTokens.length < 2) continue;
                    int matchedWords = 0;
                    int significantWords = 0;
                    int matchedSignificant = 0;
                    for (String qt : qTokens) {
                        boolean significant = !(language.code.equals("vi_vn") ? STOP_WORDS : language.stopWords).contains(qt) && qt.length() >= 2;
                        if (significant) significantWords++;
                        if (containsWordFuzzy(rawTokens, qt)) {
                            matchedWords++;
                            if (significant) matchedSignificant++;
                        }
                    }
                    if (significantWords == 0) continue;
                    // All significant words in a 2-3 word phrase must match, or >= 75% in longer phrases
                    boolean enough = (qTokens.length <= 3 && matchedWords == qTokens.length)
                            || (significantWords >= 2 && matchedSignificant == significantWords && matchedWords >= qTokens.length - 1);
                    if (enough) {
                        int kwScore = e.priority() * 100 + matchedSignificant * 25 + matchedWords * 10 + q.length();
                        if (kwScore > bestKeywordScore) {
                            bestKeywordScore = kwScore;
                            bestKeywordEntry = e;
                        }
                    }
                }
            }
            if (bestKeywordEntry != null) return new Match(this, bestKeywordEntry, false);
        }

        // Tier 4: Whole-sentence typo tolerance for informational questions
        if (input.length() < 6 || rawTokens.length < 2 || input.length() > 160) return unknown(false);
        int bestDistance = 3;
        int comparisons = 0;
        for (Entry e : entries) {
            if (e.intent().command()) continue;
            for (String q : e.questions()) {
                if (q.length() < 6 || Math.abs(input.length() - q.length()) > 2
                        || q.indexOf(' ') < 0) continue;
                int allowed = q.length() >= 20 ? 2 : 1;
                if (Math.abs(input.length() - q.length()) > allowed) continue;
                if (++comparisons > 160) break;
                int d = distance(input, q, allowed);
                if (d > allowed) continue;
                if (d < bestDistance) { best = e; bestDistance = d; tied = false; }
                else if (d == bestDistance && best != null && best.intent() != e.intent()) tied = true;
            }
        }
        return best == null ? unknown(false) : tied ? unknown(true) : new Match(this, best, false);
    }

    private static boolean containsWordFuzzy(String[] inputTokens, String target) {
        for (String token : inputTokens) {
            if (token.equals(target)) return true;
            if (target.length() >= 4 && token.length() >= 4 && Math.abs(token.length() - target.length()) <= 1) {
                if (distance(token, target, 1) <= 1) return true;
            }
        }
        return false;
    }

    private static int distance(String a, String b, int limit) {
        if (Math.abs(a.length() - b.length()) > limit) return limit + 1;
        int[] previous = new int[b.length() + 1], row = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) previous[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            row[0] = i; int minimum = i;
            for (int j = 1; j <= b.length(); j++) {
                row[j] = Math.min(Math.min(row[j - 1] + 1, previous[j] + 1), previous[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                minimum = Math.min(minimum, row[j]);
            }
            if (minimum > limit) return limit + 1;
            int[] swap = previous; previous = row; row = swap;
        }
        return previous[b.length()];
    }

    public String render(String id, Facts facts, Conversation context, String detail) {
        Answer answer = answers.get(id);
        if (answer == null) answer = answers.get(defaults.get(Intent.UNKNOWN).answer());
        List<String> lines = answer.lines();
        String variantKey = "default";
        for (Variant variant : answer.variants()) if (matches(variant.when(), facts)) {
            lines = variant.lines(); variantKey = variant.when(); break;
        }
        String template = context.pick(id + ":" + variantKey, lines);
        var matcher = VARIABLE.matcher(template);
        var output = new StringBuilder();
        while (matcher.find()) {
            String value = switch (matcher.group(1)) {
                case "personality" -> NovaDialogueText.personality(language.code,facts.personality());
                case "bond" -> String.valueOf(facts.bond());
                case "battery" -> String.valueOf(facts.battery());
                case "health" -> facts.health();
                case "status" -> facts.status();
                case "blocker" -> facts.blocker();
                case "need" -> facts.need();
                case "occupied_slots" -> String.valueOf(facts.occupiedSlots());
                case "experience" -> facts.experience();
                case "totals" -> facts.totals();
                case "memory" -> facts.memories().isEmpty() ? "" : context.pick("memory", facts.memories());
                case "detail" -> detail;
                case "profession" -> facts.profession();
                case "tool" -> facts.tool();
                case "chest" -> facts.chest();
                case "environment" -> facts.environment();
                case "owner" -> facts.owner();
                default -> "";
            };
            matcher.appendReplacement(output, java.util.regex.Matcher.quoteReplacement(value));
        }
        matcher.appendTail(output);
        String text = output.toString();
        if (text.length() > 1000) {
            int end = Character.isHighSurrogate(text.charAt(999)) ? 999 : 1000;
            text = text.substring(0, end);
        }
        return text;
    }

    private static boolean matches(String when, Facts f) {
        if (when.startsWith("personality:")) return f.personality().equals(when.substring(12));
        return switch (when) {
            case "battery_low" -> f.battery() <= 10;
            case "has_blocker" -> !f.blocker().isEmpty();
            case "no_memories" -> f.memories().isEmpty();
            case "sword_nova" -> f.swordNova();
            case "working" -> f.working();
            case "injured" -> f.injured();
            case "high_bond" -> f.bond() >= 50;
            default -> false;
        };
    }

    public static NovaDialogueCatalog parse(Map<String, String> files) {
        return parse(files,NovaDialogueLoader.language("vi_vn"));
    }

    public static NovaDialogueCatalog parse(Map<String, String> files, NovaDialogueLanguage language) {
        var entries = new ArrayList<Entry>();
        var answers = new LinkedHashMap<String, Answer>();
        var ids = new HashSet<String>();
        var followups = new HashMap<String, String>();
        require(files.size() <= 32, "Toi da 32 file JSON");
        for (var file : new TreeMap<>(files).entrySet()) try {
            JsonObject root = JsonParser.parseString(file.getValue()).getAsJsonObject();
            int version = root.get("version").getAsInt();
            require(version == 1 || version == 2, "version phai bang 1 hoac 2");
            require(ID.matcher(root.get("group").getAsString()).matches(), "group khong hop le");
            if (root.has("followups")) for (var item : root.getAsJsonObject("followups").entrySet()) {
                require(Set.of("why", "need", "memory", "more").contains(item.getKey()), "Kieu noi tiep khong hop le");
                for (String phrase : lines(item.getValue().getAsJsonArray(), false)) {
                    String plain = language.normalize(phrase);
                    require(followups.putIfAbsent(plain, item.getKey()) == null, "Cau noi tiep bi trung: " + phrase);
                }
            }
            for (var item : root.getAsJsonObject("answers").entrySet()) {
                require(ID.matcher(item.getKey()).matches(), "Ma cau tra loi khong hop le");
                JsonObject data = item.getValue().getAsJsonObject();
                List<String> lines = lines(data.getAsJsonArray("lines"), true);
                var variants = new ArrayList<Variant>();
                if (data.has("variants")) for (JsonElement v : data.getAsJsonArray("variants")) {
                    var variant = v.getAsJsonObject();
                    String when = variant.get("when").getAsString();
                    require(CONDITIONS.contains(when) || when.startsWith("personality:") && when.length() <= 80,
                            "Dieu kien khong ho tro: " + when);
                    variants.add(new Variant(when, lines(variant.getAsJsonArray("lines"), true)));
                }
                require(answers.putIfAbsent(item.getKey(), new Answer(lines, List.copyOf(variants))) == null,
                        "Trung ma cau tra loi: " + item.getKey());
            }
            for (JsonElement element : root.getAsJsonArray("questions")) {
                var q = element.getAsJsonObject();
                String id = q.get("id").getAsString();
                require(ID.matcher(id).matches() && ids.add(id), "Ma cau hoi sai hoac trung: " + id);
                Intent intent = Intent.valueOf(q.get("intent").getAsString());
                String match = q.get("match").getAsString();
                require(match.equals("exact") || match.equals("contains"), "match chi nhan exact/contains");
                require(!intent.command() || match.equals("exact"), "Lenh hanh dong bat buoc match=exact: " + id);
                int priority = q.has("priority") ? q.get("priority").getAsInt() : 50;
                require(priority >= 0 && priority <= 100, "priority phai tu 0 den 100");
                List<String> raw = lines(q.getAsJsonArray("phrases"), false);
                var normalized = new LinkedHashSet<String>();
                for (String phrase : raw) {
                    String plain = language.normalize(phrase);
                    require(!plain.isBlank() && plain.length() <= 240, "Cau hoi rong/qua dai: " + id);
                    normalized.add(plain);
                }
                entries.add(new Entry(id, intent, match, priority, List.copyOf(normalized), q.get("answer").getAsString()));
            }
            require(entries.size() <= 1000 && answers.size() <= 2000, "Qua nhieu cau hoi/cau tra loi");
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(file.getKey() + ": " + ex.getMessage(), ex);
        }
        for (Entry entry : entries) require(answers.containsKey(entry.answer()),
                "Cau " + entry.id() + " tro toi ma tra loi khong ton tai: " + entry.answer());
        return new NovaDialogueCatalog(entries, answers, followups, language);
    }

    private static List<String> lines(JsonArray array, boolean template) {
        require(array != null && !array.isEmpty() && array.size() <= 64, "Danh sach phai co 1-64 cau");
        var result = new ArrayList<String>();
        for (JsonElement element : array) {
            String text = element.getAsString();
            require(!text.isBlank() && text.length() <= 600, "Cau rong hoac dai hon 600 ky tu");
            require(text.codePoints().noneMatch(c -> Character.isISOControl(c)
                    || Character.getType(c) == Character.FORMAT || c == 0x00a7), "Cau co ky tu dieu khien");
            if (template) {
                var m = VARIABLE.matcher(text);
                while (m.find()) require(VARIABLES.contains(m.group(1)), "Bien khong ho tro: " + m.group());
            }
            result.add(text);
        }
        return List.copyOf(result);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }
}
