package net.teogemini.nova.client.screen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.gui.components.CycleButton;
import java.util.UUID;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.teogemini.nova.ai.dialogue.NovaDialogueBrain;
import net.teogemini.nova.entity.NovaEntity;
import net.teogemini.nova.network.NovaDialogueNetwork;
import net.teogemini.nova.network.NovaRodControl;
import org.lwjgl.glfw.GLFW;

/** Small private chat window; transcripts are bounded and kept only while the window is open. */
public final class NovaChatScreen extends Screen {
    private record Message(boolean owner, String text) {}
    private record Line(FormattedCharSequence text, int color) {}
    private final int entityId;
    private final UUID robot, token;
    private final List<Message> history = new ArrayList<>();
    private final List<Line> lines = new ArrayList<>();
    private final List<Button> quickButtons = new ArrayList<>();
    private EditBox input;
    private Button send;
    private int left, boxWidth, top, bottom, scroll, sequence, shownSequence;
    private int cooldown, waitingTicks;
    private boolean waiting;
    private boolean firstInit=true;
    private String language, preference;
    private List<NovaDialogueNetwork.LanguageOption> languages;
    private Map<String,String> ui;
    private CycleButton<String> languageButton;
    private String text(String key) { return ui.getOrDefault(key,key); }
    private String languageName(String code) {
        if(code.equals("auto")) return text("ui.auto");
        return languages.stream().filter(l->l.code().equals(code)).map(NovaDialogueNetwork.LanguageOption::name).findFirst().orElse(code);
    }

    public NovaChatScreen(NovaDialogueNetwork.Reply page) {
        super(Component.literal(page.ui().getOrDefault("ui.title","NOVA")));
        entityId = page.entityId(); robot = page.robot(); token = page.token();
        language=page.language(); preference=page.preference(); languages=page.languages(); ui=page.ui();
        history.add(new Message(false, page.text()));
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(NovaDialogueNetwork.Reply.TYPE, (page, context) -> {
            var client = context.client();
            if (client.level == null || client.player == null) return;
            if (page.open()) {
                if (client.screen != null && !(client.screen instanceof NovaControlScreen)) return;
                if (!(client.level.getEntity(page.entityId()) instanceof NovaEntity nova)
                        || !nova.getUUID().equals(page.robot()) || !nova.isAlive() || !nova.isOwnedBy(client.player)
                        || !NovaRodControl.holdingRod(client.player) || client.player.distanceToSqr(nova) > 64) return;
                client.setScreen(new NovaChatScreen(page));
            } else if (client.screen instanceof NovaChatScreen chat) {
                chat.receive(page);
            }
        });
    }

    private NovaEntity nova() {
        if (minecraft == null || minecraft.level == null || minecraft.player == null
                || !minecraft.player.isAlive() || minecraft.player.isSpectator()
                || !NovaRodControl.holdingRod(minecraft.player)) return null;
        return minecraft.level.getEntity(entityId) instanceof NovaEntity n
                && n.getUUID().equals(robot) && n.isAlive() && n.isOwnedBy(minecraft.player)
                && minecraft.player.distanceToSqr(n) <= 64 ? n : null;
    }

    @Override protected void init() {
        String draft = input == null ? "" : input.getValue();
        boxWidth = Math.min(480, width - 20);
        left = (width - boxWidth) / 2; top = 76; bottom = height - 76;
        quickButtons.clear();
        var values=new ArrayList<String>();values.add("auto");
        for(var option:languages) values.add(option.code());
        languageButton=addRenderableWidget(CycleButton.<String>builder(v->Component.literal(languageName(v)),preference)
                .withValues(values).create(left,46,boxWidth,20,Component.literal(text("ui.language")),(button,value)->changeLanguage(value)));
        input = new EditBox(font, left, height - 64, boxWidth - 60, 20, Component.literal(text("ui.input")));
        input.setMaxLength(NovaDialogueBrain.MAX_INPUT);
        input.setHint(Component.literal(text("ui.hint")));
        input.setValue(draft);
        input.setResponder(s -> refreshButtons());
        addRenderableWidget(input);
        send = addRenderableWidget(Button.builder(Component.literal(text("ui.send")), b -> send(input.getValue()))
                .bounds(left + boxWidth - 54, height - 64, 54, 20).build());
        String[] labels = {text("ui.battery"),text("ui.status"),text("ui.equipment"),text("ui.memory"),text("ui.guide")};
        String[] messages = {text("ui.query_battery"),text("ui.query_status"),text("ui.query_equipment"),text("ui.query_memory"),text("ui.query_guide")};
        int step = boxWidth / 6;
        for (int i = 0; i < labels.length; i++) {
            String message = messages[i];
            quickButtons.add(addRenderableWidget(Button.builder(Component.literal(labels[i]), b -> send(message))
                    .bounds(left + i * step, height - 36, step - 4, 20).build()));
        }
        addRenderableWidget(Button.builder(Component.literal(text("ui.back")), b -> {
            NovaEntity n = nova();
            if (n == null) onClose(); else minecraft.setScreen(new NovaControlScreen(n));
        }).bounds(left + 5 * step, height - 36, boxWidth - 5 * step, 20).build());
        setInitialFocus(input);
        wrap();
        refreshButtons();
        if(firstInit) {
            firstInit=false;
            String saved=NovaChatLanguageSettings.get();
            if(!saved.equals(preference) && (saved.equals("auto") || languages.stream().anyMatch(l->l.code().equals(saved)))) changeLanguage(saved);
        }
    }

    private void refreshButtons() {
        if (send == null || input == null) return;
        boolean ready = !waiting && cooldown == 0;
        send.active = ready && !input.getValue().isBlank();
        for (var b : quickButtons) b.active = ready;
        if(languageButton!=null) languageButton.active=ready;
    }

    private void send(String text) {
        if (waiting || cooldown > 0 || nova() == null) return;
        String clean = NovaDialogueBrain.clean(text);
        if (clean.isBlank()) return;
        waiting = true; waitingTicks = 0; cooldown = 22;
        ClientPlayNetworking.send(new NovaDialogueNetwork.Request(entityId, robot, token, ++sequence, clean, preference));
        input.setValue("");
        setInitialFocus(input);
        refreshButtons();
    }

    private void changeLanguage(String selected) {
        if(waiting || cooldown>0 || nova()==null) return;
        preference=selected;
        waiting=true;waitingTicks=0;cooldown=22;
        ClientPlayNetworking.send(new NovaDialogueNetwork.Request(entityId,robot,token,++sequence,"",selected));
        refreshButtons();
    }

    private void receive(NovaDialogueNetwork.Reply page) {
        if (page.entityId() != entityId || !page.robot().equals(robot) || !page.token().equals(token)
                || page.sequence() != sequence || page.sequence() <= shownSequence) return;
        shownSequence = page.sequence();
        boolean changed=!page.ui().isEmpty() && (!language.equals(page.language()) || !ui.equals(page.ui()) || !languages.equals(page.languages()));
        if(!page.ui().isEmpty()) {
            language=page.language();preference=page.preference();ui=page.ui();languages=page.languages();
            NovaChatLanguageSettings.save(preference);
        }
        waiting = false;
        if(changed) rebuildWidgets();
        if (!page.input().isBlank()) history.add(new Message(true, page.input()));
        history.add(new Message(false, page.text()));
        while (history.size() > 24) history.removeFirst();
        wrap();
        refreshButtons();
    }

    private void wrap() {
        lines.clear();
        for (Message message : history) {
            int color = message.owner() ? 0xFFFFFFFF : 0xFF80E5FF;
            for (var text : font.split(Component.literal((message.owner() ? text("ui.you")+": " : "NOVA: ") + message.text()), boxWidth - 16))
                lines.add(new Line(text, color));
            lines.add(new Line(Component.literal(" ").getVisualOrderText(), color));
        }
        scroll = maxScroll();
    }

    private int maxScroll() { return Math.max(0, lines.size() * 12 - (bottom - top)); }
    @Override public void tick() {
        if (nova() == null) { onClose(); return; }
        if (cooldown > 0) cooldown--;
        if (waiting && ++waitingTicks >= 100) {
            waiting = false;
            history.add(new Message(false, text("ui.timeout")));
            while (history.size() > 24) history.removeFirst();
            wrap();
        }
        refreshButtons();
    }

    @Override public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            send(input.getValue());
            return true;
        }
        return super.keyPressed(event);
    }

    @Override public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        if (x >= left && x <= left + boxWidth && y >= top && y <= bottom) {
            scroll = Mth.clamp(scroll - (int)(vertical * 24), 0, maxScroll());
            return true;
        }
        return super.mouseScrolled(x, y, horizontal, vertical);
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        super.render(g, mouseX, mouseY, delta);
        g.drawCenteredString(font, Component.literal(text("ui.title")), width / 2, 13, 0xFF80E5FF);
        g.drawCenteredString(font, waiting ? text("ui.waiting") : text("ui.help"),
                width / 2, 29, 0xFFD4EDF2);
        g.fill(left, top - 4, left + boxWidth, bottom + 4, 0xDD071E2B);
        g.enableScissor(left, top, left + boxWidth, bottom);
        for (int i = Math.max(0, scroll / 12); i < lines.size(); i++) {
            int y = top + i * 12 - scroll;
            if (y >= bottom) break;
            var line = lines.get(i);
            g.drawString(font, line.text(), left + 8, y, line.color(), false);
        }
        g.disableScissor();
    }
    @Override public boolean isPauseScreen() { return false; }
}
