package sukima;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class SukimaClient implements ClientModInitializer {
    static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    public static final Map<String, Module> MODULES = new LinkedHashMap<>();
    public static final Set<String> FRIENDS = new HashSet<>();
    static boolean rShiftWas = true;

    static void add(Module m) { MODULES.put(m.name.toLowerCase(), m); }

    public static List<Module> byCategory(Module.Category c) {
        return MODULES.values().stream().filter(m -> m.category == c).toList();
    }

    static void msg(String s) { if (mc().player != null) mc().player.sendMessage(Text.literal("§d[Sukima]§r " + s), false); }

    static int parseKey(String s) {
        s = s.toLowerCase();
        if (s.equals("none")) return -1;
        if (s.equals("rshift")) return GLFW.GLFW_KEY_RIGHT_SHIFT;
        if (s.equals("lshift")) return GLFW.GLFW_KEY_LEFT_SHIFT;
        if (s.matches("f([1-9]|1[0-2])")) return GLFW.GLFW_KEY_F1 + Integer.parseInt(s.substring(1)) - 1;
        if (s.length() == 1) return Character.toUpperCase(s.charAt(0)); // A-Z, 0-9 совпадают с GLFW
        return -2;
    }

    @Override
    public void onInitializeClient() {
        add(new Modules.KillAura());
        add(new Modules.Criticals());
        add(new Modules.TriggerBot());
        add(new Modules.Velocity());
        add(new Modules.AutoTotem());
        add(new Modules.Sprint());
        add(new Modules.Flight());
        add(new Modules.Speed());
        add(new Modules.Step());
        add(new Modules.NoFall());
        add(new Modules.ESP());
        add(new Modules.Fullbright());

        Hud.init();

        // Команды: .gui | .t <модуль> | .list | .bind <модуль> <клавиша|none> | .friend add/remove/list <ник>
        ClientSendMessageEvents.ALLOW_CHAT.register(text -> {
            if (!text.startsWith(".")) return true;
            String[] a = text.substring(1).split(" ");
            switch (a[0]) {
                case "gui" -> mc().execute(() -> mc().setScreen(new ClickGui()));
                case "list" -> MODULES.values().forEach(m -> msg(m.name + ": " + (m.enabled ? "ON" : "off")));
                case "t" -> {
                    Module m = a.length > 1 ? MODULES.get(a[1].toLowerCase()) : null;
                    if (m != null) { m.toggle(); msg(m.name + (m.enabled ? " ON" : " OFF")); }
                }
                case "bind" -> {
                    Module m = a.length > 2 ? MODULES.get(a[1].toLowerCase()) : null;
                    int k = a.length > 2 ? parseKey(a[2]) : -2;
                    if (m != null && k != -2) { m.key = k; msg(m.name + " bind: " + a[2]); }
                    else msg("Использование: .bind <модуль> <клавиша|none>");
                }
                case "friend" -> {
                    if (a.length > 1 && a[1].equals("list")) msg("Друзья: " + FRIENDS);
                    else if (a.length > 2 && a[1].equals("add")) { FRIENDS.add(a[2].toLowerCase()); msg("+ " + a[2]); }
                    else if (a.length > 2 && a[1].equals("remove")) { FRIENDS.remove(a[2].toLowerCase()); msg("- " + a[2]); }
                }
                default -> msg(".gui | .t <модуль> | .list | .bind <модуль> <клавиша> | .friend add/remove/list <ник>");
            }
            return false;
        });

        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (c.player == null || c.world == null) return;
            long h = c.getWindow().getHandle();

            // бинды работают, только когда не открыт экран (чат, меню)
            if (c.currentScreen == null) {
                for (Module m : MODULES.values()) {
                    if (m.key < 0) continue;
                    boolean down = GLFW.glfwGetKey(h, m.key) == GLFW.GLFW_PRESS;
                    if (down && !m.keyWas) { m.toggle(); msg(m.name + (m.enabled ? " ON" : " OFF")); }
                    m.keyWas = down;
                }
                boolean rs = GLFW.glfwGetKey(h, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
                if (rs && !rShiftWas) c.setScreen(new ClickGui());
                rShiftWas = rs;
            }

            for (Module m : MODULES.values()) if (m.enabled) m.onTick();
        });
    }
}
