package org.jcs.egm.stones.stone_mind;

public enum MindStoneSuggestionEffect {
    GO_AWAY("Go Away"),
    DROP_HELD_ITEM("Drop Held Item"),
    FREEZE("Freeze"),
    ENRAGE("Enrage");

    private final String displayName;

    MindStoneSuggestionEffect(String name) {
        this.displayName = name;
    }
    public String getDisplayName() { return displayName; }

    public static MindStoneSuggestionEffect fromOrdinal(int i) {
        return values()[Math.max(0, Math.min(values().length - 1, i))];
    }
}
