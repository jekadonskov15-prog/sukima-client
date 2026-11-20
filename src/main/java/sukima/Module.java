package sukima;

import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.List;

public abstract class Module {
    public enum Category {
        COMBAT("Combat"), MOVEMENT("Movement"), VISUAL("Visual");
        public final String label;
        Category(String l) { label = l; }
    }

    public final String name;
    public final Category category;
    public boolean enabled, expanded, keyWas;
    public int key = -1;
    public final List<Setting> settings = new ArrayList<>();

    protected Module(String name, Category c) { this.name = name; this.category = c; }

    protected Setting.Num num(String n, double v, double min, double max, double step) {
        var s = new Setting.Num(n, v, min, max, step); settings.add(s); return s;
    }
    protected Setting.Bool bool(String n, boolean v) { var s = new Setting.Bool(n, v); settings.add(s); return s; }
    protected Setting.Mode mode(String n, String... m) { var s = new Setting.Mode(n, m); settings.add(s); return s; }

    public void onTick() {}
    public void onEnable() {}
    public void onDisable() {}

    public void toggle() {
        enabled = !enabled;
        if (MinecraftClient.getInstance().player == null) return;
        if (enabled) onEnable(); else onDisable();
    }
}
