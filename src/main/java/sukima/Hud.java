package sukima;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.Comparator;
import java.util.List;

public class Hud {
    public static void init() {
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> render(ctx));
    }

    static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        var tr = mc.textRenderer;

        // ---- ватермарк ----
        String wm = "Sukima";
        String info = " | " + mc.getCurrentFps() + " fps";
        int w = tr.getWidth(wm + info) + 8;
        ctx.fill(4, 4, 4 + w, 18, 0xA0101018);
        ctx.fillGradient(4, 4, 4 + w, 6, Theme.accent(0), Theme.accent(60));
        int x = 8;
        for (char c : wm.toCharArray()) {               // градиентный текст
            ctx.drawTextWithShadow(tr, String.valueOf(c), x, 8, Theme.accent(x * 3));
            x += tr.getWidth(String.valueOf(c));
        }
        ctx.drawTextWithShadow(tr, info, x, 8, 0xFFAAAAAA);

        // ---- список включённых модулей справа ----
        List<Module> on = SukimaClient.MODULES.values().stream()
                .filter(m -> m.enabled)
                .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(m.name)).reversed())
                .toList();
        int sw = ctx.getScaledWindowWidth();
        int y = 4, i = 0;
        for (Module m : on) {
            int tw = tr.getWidth(m.name);
            int col = Theme.accent(i * 40);
            ctx.fill(sw - tw - 8, y, sw, y + 11, 0x90101018);
            ctx.fill(sw - 2, y, sw, y + 11, col);
            ctx.drawTextWithShadow(tr, m.name, sw - tw - 5, y + 2, col);
            y += 11; i++;
        }
    }
}
