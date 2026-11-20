package sukima;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class ClickGui extends Screen {
    static final int W = 120, HEAD = 16, ROW = 14, SROW = 12;
    final Map<Module.Category, int[]> pos = new LinkedHashMap<>();
    Module.Category dragging;
    int dragDx, dragDy;
    boolean wasL, wasR, wasShift = true;

    public ClickGui() {
        super(Text.literal("Sukima"));
        int x = 20;
        for (Module.Category c : Module.Category.values()) { pos.put(c, new int[]{x, 20}); x += W + 12; }
    }

    @Override public boolean shouldPause() { return false; }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        var tr = MinecraftClient.getInstance().textRenderer;
        long h = MinecraftClient.getInstance().getWindow().getHandle();
        boolean l = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
        boolean r = GLFW.glfwGetMouseButton(h, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;
        boolean click = l && !wasL, rclick = r && !wasR;
        wasL = l; wasR = r;
        boolean sh = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        if (sh && !wasShift) { close(); return; }
        wasShift = sh;

        ctx.fill(0, 0, width, height, 0x70000000);
        if (!l) dragging = null;
        if (dragging != null) { int[] p = pos.get(dragging); p[0] = mx - dragDx; p[1] = my - dragDy; }

        int idx = 0;
        for (var e : pos.entrySet()) {
            Module.Category cat = e.getKey();
            int x = e.getValue()[0], y = e.getValue()[1];

            ctx.fillGradient(x, y, x + W, y + HEAD, Theme.accent(idx * 80), 0xFF2A1F45);
            ctx.drawCenteredTextWithShadow(tr, cat.label, x + W / 2, y + 4, 0xFFFFFFFF);
            if (click && inside(mx, my, x, y, W, HEAD)) { dragging = cat; dragDx = mx - x; dragDy = my - y; }

            int ry = y + HEAD;
            for (Module m : SukimaClient.byCategory(cat)) {
                boolean hover = inside(mx, my, x, ry, W, ROW);
                int bg = m.enabled ? 0xE01E1633 : 0xD0101018;
                if (hover) bg = m.enabled ? 0xF0281E45 : 0xE01A1A26;
                ctx.fill(x, ry, x + W, ry + ROW, bg);
                if (m.enabled) ctx.fill(x, ry, x + 2, ry + ROW, Theme.accent(idx * 80 + ry));
                ctx.drawTextWithShadow(tr, m.name, x + 7, ry + 3, m.enabled ? Theme.accent(ry) : 0xFFBBBBBB);
                if (!m.settings.isEmpty()) ctx.drawTextWithShadow(tr, m.expanded ? "-" : "+", x + W - 10, ry + 3, 0xFF777777);
                if (m.key >= 0) {
                    String k = "[" + (m.key >= 32 && m.key <= 126 ? String.valueOf((char) m.key) : "K" + m.key) + "]";
                    ctx.drawTextWithShadow(tr, k, x + W - 14 - tr.getWidth(k), ry + 3, 0xFF777777);
                }
                if (dragging == null && hover) {
                    if (click) m.toggle();
                    if (rclick && !m.settings.isEmpty()) m.expanded = !m.expanded;
                }
                ry += ROW;

                if (m.expanded) {
                    for (Setting s : m.settings) {
                        boolean sh2 = inside(mx, my, x, ry, W, SROW);
                        ctx.fill(x, ry, x + W, ry + SROW, 0xE00B0B12);
                        if (s instanceof Setting.Num n) {
                            if (l && sh2 && dragging == null) n.setFromRatio((mx - x) / (double) W);
                            int fw = (int) (W * (n.value - n.min) / (n.max - n.min));
                            ctx.fill(x, ry, x + fw, ry + SROW, (Theme.accent(ry) & 0x00FFFFFF) | 0x66000000);
                            ctx.drawTextWithShadow(tr, n.name, x + 7, ry + 2, 0xFFCCCCCC);
                            String v = String.format("%.2f", n.value);
                            ctx.drawTextWithShadow(tr, v, x + W - 5 - tr.getWidth(v), ry + 2, 0xFFFFFFFF);
                        } else if (s instanceof Setting.Bool b) {
                            if (click && sh2 && dragging == null) b.value = !b.value;
                            ctx.drawTextWithShadow(tr, b.name, x + 7, ry + 2, 0xFFCCCCCC);
                            String v = b.value ? "ON" : "OFF";
                            ctx.drawTextWithShadow(tr, v, x + W - 5 - tr.getWidth(v), ry + 2, b.value ? Theme.accent(ry) : 0xFF777777);
                        } else if (s instanceof Setting.Mode md) {
                            if (click && sh2 && dragging == null) md.cycle();
                            ctx.drawTextWithShadow(tr, md.name, x + 7, ry + 2, 0xFFCCCCCC);
                            ctx.drawTextWithShadow(tr, md.get(), x + W - 5 - tr.getWidth(md.get()), ry + 2, 0xFFFFFFFF);
                        }
                        ry += SROW;
                    }
                }
            }
            ctx.fill(x, ry, x + W, ry + 2, Theme.accent(idx * 80 + 100));
            idx++;
        }
        ctx.drawCenteredTextWithShadow(tr, "ЛКМ — вкл/выкл · ПКМ — настройки · RShift — закрыть", width / 2, height - 14, Theme.accent(0));
    }

    static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
