package com.carrot123.yixiangshouronglu.gene;

public final class GeneTypes {
    private GeneTypes() {}
    public enum Region {
        NERVOUS(2), VISION(1), CARDIOPULMONARY(2), MUSCLE(3), SKELETAL(2);
        public final int slots;
        Region(int slots) { this.slots = slots; }
        public String key() { return "gene.yixiangshouronglu.region." + name().toLowerCase(java.util.Locale.ROOT); }
    }
    public enum Strength {
        ZAYIN, TETH, HE, WAW, ALEPH;
        public String key() { return "gene.yixiangshouronglu.strength." + name().toLowerCase(java.util.Locale.ROOT); }
    }
    public enum Quality {
        ALPHA, BETA, GAMMA;
        public Quality next() { return this == GAMMA ? null : values()[ordinal() + 1]; }
        public String key() { return "gene.yixiangshouronglu.quality." + name().toLowerCase(java.util.Locale.ROOT); }
        public String symbol() { return switch (this) { case ALPHA -> "α"; case BETA -> "β"; case GAMMA -> "γ"; }; }
    }
    public enum Capacity {
        I(20), II(25), III(30), IV(35), V(40);
        public final int limit;
        Capacity(int limit) { this.limit = limit; }
    }
    public enum Rejection { NONE, MILD, SEVERE, UNKNOWN }
    public enum Mode { SYNTHESIS, UPGRADE }
    public static Rejection rejection(long used, int normal) {
        long extra = used - normal;
        return extra <= 0 ? Rejection.NONE : extra <= 2 ? Rejection.MILD : Rejection.SEVERE;
    }
}
