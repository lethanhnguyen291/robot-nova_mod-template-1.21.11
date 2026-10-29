package net.teogemini.nova.network;

import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.teogemini.nova.ROBOTNOVA_MOD;
import net.teogemini.nova.ai.dialogue.*;
import net.teogemini.nova.entity.NovaEntity;

/** Private, owner-authorized dialogue. Each session chooses its own language. */
public final class NovaDialogueNetwork {
    private static final Map<MinecraftServer,NovaDialogueSessions> SERVERS=new IdentityHashMap<>();
    private NovaDialogueNetwork() {}
    public record LanguageOption(String code,String name) {}
    public record Request(int entityId,UUID robot,UUID token,int sequence,String text,String languagePreference)
            implements CustomPacketPayload {
        public Request(int id,UUID robot,UUID token,int sequence,String text) { this(id,robot,token,sequence,text,"auto"); }
        public static final Type<Request> TYPE=new Type<>(ROBOTNOVA_MOD.id("dialogue_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,v)-> {
            b.writeVarInt(v.entityId);b.writeUUID(v.robot);b.writeUUID(v.token);b.writeVarInt(v.sequence);
            b.writeUtf(v.text,NovaDialogueBrain.MAX_INPUT);b.writeUtf(v.languagePreference,16);
        },b->new Request(b.readVarInt(),b.readUUID(),b.readUUID(),b.readVarInt(),
                b.readUtf(NovaDialogueBrain.MAX_INPUT),b.readUtf(16)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Reply(int entityId,UUID robot,UUID token,int sequence,boolean open,String input,String text,
                        String language,String preference,List<LanguageOption> languages,Map<String,String> ui)
            implements CustomPacketPayload {
        public Reply {
            languages=List.copyOf(languages);ui=Map.copyOf(ui);
            if(languages.size()>NovaDialogueLoader.MAX_LANGUAGES || ui.size()>32)
                throw new IllegalArgumentException("Oversized dialogue language metadata");
        }
        public Reply(int id,UUID robot,UUID token,int sequence,boolean open,String input,String text) {
            this(id,robot,token,sequence,open,input,text,"vi_vn","auto",List.of(),Map.of());
        }
        public static final Type<Reply> TYPE=new Type<>(ROBOTNOVA_MOD.id("dialogue_reply"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Reply> CODEC=StreamCodec.of((b,v)-> {
            b.writeVarInt(v.entityId);b.writeUUID(v.robot);b.writeUUID(v.token);
            b.writeVarInt(v.sequence);b.writeBoolean(v.open);b.writeUtf(v.input,NovaDialogueBrain.MAX_INPUT);b.writeUtf(v.text,1024);
            b.writeUtf(v.language,16);b.writeUtf(v.preference,16);b.writeVarInt(v.languages.size());
            for(var option:v.languages) { b.writeUtf(option.code,16);b.writeUtf(option.name,48); }
            b.writeVarInt(v.ui.size());
            for(var entry:v.ui.entrySet()) { b.writeUtf(entry.getKey(),40);b.writeUtf(entry.getValue(),240); }
        },b-> {
            int id=b.readVarInt();UUID robot=b.readUUID(),token=b.readUUID();int sequence=b.readVarInt();
            boolean open=b.readBoolean();String input=b.readUtf(NovaDialogueBrain.MAX_INPUT),text=b.readUtf(1024);
            String language=b.readUtf(16),preference=b.readUtf(16);
            int count=b.readVarInt();
            if(count<0 || count>NovaDialogueLoader.MAX_LANGUAGES) throw new IllegalArgumentException("Invalid language count");
            var options=new ArrayList<LanguageOption>();
            for(int i=0;i<count;i++) options.add(new LanguageOption(b.readUtf(16),b.readUtf(48)));
            int labels=b.readVarInt();
            if(labels<0 || labels>32) throw new IllegalArgumentException("Invalid UI label count");
            var ui=new LinkedHashMap<String,String>();
            for(int i=0;i<labels;i++) {
                String key=b.readUtf(40),value=b.readUtf(240);
                if(ui.putIfAbsent(key,value)!=null) throw new IllegalArgumentException("Duplicate UI label");
            }
            return new Reply(id,robot,token,sequence,open,input,text,language,preference,options,ui);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static NovaDialogueSessions sessions(ServerPlayer player) {
        return SERVERS.computeIfAbsent(player.level().getServer(),s->new NovaDialogueSessions());
    }
    private static long now(ServerPlayer p) { return p.level().getServer().overworld().getGameTime(); }
    private static String dimension(ServerPlayer p) { return p.level().dimension().toString(); }
    private static boolean allowed(ServerPlayer player,NovaEntity nova) {
        return player.isAlive() && !player.isSpectator() && NovaRodControl.holdingRod(player)
                && player.level()==nova.level() && nova.isAlive() && nova.isTame()
                && nova.isOwnedBy(player) && player.distanceToSqr(nova)<=64 && player.hasLineOfSight(nova);
    }
    private static Reply reply(NovaEntity nova,NovaDialogueSessions.Session session,int sequence,boolean open,String input,String text) {
        String language=session.context.language();
        var choices=NovaDialogueLoader.languages().stream().map(c->new LanguageOption(c,NovaDialogueLoader.language(c).name)).toList();
        return new Reply(nova.getId(),nova.getUUID(),session.token,sequence,open,input,text,language,
                session.languagePreference,choices,NovaDialogueLoader.language(language).ui());
    }
    private static void select(ServerPlayer player,NovaDialogueSessions.Session session) {
        session.context.language(NovaDialogueLoader.select(session.languagePreference,player.clientInformation().language()));
    }
    private static String greeting(NovaEntity nova,NovaDialogueSessions.Session session,long now) {
        String language=session.context.language();
        return NovaDialogueBrain.answer(NovaDialogueBrain.Intent.HELLO,NovaDialogueFacts.read(nova,language),session.context,now)
                +" "+NovaDialogueLoader.message(language,"ui.open_hint");
    }
    public static void open(NovaEntity nova,ServerPlayer player) {
        if(!allowed(player,nova)) return;
        long now=now(player);
        var session=sessions(player).open(player.getUUID(),nova.getUUID(),dimension(player),now);
        if(session==null) return;
        select(player,session);
        nova.sounds().play(net.teogemini.nova.sound.NovaSounds.Cue.GREET);
        NovaDialogueFacts.react(nova,NovaDialogueBrain.Intent.HELLO);
        ServerPlayNetworking.send(player,reply(nova,session,0,true,"",greeting(nova,session,now)));
    }
    public static void register() {
        PayloadTypeRegistry.playC2S().register(Request.TYPE,Request.CODEC);
        PayloadTypeRegistry.playS2C().register(Reply.TYPE,Reply.CODEC);
        ServerLifecycleEvents.SERVER_STOPPED.register(SERVERS::remove);
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)-> {
            var store=SERVERS.get(server);if(store!=null) store.disconnect(handler.player.getUUID());
        });
        ServerPlayNetworking.registerGlobalReceiver(Request.TYPE,(request,context)-> {
            var player=context.player();
            if(!player.isAlive() || player.isSpectator()) return;
            long now=now(player);
            var session=sessions(player).find(player.getUUID(),request.robot,request.token,dimension(player),now);
            if(session==null || !session.accept(request.sequence,now)) return;
            if(!(player.level().getEntity(request.entityId) instanceof NovaEntity nova)
                    || !nova.getUUID().equals(request.robot) || !allowed(player,nova)) {
                ServerPlayNetworking.send(player,new Reply(request.entityId,request.robot,request.token,request.sequence,false,"",
                        NovaDialogueLoader.message(session.context.language(),"ui.invalid")));
                return;
            }
            String preference=request.languagePreference;
            if(!preference.equals("auto") && !NovaDialogueLoader.supported(preference)) {
                ServerPlayNetworking.send(player,reply(nova,session,request.sequence,false,"",
                        NovaDialogueLoader.message(session.context.language(),"ui.invalid")));
                return;
            }
            session.languagePreference=preference;
            select(player,session);
            String language=session.context.language(),input=NovaDialogueBrain.clean(request.text);
            if(input.isBlank()) {
                ServerPlayNetworking.send(player,reply(nova,session,request.sequence,false,"",greeting(nova,session,now)));
                return;
            }
            var match=NovaDialogueBrain.match(input,session.context,now);
            var intent=match.intent();
            String answer;
            if(intent.command()) {
                var result=NovaDialogueActions.execute(nova,intent,language);
                answer=NovaDialogueBrain.actionAnswer(match,NovaDialogueFacts.read(nova,language),session.context,now,result);
                nova.sounds().dialogue(intent,!result.responseOverride().isEmpty());
            } else {
                answer=NovaDialogueBrain.answer(match,NovaDialogueFacts.read(nova,language),session.context,now);
                nova.sounds().dialogue(intent,false);
                NovaDialogueFacts.react(nova,intent);
            }
            ServerPlayNetworking.send(player,reply(nova,session,request.sequence,false,input,answer));
        });
    }
}
