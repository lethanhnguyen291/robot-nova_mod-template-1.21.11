package net.teogemini.nova.client.screen;

import com.google.gson.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import net.fabricmc.loader.api.FabricLoader;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.ai.dialogue.NovaDialogueLanguage;

/** One local preference, shared across chat windows; it never changes Minecraft's UI language. */
public final class NovaChatLanguageSettings {
    private static final Path FILE=FabricLoader.getInstance().getConfigDir().resolve("robot-nova_mod/chat-language.json");
    private static String cached;
    private NovaChatLanguageSettings() {}
    public static boolean valid(String code) { return code.equals("auto") || NovaDialogueLanguage.CODE.matcher(code).matches(); }
    public static String get() {
        if(cached!=null) return cached;
        cached="auto";
        try {
            if(Files.isRegularFile(FILE) && Files.size(FILE)<4096) {
                String code=JsonParser.parseString(Files.readString(FILE,StandardCharsets.UTF_8)).getAsJsonObject().get("language").getAsString();
                if(valid(code)) cached=code;
            }
        } catch(Exception ex) { ROBOTNOVA_MOD.LOGGER.warn("NOVA chat language preference could not be read"); }
        return cached;
    }
    public static void save(String language) {
        if(!valid(language) || get().equals(language)) return;
        cached=language;
        try {
            Files.createDirectories(FILE.getParent());
            var data=new JsonObject();data.addProperty("version",1);data.addProperty("language",language);
            Files.writeString(FILE,new GsonBuilder().setPrettyPrinting().create().toJson(data)+"\n",StandardCharsets.UTF_8);
        } catch(Exception ex) { ROBOTNOVA_MOD.LOGGER.warn("NOVA chat language preference could not be saved"); }
    }
}
