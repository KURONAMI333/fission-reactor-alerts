package com.kuronami.fissionreactoralerts.alert;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Covers the acceptance semantics: one alert per danger episode, re-arm only
 * after the reactor returns to zero damage, no spam while it stays damaged.
 */
class DamageAlertStateTest {

    @Test
    void firesOnceWhenDamageAppears() {
        DamageAlertState s = new DamageAlertState();
        assertTrue(s.update(0.1));
        assertFalse(s.update(0.2));
        assertFalse(s.update(50));
    }

    @Test
    void staysQuietWhileDamageRisesAndFalls() {
        DamageAlertState s = new DamageAlertState();
        assertTrue(s.update(10));
        assertFalse(s.update(30));
        assertFalse(s.update(15));
        assertFalse(s.update(0.001));
    }

    @Test
    void reArmsAtZeroAndFiresAgainOnNextIncident() {
        DamageAlertState s = new DamageAlertState();
        assertTrue(s.update(5));
        assertFalse(s.update(5));
        assertFalse(s.update(0));   // recovered: re-arm, no alert
        assertTrue(s.update(2));    // new incident: one alert again
        assertFalse(s.update(3));
    }

    @Test
    void firstObservationOfDamagedReactorAlerts() {
        // e.g. server restart with a reactor saved mid-crisis, or a reactor
        // that formed while already damaged — one alert, not a storm.
        DamageAlertState s = new DamageAlertState();
        assertTrue(s.update(40));
        assertFalse(s.update(40));
    }

    @Test
    void healthyReactorNeverAlerts() {
        DamageAlertState s = new DamageAlertState();
        for (int i = 0; i < 1000; i++) {
            assertFalse(s.update(0));
        }
    }

    @Test
    void meltdownEdgeStillCountsAsDangerEpisode() {
        // Damage at/over MAX_DAMAGE (100) is the same episode: no refire,
        // and post-meltdown damage reset (a positive value) stays latched.
        DamageAlertState s = new DamageAlertState();
        assertTrue(s.update(100));
        assertFalse(s.update(100));
        assertFalse(s.update(60)); // postMeltdownDamage value is still >0
    }
}
