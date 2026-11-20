package sukima;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.List;

public class Modules {
    static MinecraftClient mc() { return MinecraftClient.getInstance(); }

    static boolean on(String name) {
        Module m = SukimaClient.MODULES.get(name.toLowerCase());
        return m != null && m.enabled;
    }

    static boolean friend(PlayerEntity p) {
        return SukimaClient.FRIENDS.contains(p.getName().getString().toLowerCase());
    }

    /** Движение по WASD в направлении взгляда: {dx, dz}. */
    static double[] dir(double speed) {
        var o = mc().options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        double rad = Math.toRadians(mc().player.getYaw());
        return new double[]{(-Math.sin(rad) * f + Math.cos(rad) * s) * speed,
                            (Math.cos(rad) * f + Math.sin(rad) * s) * speed};
    }

    // ================= COMBAT =================
    public static class KillAura extends Module {
        Setting.Num range = num("Range", 4.2, 3.0, 6.0, 0.1);
        Setting.Num cps = num("CPS", 12, 1, 20, 1);
        Setting.Bool cooldown = bool("Cooldown", true);
        Setting.Bool rotate = bool("Rotate", true);
        Setting.Bool walls = bool("Walls", false);
        Setting.Mode priority = mode("Priority", "Distance", "Health");
        long last;

        public KillAura() { super("KillAura", Category.COMBAT); }

        @Override public void onTick() {
            var mc = mc();
            List<PlayerEntity> list = mc.world.getPlayers().stream()
                    .filter(p -> p != mc.player && p.isAlive() && !friend(p))
                    .filter(p -> mc.player.distanceTo(p) <= range.value)
                    .filter(p -> walls.value || mc.player.canSee(p))
                    .sorted(priority.idx == 0
                            ? Comparator.comparingDouble(p -> mc.player.distanceTo(p))
                            : Comparator.comparingDouble(p -> p.getHealth()))
                    .toList();
            if (list.isEmpty()) return;
            PlayerEntity t = list.get(0);

            if (rotate.value) {
                Vec3d eye = mc.player.getEyePos();
                Vec3d to = t.getPos().add(0, t.getHeight() * 0.65, 0);
                Vec3d d = to.subtract(eye);
                mc.player.setYaw((float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f);
                mc.player.setPitch((float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z))));
            }

            boolean crit = on("Criticals");
            if (crit && mc.player.isOnGround()) { mc.player.jump(); return; }
            if (crit && mc.player.fallDistance <= 0) return;

            long now = System.currentTimeMillis();
            boolean ready = cooldown.value ? mc.player.getAttackCooldownProgress(0f) >= 0.95f
                                           : now - last >= 1000.0 / cps.value;
            if (!ready) return;
            last = now;
            mc.interactionManager.attackEntity(mc.player, t);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    /** Флаг: KillAura бьёт только в падении (крит). Прыгает сам. */
    public static class Criticals extends Module {
        public Criticals() { super("Criticals", Category.COMBAT); }
    }

    public static class TriggerBot extends Module {
        public TriggerBot() { super("TriggerBot", Category.COMBAT); }
        @Override public void onTick() {
            var mc = mc();
            if (mc.crosshairTarget instanceof EntityHitResult r
                    && r.getEntity() instanceof PlayerEntity p && !friend(p)
                    && mc.player.getAttackCooldownProgress(0f) >= 0.95f) {
                mc.interactionManager.attackEntity(mc.player, p);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    public static class Velocity extends Module {
        public Velocity() { super("Velocity", Category.COMBAT); }  // логика в VelocityMixin
    }

    public static class AutoTotem extends Module {
        Setting.Num health = num("Health", 20, 1, 20, 1);
        public AutoTotem() { super("AutoTotem", Category.COMBAT); }
        @Override public void onTick() {
            var mc = mc();
            if (mc.currentScreen != null) return;
            if (mc.player.getHealth() > health.value) return;
            if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
            for (int i = 9; i <= 44; i++) {
                if (mc.player.playerScreenHandler.getSlot(i).getStack().isOf(Items.TOTEM_OF_UNDYING)) {
                    mc.interactionManager.clickSlot(0, i, 40, SlotActionType.SWAP, mc.player);
                    return;
                }
            }
        }
    }

    // ================= MOVEMENT =================
    public static class Sprint extends Module {
        public Sprint() { super("Sprint", Category.MOVEMENT); }
        @Override public void onTick() {
            var p = mc().player;
            if (p.forwardSpeed > 0 && !p.horizontalCollision) p.setSprinting(true);
        }
    }

    public static class Flight extends Module {
        Setting.Num speed = num("Speed", 0.8, 0.1, 3.0, 0.1);
        public Flight() { super("Flight", Category.MOVEMENT); }
        @Override public void onTick() {
            var o = mc().options;
            double[] d = dir(speed.value);
            double dy = o.jumpKey.isPressed() ? speed.value * 0.6 : o.sneakKey.isPressed() ? -speed.value * 0.6 : 0;
            mc().player.setVelocity(d[0], dy, d[1]);
        }
    }

    public static class Speed extends Module {
        Setting.Num speed = num("Speed", 0.35, 0.1, 1.5, 0.05);
        Setting.Bool hop = bool("Bhop", true);
        public Speed() { super("Speed", Category.MOVEMENT); }
        @Override public void onTick() {
            var p = mc().player;
            double[] d = dir(speed.value);
            if (d[0] == 0 && d[1] == 0) return;
            if (hop.value && p.isOnGround()) p.jump();
            p.setVelocity(d[0], p.getVelocity().y, d[1]);
        }
    }

    public static class Step extends Module {
        Setting.Num height = num("Height", 1.5, 0.6, 2.5, 0.1);
        public Step() { super("Step", Category.MOVEMENT); }
        @Override public void onTick() {
            var a = mc().player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
            if (a != null) a.setBaseValue(height.value);
        }
        @Override public void onDisable() {
            var a = mc().player.getAttributeInstance(EntityAttributes.STEP_HEIGHT);
            if (a != null) a.setBaseValue(0.6);
        }
    }

    public static class NoFall extends Module {
        public NoFall() { super("NoFall", Category.MOVEMENT); }
        @Override public void onTick() {
            if (mc().player.fallDistance > 2.5f) mc().player.setOnGround(true);
        }
    }

    // ================= VISUAL =================
    public static class ESP extends Module {
        public ESP() { super("ESP", Category.VISUAL); }
        @Override public void onTick() {
            for (PlayerEntity p : mc().world.getPlayers()) if (p != mc().player) p.setGlowing(true);
        }
        @Override public void onDisable() {
            for (PlayerEntity p : mc().world.getPlayers()) p.setGlowing(false);
        }
    }

    public static class Fullbright extends Module {
        public Fullbright() { super("Fullbright", Category.VISUAL); }
        @Override public void onTick() {
            mc().player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false));
        }
        @Override public void onDisable() { mc().player.removeStatusEffect(StatusEffects.NIGHT_VISION); }
    }
}
