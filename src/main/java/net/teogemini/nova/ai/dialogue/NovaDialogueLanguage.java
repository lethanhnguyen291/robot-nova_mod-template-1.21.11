package net.teogemini.nova.ai.dialogue;

import com.google.gson.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;

/** Immutable language rules and translated messages; no translation service is used at runtime. */
public final class NovaDialogueLanguage {
    private static final Pattern SEPARATORS=Pattern.compile("[^\\p{L}\\p{N}\\p{M}]+");
    private static final Pattern MARKS=Pattern.compile("\\p{M}+");
    public static final Pattern CODE=Pattern.compile("[a-z]{2,3}_[a-z0-9]{2,8}");
    public final String code, name;
    private final List<String> prefixes, suffixes;
    private final Map<String,String> aliases, messages;
    public final Set<String> negations, stopWords;

    private NovaDialogueLanguage(String code,String name,List<String> prefixes,List<String> suffixes,
                                Map<String,String> aliases,Map<String,String> messages,
                                Set<String> negations,Set<String> stopWords) {
        this.code=code; this.name=name; this.prefixes=prefixes; this.suffixes=suffixes;
        this.aliases=aliases; this.messages=messages; this.negations=negations; this.stopWords=stopWords;
    }
    public String text(String key) { return messages.get(key); }
    public Map<String,String> ui() {
        var result=new LinkedHashMap<String,String>();
        messages.forEach((k,v)-> { if(k.startsWith("ui.")) result.put(k,v); });
        return Map.copyOf(result);
    }
    public boolean unspaced() { return code.startsWith("ja_") || code.startsWith("zh_") || code.startsWith("ko_"); }

    public String normalize(String text) {
        if(code.equals("vi_vn")) return NovaDialogueBrain.normalize(text);
        String plain=Normalizer.normalize(text,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        if(Set.of("en","fr","de","es","pt").contains(code.substring(0,2)))
            plain=MARKS.matcher(Normalizer.normalize(plain,Normalizer.Form.NFD)).replaceAll("");
        if(code.startsWith("de_")) plain=plain.replace("ß","ss");
        if(code.startsWith("ru_")) plain=plain.replace('ё','е');
        plain=SEPARATORS.matcher(plain).replaceAll(" ").strip().replaceAll("\\s+"," ");
        if(aliases.isEmpty() || plain.isEmpty()) return plain;
        var result=new StringJoiner(" ");
        for(String word:plain.split(" ")) result.add(aliases.getOrDefault(word,word));
        return result.toString();
    }

    public String commandForm(String text) {
        if(code.equals("vi_vn")) return NovaDialogueBrain.commandForm(text);
        String value=text;
        for(int pass=0;pass<4;pass++) {
            String before=value;
            for(String raw:prefixes) {
                String prefix=normalize(raw);
                if(value.startsWith(prefix+" ")) value=value.substring(prefix.length()).stripLeading();
                else if(unspaced() && value.startsWith(prefix) && value.length()>prefix.length()) value=value.substring(prefix.length()).stripLeading();
            }
            for(String raw:suffixes) {
                String suffix=normalize(raw);
                if(value.endsWith(" "+suffix)) value=value.substring(0,value.length()-suffix.length()).stripTrailing();
                else if(unspaced() && value.endsWith(suffix) && value.length()>suffix.length()) value=value.substring(0,value.length()-suffix.length()).stripTrailing();
            }
            if(before.equals(value)) break;
        }
        return value;
    }
    public boolean containsPhrase(String input,String phrase) {
        return unspaced() ? input.contains(phrase) : (" "+input+" ").contains(" "+phrase+" ");
    }

    public static NovaDialogueLanguage parse(String code,String json) {
        if(!CODE.matcher(code).matches()) throw new IllegalArgumentException("Invalid language code: "+code);
        var obj=JsonParser.parseString(json).getAsJsonObject();
        String name=obj.get("name").getAsString();
        if(name.isBlank() || name.length()>48) throw new IllegalArgumentException("Invalid language name: "+code);
        Map<String,String> messages=map(obj.getAsJsonObject("messages"));
        for(String key:List.of("ui.title","ui.input","ui.hint","ui.send","ui.back","ui.waiting",
                "ui.help","ui.timeout","ui.you","ui.language","ui.auto","ui.invalid","ui.open_hint",
                "ui.battery","ui.status","ui.equipment","ui.memory","ui.guide",
                "ui.query_battery","ui.query_status","ui.query_equipment","ui.query_memory","ui.query_guide")) {
            String value=messages.get(key);
            if(value==null || value.isBlank() || value.length()>240) throw new IllegalArgumentException(code+": missing/oversized "+key);
        }
        return new NovaDialogueLanguage(code,name,list(obj,"prefixes"),list(obj,"suffixes"),
                map(obj.getAsJsonObject("aliases")),messages,Set.copyOf(list(obj,"negations")),Set.copyOf(list(obj,"stop_words")));
    }
    private static List<String> list(JsonObject obj,String key) {
        if(!obj.has(key)) return List.of();
        var result=new ArrayList<String>();
        for(JsonElement e:obj.getAsJsonArray(key)) {
            String value=e.getAsString();
            if(value.isBlank() || value.length()>100) throw new IllegalArgumentException("Invalid language rule: "+key);
            result.add(value);
        }
        if(result.size()>256) throw new IllegalArgumentException("Too many language rules: "+key);
        return List.copyOf(result);
    }
    private static Map<String,String> map(JsonObject object) {
        var result=new LinkedHashMap<String,String>();
        if(object!=null) for(var e:object.entrySet()) {
            String value=e.getValue().getAsString();
            if(value.length()>2000 || value.codePoints().anyMatch(c->Character.isISOControl(c) || Character.getType(c)==Character.FORMAT))
                throw new IllegalArgumentException("Invalid translated message: "+e.getKey());
            result.put(e.getKey(),value);
        }
        if(result.size()>2000) throw new IllegalArgumentException("Too many translated messages");
        return Map.copyOf(result);
    }
}
