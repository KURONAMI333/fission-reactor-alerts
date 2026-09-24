package com.kuronami.fissionreactoralerts;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Server-side early warning for Mekanism fission reactors.
 *
 * <p>A mixin tail on {@code FissionReactorMultiblockData#tick} observes the
 * live damage value once per server tick per formed reactor. When damage
 * first appears the mod sends one chat alert to the online members of the
 * team claiming the reactor's chunks (FTB Chunks/Teams), or to nearby players
 * when the site is unclaimed. The reactor itself is never modified — this mod
 * only reads state that Mekanism already exposes.
 */
@Mod(FissionReactorAlerts.MODID)
public final class FissionReactorAlerts {
    public static final String MODID = "fissionreactoralerts";
    public static final Logger LOGGER = LogUtils.getLogger();

    public FissionReactorAlerts() {
        LOGGER.info("Fission Reactor Alerts loaded");
    }
}
