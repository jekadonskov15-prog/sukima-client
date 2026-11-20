package sukima.mixin;

import sukima.Module;
import sukima.SukimaClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class VelocityMixin {
    @Inject(method = "onEntityVelocityUpdate", at = @At("HEAD"), cancellable = true)
    private void sukima$velocity(EntityVelocityUpdateS2CPacket packet, CallbackInfo ci) {
        Module m = SukimaClient.MODULES.get("velocity");
        MinecraftClient mc = MinecraftClient.getInstance();
        if (m != null && m.enabled && mc.player != null && packet.getEntityId() == mc.player.getId()) ci.cancel();
    }
}
