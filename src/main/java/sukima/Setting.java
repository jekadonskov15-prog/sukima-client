package sukima;

public abstract class Setting {
    public final String name;
    protected Setting(String name) { this.name = name; }

    public static class Num extends Setting {
        public double value, min, max, step;
        public Num(String n, double v, double min, double max, double step) {
            super(n); this.value = v; this.min = min; this.max = max; this.step = step;
        }
        public void setFromRatio(double t) {
            t = Math.max(0, Math.min(1, t));
            double v = min + t * (max - min);
            value = Math.round(v / step) * step;
        }
    }

    public static class Bool extends Setting {
        public boolean value;
        public Bool(String n, boolean v) { super(n); value = v; }
    }

    public static class Mode extends Setting {
        public final String[] modes; public int idx;
        public Mode(String n, String... modes) { super(n); this.modes = modes; }
        public String get() { return modes[idx]; }
        public void cycle() { idx = (idx + 1) % modes.length; }
    }
}
