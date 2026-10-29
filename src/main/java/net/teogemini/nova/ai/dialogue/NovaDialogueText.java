package net.teogemini.nova.ai.dialogue;

import java.util.Map;
import java.util.regex.Pattern;

/** Positional templates let each language reorder names, counts and other live facts. */
public final class NovaDialogueText {
    private static final Pattern VARIABLE=Pattern.compile("\\{(\\d+)}");
    private NovaDialogueText() {}
    public static String itemName(String language,net.minecraft.world.item.ItemStack stack) {
        String original=stack.getHoverName().getString();
        if (language.equals("vi_vn") || stack.has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) return original;
        var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem());
        String key="item."+id.getNamespace()+"."+id.getPath();
        String translated=NovaDialogueLoader.message(language,key);
        return translated.equals(key) ? original : translated;
    }
    public static String entityName(String language,net.minecraft.world.entity.Entity entity) {
        String original=entity.getName().getString();
        if(language.equals("vi_vn") || entity.hasCustomName()) return original;
        var id=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        String key="entity."+id.getNamespace()+"."+id.getPath();
        String translated=NovaDialogueLoader.message(language,key);
        return translated.equals(key) ? original : translated;
    }
    public static String tr(String language,String key,Object... args) {
        String template=NovaDialogueLoader.message(language,key);
        var matcher=VARIABLE.matcher(template);
        var output=new StringBuilder();
        while(matcher.find()) {
            int i=Integer.parseInt(matcher.group(1));
            String value=i<args.length ? String.valueOf(args[i]) : matcher.group();
            matcher.appendReplacement(output,java.util.regex.Matcher.quoteReplacement(value));
        }
        matcher.appendTail(output);
        return output.toString();
    }
    public static String personality(String language,String label) {
        String key=switch(label) {
            case "Tò mò" -> "personality.curious";
            case "Cẩn thận" -> "personality.careful";
            case "Vui vẻ" -> "personality.cheerful";
            case "Điềm tĩnh" -> "personality.calm";
            default -> "";
        };
        return key.isEmpty() ? label : tr(language,key);
    }
    /** Keep old saved journals intact; translate known built-in events only when displaying them. */
    public static String memory(String language,String text) {
        if(language.equals("vi_vn")) return text;
        Map<String,String> fixed=Map.of(
            "Tìm được viên kim cương đầu tiên cho chủ nhân!","memory.diamond",
            "Chủ nhân đã giúp mình thoát khỏi chỗ mắc kẹt.","memory.rescued",
            "Được chủ nhân tiếp năng lượng khi pin cạn.","memory.recharged",
            "Chủ nhân đã chọn một chiếc rương làm nhà cho mình.","memory.home",
            "Tặng chủ nhân một bông hoa mình tìm thấy.","memory.flower");
        if(fixed.containsKey(text)) return tr(language,fixed.get(text));
        if(text.startsWith("Lần đầu kết nối với ") && text.endsWith("."))
            return tr(language,"memory.met",text.substring("Lần đầu kết nối với ".length(),text.length()-1));
        var delivered=Pattern.compile("Đã cất an toàn (\\d+) vật phẩm vào rương\\.").matcher(text);
        if(delivered.matches()) return tr(language,"memory.delivered",delivered.group(1));
        return text;
    }
    public static String totals(String language,String text) {
        var matcher=Pattern.compile("Quặng (\\d+) • Gỗ (\\d+) • Trồng (\\d+) • Thu hoạch (\\d+) • Cất đồ (\\d+)").matcher(text);
        return matcher.matches() ? tr(language,"facts.totals",matcher.group(1),matcher.group(2),matcher.group(3),matcher.group(4),matcher.group(5)) : text;
    }
}
