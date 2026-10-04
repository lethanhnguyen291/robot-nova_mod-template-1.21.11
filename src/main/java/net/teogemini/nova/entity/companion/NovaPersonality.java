package net.teogemini.nova.entity.companion;

public enum NovaPersonality {
    CURIOUS("Tò mò", 600, 2), CAREFUL("Cẩn thận", 1000, 1),
    CHEERFUL("Vui vẻ", 500, 0), CALM("Điềm tĩnh", 1200, 7);
    public final String label;
    public final int socialDelay;
    public final int idleFace;
    NovaPersonality(String label, int socialDelay, int idleFace) {
        this.label = label; this.socialDelay = socialDelay; this.idleFace = idleFace;
    }
    public static NovaPersonality fromId(int id) {
        return values()[Math.floorMod(id, values().length)];
    }
}
