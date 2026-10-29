package net.teogemini.nova.ai.dialogue;

import com.google.gson.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.teogemini.nova.ROBOTNOVA_MOD;

/** Atomic multilingual snapshots, loaded only at startup or /reload. */
public final class NovaDialogueLoader {
    public static final int MAX_LANGUAGES=50;
    private static final String PREFIX="/data/robot-nova_mod/dialogue/";
    private static final List<String> FILES=List.of("00_system.json","10_movement.json","20_work.json",
            "30_status.json","40_social.json","50_smalltalk.json");
    private static final int FILE_LIMIT=262144, PACK_LIMIT=2097152, TOTAL_LIMIT=16777216;
    public record Pack(NovaDialogueLanguage language,NovaDialogueCatalog catalog) {}
    private record Snapshot(String fallback,Map<String,Pack> packs) {}
    private static volatile Snapshot current=loadBundled();
    private NovaDialogueLoader() {}

    private static String resource(String name) {
        try(var input=NovaDialogueLoader.class.getResourceAsStream(PREFIX+name)) {
            if(input==null) throw new IllegalStateException("Missing NOVA language resource: "+name);
            return new String(input.readAllBytes(),StandardCharsets.UTF_8);
        } catch(IOException ex) { throw new IllegalStateException(name,ex); }
    }
    private static List<String> codes(String manifest) {
        var json=JsonParser.parseString(manifest).getAsJsonObject();
        if(json.get("version").getAsInt()!=1) throw new IllegalArgumentException("Unsupported language manifest version");
        var codes=new LinkedHashSet<String>();
        for(JsonElement e:json.getAsJsonArray("languages")) {
            String code=e.getAsString();
            if(!NovaDialogueLanguage.CODE.matcher(code).matches() || !codes.add(code))
                throw new IllegalArgumentException("Invalid/duplicate language code: "+code);
        }
        if(codes.size()>MAX_LANGUAGES || !codes.contains("vi_vn") || !codes.contains("en_us"))
            throw new IllegalArgumentException("Language packs require vi_vn and en_us; maximum "+MAX_LANGUAGES);
        return List.copyOf(codes);
    }
    private static String fallback(String manifest,Map<String,Pack> packs) {
        String value=JsonParser.parseString(manifest).getAsJsonObject().get("fallback").getAsString();
        if(!packs.containsKey(value)) throw new IllegalArgumentException("Fallback language is missing: "+value);
        return value;
    }
    private static Snapshot loadBundled() {
        String manifest=resource("languages.json");
        var packs=new LinkedHashMap<String,Pack>();
        for(String code:codes(manifest)) {
            var rules=NovaDialogueLanguage.parse(code,resource(code+"/locale.json"));
            packs.put(code,new Pack(rules,NovaDialogueCatalog.parse(bundledFiles(code),rules)));
        }
        return new Snapshot(fallback(manifest,packs),Collections.unmodifiableMap(packs));
    }
    public static Map<String,String> bundledFiles() { return bundledFiles("vi_vn"); }
    public static Map<String,String> bundledFiles(String language) {
        var files=new LinkedHashMap<String,String>();
        for(String file:FILES) files.put(file,resource(language+"/"+file));
        return files;
    }
    public static List<String> languages() { return List.copyOf(current.packs().keySet()); }
    public static boolean supported(String code) { return current.packs().containsKey(code); }
    public static String resolve(String requested) {
        String code=requested==null ? "" : requested.toLowerCase(Locale.ROOT).replace('-','_');
        if(supported(code)) return code;
        int split=code.indexOf('_');
        if(split>0) {
            String prefix=code.substring(0,split)+"_";
            for(String candidate:current.packs().keySet()) if(candidate.startsWith(prefix)) return candidate;
        }
        return current.fallback();
    }
    public static String select(String preference,String clientLanguage) {
        return preference==null || preference.equals("auto") ? resolve(clientLanguage)
                : supported(preference) ? preference : current.fallback();
    }
    public static NovaDialogueLanguage language(String code) { return current.packs().get(resolve(code)).language(); }
    public static NovaDialogueCatalog current() { return current("vi_vn"); }
    public static NovaDialogueCatalog current(String code) { return current.packs().get(resolve(code)).catalog(); }
    public static String message(String language,String key) {
        Snapshot snapshot=current;
        Pack pack=snapshot.packs().getOrDefault(language,snapshot.packs().get(snapshot.fallback()));
        String value=pack.language().text(key);
        if(value==null) value=snapshot.packs().get("en_us").language().text(key);
        if(value==null) value=snapshot.packs().get("vi_vn").language().text(key);
        return value==null ? key : value;
    }
    private static String readText(Path file) throws IOException {
        byte[] bytes;
        try(var input=Files.newInputStream(file)) { bytes=input.readNBytes(FILE_LIMIT+1); }
        if(bytes.length>FILE_LIMIT) throw new IOException("Language file too large: "+file);
        String text=new String(bytes,StandardCharsets.UTF_8);
        return text.startsWith("\uFEFF") ? text.substring(1) : text;
    }
    private static Pack readPack(Path directory,String code) throws IOException {
        var rules=NovaDialogueLanguage.parse(code,readText(directory.resolve("locale.json")));
        var files=new LinkedHashMap<String,String>();
        int total=0;
        try(var stream=Files.list(directory)) {
            var paths=stream.filter(p->Files.isRegularFile(p) && p.toString().endsWith(".json")
                    && !p.getFileName().toString().equals("locale.json")).sorted().limit(33).toList();
            if(paths.size()>32) throw new IOException("Too many dialogue files: "+code);
            for(Path path:paths) {
                String text=readText(path);
                total+=text.getBytes(StandardCharsets.UTF_8).length;
                if(total>PACK_LIMIT) throw new IOException("Language pack too large: "+code);
                files.put(path.getFileName().toString(),text);
            }
        }
        return new Pack(rules,NovaDialogueCatalog.parse(files,rules));
    }
    public static NovaDialogueCatalog read(Path directory) throws IOException {
        return readPack(directory,"vi_vn").catalog();
    }
    public static synchronized boolean reload(Path directory) {
        try {
            String manifest=readText(directory.resolve("languages.json"));
            var packs=new LinkedHashMap<String,Pack>();
            long total=0;
            for(String code:codes(manifest)) {
                Path folder=directory.resolve(code);
                try(var files=Files.list(folder)) {
                    for(Path p:files.filter(Files::isRegularFile).toList()) {
                        total+=Files.size(p);
                        if(total>TOTAL_LIMIT) throw new IOException("All language packs exceed 16 MiB");
                    }
                }
                packs.put(code,readPack(folder,code));
            }
            Snapshot replacement=new Snapshot(fallback(manifest,packs),Collections.unmodifiableMap(packs));
            current=replacement;
            ROBOTNOVA_MOD.LOGGER.info("NOVA dialogue: loaded {} languages from {}",packs.size(),directory);
            return true;
        } catch(IOException | RuntimeException ex) {
            ROBOTNOVA_MOD.LOGGER.error("NOVA languages unchanged; fix JSON then /reload: {}",ex.getMessage());
            return false;
        }
    }
    private static void writeMissing(Path file,String text) throws IOException {
        try { Files.writeString(file,text,StandardCharsets.UTF_8,StandardOpenOption.CREATE_NEW); }
        catch(FileAlreadyExistsException ignored) { /* Preserve custom dialogue and translations. */ }
    }
    public static void installDefaults(Path directory) throws IOException {
        Files.createDirectories(directory);
        writeMissing(directory.resolve("languages.json"),resource("languages.json"));
        Path vietnamese=directory.resolve("vi_vn");
        Files.createDirectories(vietnamese);
        // The previous flat configuration is copied once, before installing default Vietnamese files.
        try(var stream=Files.list(directory)) {
            for(Path old:stream.filter(p->Files.isRegularFile(p) && p.toString().endsWith(".json")
                    && !p.getFileName().toString().equals("languages.json")).toList())
                writeMissing(vietnamese.resolve(old.getFileName()),readText(old));
        }
        for(String code:codes(resource("languages.json"))) {
            Path folder=directory.resolve(code); Files.createDirectories(folder);
            writeMissing(folder.resolve("locale.json"),resource(code+"/locale.json"));
            for(String file:FILES) writeMissing(folder.resolve(file),resource(code+"/"+file));
        }
    }
    public static void register() {
        Path directory=FabricLoader.getInstance().getConfigDir().resolve("robot-nova_mod/dialogue");
        ServerLifecycleEvents.SERVER_STARTING.register(server-> {
            try { installDefaults(directory); reload(directory); }
            catch(IOException ex) { ROBOTNOVA_MOD.LOGGER.error("NOVA language config unavailable",ex); }
        });
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server,resources,success)-> {
            if(success) server.execute(()->reload(directory));
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server->current=loadBundled());
    }
}
