package net.teogemini.nova.entity.companion;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Small, bounded, per-robot save data. No offline decay or global robot references. */
public final class NovaMemories {
    private final List<String> journal = new ArrayList<>();
    private final List<GlobalPos> deposits = new ArrayList<>();
    private final List<GlobalPos> buttons = new ArrayList<>();
    private int personality = -1, bond, mined, planted, harvested, logs, delivered;
    private boolean autoTools, metOwner, firstDiamond, rescued;
    private String ownerName = "";
    public NovaPersonality personality() { return NovaPersonality.fromId(Math.max(0, personality)); }
    public void initialize(int seed) { if (personality < 0) personality = Math.floorMod(seed, 4); }
    public void cyclePersonality() { personality = (personality().ordinal() + 1) % 4; }
    public int bond() { return bond; }
    public void growBond(int amount) { bond = (int)Math.min(100, (long)bond + Math.max(0, amount)); }
    public boolean autoTools() { return autoTools; }
    public void toggleAutoTools() { autoTools = !autoTools; }
    public List<String> journal() { return List.copyOf(journal); }
    public List<GlobalPos> deposits() { return List.copyOf(deposits); }
    public List<GlobalPos> buttons() { return List.copyOf(buttons); }
    public void remember(String text) {
        text = text.substring(0, Math.min(180, text.length()));
        if (!journal.isEmpty() && journal.getFirst().equals(text)) return;
        journal.addFirst(text);
        while (journal.size() > 8) journal.removeLast();
    }
    public void meet(String name) {
        if (metOwner) return;
        metOwner = true; ownerName = name.substring(0, Math.min(64, name.length()));
        remember("Lần đầu kết nối với " + ownerName + "."); growBond(5);
    }
    public void diamond() {
        if (firstDiamond) return;
        firstDiamond = true; growBond(5); remember("Tìm được viên kim cương đầu tiên cho chủ nhân!");
    }
    public void rescued() {
        if (rescued) return;
        rescued = true; growBond(5); remember("Chủ nhân đã giúp mình thoát khỏi chỗ mắc kẹt.");
    }
    public void chargedFromEmpty() { remember("Được chủ nhân tiếp năng lượng khi pin cạn."); }
    public void mined() { mined = increment(mined, 1); }
    public void log() { logs = increment(logs, 1); }
    public void planted() { planted = increment(planted, 1); }
    public void harvested() { harvested = increment(harvested, 1); }
    public void delivered(int count) { delivered = increment(delivered, count); }
    private static int increment(int value, int count) {
        return (int)Math.min(Integer.MAX_VALUE, (long)value + Math.max(0, count));
    }
    public String totals() {
        return "Quặng " + mined + " • Gỗ " + logs + " • Trồng " + planted
                + " • Thu hoạch " + harvested + " • Cất đồ " + delivered;
    }
    // Returns 1 added, 0 removed, -1 full. Authorizations are explicit and bounded.
    public int toggleDeposit(GlobalPos pos) { return toggle(deposits, pos); }
    public int toggleButton(GlobalPos pos) { return toggle(buttons, pos); }
    private static int toggle(List<GlobalPos> list, GlobalPos pos) {
        if (list.remove(pos)) return 0;
        if (list.size() >= 9) return -1;
        list.add(pos); return 1;
    }
    public void save(ValueOutput out) {
        out.putInt("Personality", personality); out.putInt("Bond", bond);
        out.putBoolean("AutoTools", autoTools); out.putBoolean("MetOwner", metOwner);
        out.putBoolean("FirstDiamond", firstDiamond); out.putBoolean("Rescued", rescued);
        out.putString("OwnerName", ownerName);
        out.putInt("Mined", mined); out.putInt("Logs", logs); out.putInt("Planted", planted);
        out.putInt("Harvested", harvested); out.putInt("Delivered", delivered);
        out.store("Journal", Codec.STRING.listOf(), journal);
        out.store("Deposits", GlobalPos.CODEC.listOf(), deposits);
        out.store("Buttons", GlobalPos.CODEC.listOf(), buttons);
    }
    public void load(ValueInput in) {
        personality = Math.max(-1, Math.min(3, in.getIntOr("Personality", -1)));
        bond = Math.max(0, Math.min(100, in.getIntOr("Bond", 0)));
        autoTools = in.getBooleanOr("AutoTools", false); metOwner = in.getBooleanOr("MetOwner", false);
        firstDiamond = in.getBooleanOr("FirstDiamond", false); rescued = in.getBooleanOr("Rescued", false);
        ownerName = in.getStringOr("OwnerName", "");
        mined = Math.max(0, in.getIntOr("Mined", 0)); logs = Math.max(0, in.getIntOr("Logs", 0));
        planted = Math.max(0, in.getIntOr("Planted", 0)); harvested = Math.max(0, in.getIntOr("Harvested", 0));
        delivered = Math.max(0, in.getIntOr("Delivered", 0));
        journal.clear();
        in.read("Journal", Codec.STRING.listOf()).orElse(List.of()).stream().limit(8)
                .map(s -> s.substring(0, Math.min(180, s.length()))).forEach(journal::add);
        deposits.clear(); buttons.clear();
        in.read("Deposits", GlobalPos.CODEC.listOf()).orElse(List.of()).stream().distinct().limit(9).forEach(deposits::add);
        in.read("Buttons", GlobalPos.CODEC.listOf()).orElse(List.of()).stream().distinct().limit(9).forEach(buttons::add);
    }
}
