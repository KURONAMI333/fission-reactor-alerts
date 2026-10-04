package com.kuronami.fissionreactoralerts.alert;

/**
 * Per-reactor alert latch. Pure logic, unit-tested without Minecraft.
 *
 * <p>Fires once when reactor damage first appears ({@code 0 -> >0}, including
 * the first observation of an already-damaged reactor), then stays latched for
 * the whole danger episode. Re-arms only when damage returns to zero, so a
 * reactor that keeps hovering above zero cannot spam, while a reactor that
 * recovered and then overheats again does re-alert — matching the required
 * "alert once per danger episode" semantics.
 */
public final class DamageAlertState {

    private boolean alerted;

    /**
     * @param reactorDamage current Mekanism {@code reactorDamage} value (0–100)
     * @return true when this observation should send exactly one alert
     */
    public boolean update(double reactorDamage) {
        if (reactorDamage <= 0) {
            alerted = false;
            return false;
        }
        if (alerted) {
            return false;
        }
        alerted = true;
        return true;
    }

    /** True while this reactor is inside a latched danger episode. */
    public boolean isAlerted() {
        return alerted;
    }
}
