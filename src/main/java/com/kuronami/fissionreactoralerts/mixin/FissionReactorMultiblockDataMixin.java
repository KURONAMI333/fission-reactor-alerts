package com.kuronami.fissionreactoralerts.mixin;

import com.kuronami.fissionreactoralerts.alert.ReactorAlertTracker;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Watches each formed fission reactor once per server tick.
 *
 * <p>Mekanism ticks the live {@link FissionReactorMultiblockData} from the
 * master tile's {@code onUpdateServer}, so a RETURN injection sees the
 * post-{@code handleDamage} damage value on the server thread. The handler is
 * read-only: it never calls into Mekanism mutation paths.
 */
@Mixin(FissionReactorMultiblockData.class)
public abstract class FissionReactorMultiblockDataMixin {

    @Inject(method = "tick", at = @At("RETURN"))
    private void fissionreactoralerts$watch(Level world, CallbackInfoReturnable<Boolean> cir) {
        ReactorAlertTracker.onReactorTick((FissionReactorMultiblockData) (Object) this, world);
    }
}
