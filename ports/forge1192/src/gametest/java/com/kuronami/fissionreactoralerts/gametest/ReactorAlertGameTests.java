package com.kuronami.fissionreactoralerts.gametest;

import com.kuronami.fissionreactoralerts.FissionReactorAlerts;
import com.kuronami.fissionreactoralerts.alert.ReactorAlertTracker;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import mekanism.generators.common.content.fission.FissionReactorMultiblockData;
import mekanism.generators.common.tile.fission.TileEntityFissionReactorCasing;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * End-to-end check on a real headless server: builds a minimum-size Mekanism
 * fission reactor (3×3×4 casing shell, one fuel assembly + control rod column),
 * drives its live {@code reactorDamage} field, and asserts the tracker latches
 * exactly once per danger episode and re-arms at zero damage.
 *
 * <p>Assertions are per-reactor (via {@link ReactorAlertTracker#isReactorAlerted})
 * because GameTest batches run in parallel on one server — a global counter would
 * pick up the sibling test's alerts.
 */
@GameTestHolder(FissionReactorAlerts.MODID)
public class ReactorAlertGameTests {

    private static final String NS = "mekanismgenerators";
    private static final String MODE_PROPERTY = "fissionreactoralerts.gametest.mode";
    private static final List<String> CORE_MODS = List.of(
            FissionReactorAlerts.MODID, "mekanism", "mekanismgenerators");
    private static final List<String> FTB_MODS = List.of(
            "ftbchunks", "ftbteams", "ftblibrary", "architectury");

    private static void assertTrue(GameTestHelper helper, boolean condition, String message) {
        if (!condition) helper.fail(message);
    }

    private static void assertFalse(GameTestHelper helper, boolean condition, String message) {
        if (condition) helper.fail(message);
    }

    /** Verify the selected host shape before any test can pass on the wrong runtime. */
    private static boolean assertExpectedHost(GameTestHelper helper) {
        String mode = System.getProperty(MODE_PROPERTY);
        boolean fullMode;
        if ("full".equals(mode)) {
            fullMode = true;
        } else if ("without-ftb".equals(mode)) {
            fullMode = false;
        } else {
            helper.fail("unknown GameTest mode '" + mode + "'; expected full or without-ftb");
            return false;
        }

        for (String modId : CORE_MODS) {
            assertTrue(helper, ModList.get().isLoaded(modId),
                    "required mod missing in " + mode + " mode: " + modId);
        }
        for (String modId : FTB_MODS) {
            assertTrue(helper, ModList.get().isLoaded(modId) == fullMode,
                    "unexpected FTB host presence in " + mode + " mode: " + modId);
        }
        return fullMode;
    }

    private static Block mekBlock(String name) {
        Block b = Registry.BLOCK.get(ResourceLocation.tryParse(NS + ":" + name));
        if (b == null || b == Blocks.AIR) {
            throw new IllegalStateException("mekanismgenerators block missing: " + name);
        }
        return b;
    }

    /** 3×3×4 exterior (x,z = 0..2; y = 0..3), interior column = fuel + control rod. */
    private static void buildReactor(ServerLevel level, BlockPos base) {
        BlockState casing = mekBlock("fission_reactor_casing").defaultBlockState();
        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = 0; z < 3; z++) {
                    boolean shell = x == 0 || x == 2 || z == 0 || z == 2 || y == 0 || y == 3;
                    if (shell) {
                        level.setBlock(base.offset(x, y, z), casing, 3);
                    }
                }
            }
        }
        // Interior column at the centre: fuel assembly at y+1, control rod on top at y+2
        // (validator requires control rod directly above the highest fuel assembly).
        level.setBlock(base.offset(1, 1, 1), mekBlock("fission_fuel_assembly").defaultBlockState(), 3);
        level.setBlock(base.offset(1, 2, 1), mekBlock("control_rod_assembly").defaultBlockState(), 3);
    }

    private static FissionReactorMultiblockData reactorData(ServerLevel level, BlockPos base) {
        BlockEntity be = level.getBlockEntity(base);
        if (!(be instanceof TileEntityFissionReactorCasing casing)) {
            throw new IllegalStateException("no fission casing tile at " + base + " (got " + be + ")");
        }
        return casing.getMultiblock();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty5x5x5", timeoutTicks = 400)
    public static void damagedReactorAlertsOnceAndReArms(GameTestHelper helper) {
        assertExpectedHost(helper);
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        buildReactor(level, base);

        AtomicReference<FissionReactorMultiblockData> ref = new AtomicReference<>();

        helper.startSequence()
                // Formation can take a few ticks while the validator runs.
                .thenWaitUntil(() -> assertTrue(helper, 
                        reactorData(level, base).isFormed(), "reactor did not form"))
                .thenExecuteAfter(2, () -> {
                    ref.set(reactorData(level, base));
                    assertFalse(helper, ReactorAlertTracker.isReactorAlerted(ref.get(), level),
                            "freshly formed reactor already latched");
                    ref.get().reactorDamage = 5;   // danger episode begins
                })
                .thenExecuteAfter(5, () -> {
                    assertTrue(helper, ReactorAlertTracker.isReactorAlerted(ref.get(), level),
                            "damage onset did not latch the alert");
                    ref.get().reactorDamage = 0;   // recovered → re-arm
                })
                .thenExecuteAfter(5, () -> {
                    assertFalse(helper, ReactorAlertTracker.isReactorAlerted(ref.get(), level),
                            "alert stayed latched after damage returned to zero");
                    ref.get().reactorDamage = 9;   // second incident
                })
                .thenExecuteAfter(5, () -> assertTrue(helper, 
                        ReactorAlertTracker.isReactorAlerted(ref.get(), level),
                        "re-alert did not fire after recovery"))
                .thenSucceed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty5x5x5", timeoutTicks = 200)
    public static void healthyReactorNeverAlerts(GameTestHelper helper) {
        assertExpectedHost(helper);
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        buildReactor(level, base);

        AtomicReference<FissionReactorMultiblockData> ref = new AtomicReference<>();
        helper.startSequence()
                .thenWaitUntil(() -> assertTrue(helper, 
                        reactorData(level, base).isFormed(), "reactor did not form"))
                .thenExecuteAfter(2, () -> ref.set(reactorData(level, base)))
                .thenExecuteAfter(40, () -> assertFalse(helper, 
                        ReactorAlertTracker.isReactorAlerted(ref.get(), level),
                        "healthy reactor produced an alert"))
                .thenSucceed();
    }

    /**
     * Claim path, end to end: a fake player put through the real login gets an
     * FTB Teams player team, claims the reactor's chunks via the real FTB
     * Chunks API, then must receive the actual system-chat packet when the
     * reactor takes damage.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty5x5x5", timeoutTicks = 400)
    public static void claimedReactorAlertsOnlineTeamMember(GameTestHelper helper) {
        boolean fullMode = assertExpectedHost(helper);
        if (!fullMode) {
            FissionReactorAlerts.LOGGER.warn(
                    "INTENTIONAL GameTest skip: claimedReactorAlertsOnlineTeamMember is only "
                            + "applicable in full mode; without-ftb host absence was asserted");
            helper.succeed();
            return;
        }
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        buildReactor(level, base);
        FakeClient client = FakeClient.login(level, "fra-claim-owner");

        AtomicReference<FissionReactorMultiblockData> ref = new AtomicReference<>();
        helper.startSequence()
                .thenWaitUntil(() -> assertTrue(helper, 
                        reactorData(level, base).isFormed(), "reactor did not form"))
                .thenExecuteAfter(2, () -> {
                    ref.set(reactorData(level, base));
                    assertTrue(helper, client.player().getLevel() == level,
                            "fake player did not land in the test level");
                    FtbTestSupport.requirePlayerTeam(client.player());
                    FtbTestSupport.claimAllChunks(
                            client.player(), level, ref.get().getMinPos(), ref.get().getMaxPos());
                    client.drainSystemChat();
                    ref.get().reactorDamage = 5;
                })
                .thenExecuteAfter(10, () -> {
                    List<Component> got = client.drainSystemChat();
                    assertTrue(helper, got.stream().anyMatch(c ->
                                    c.getContents() instanceof TranslatableContents t
                                            && t.getKey().equals("message.fissionreactoralerts.damage_team")),
                            "claiming team's online member did not receive the alert; got: " + got);
                })
                .thenSucceed();
    }

    /**
     * Fallback path with a real recipient: the site is unclaimed, so the alert
     * must reach players within 64 blocks via an actual system-chat packet.
     */
    @PrefixGameTestTemplate(false)
    @GameTest(template = "empty5x5x5", timeoutTicks = 400)
    public static void unclaimedReactorAlertsNearbyPlayer(GameTestHelper helper) {
        assertExpectedHost(helper);
        ServerLevel level = helper.getLevel();
        BlockPos base = helper.absolutePos(new BlockPos(1, 1, 1));
        buildReactor(level, base);
        FakeClient client = FakeClient.login(level, "fra-nearby");

        AtomicReference<FissionReactorMultiblockData> ref = new AtomicReference<>();
        helper.startSequence()
                .thenWaitUntil(() -> assertTrue(helper, 
                        reactorData(level, base).isFormed(), "reactor did not form"))
                .thenExecuteAfter(2, () -> {
                    ref.set(reactorData(level, base));
                    assertTrue(helper, client.player().getLevel() == level,
                            "fake player did not land in the test level");
                    client.player().setPos(base.getX() + 1.5, base.getY() + 1, base.getZ() + 1.5);
                    client.drainSystemChat();
                    ref.get().reactorDamage = 5;
                })
                .thenExecuteAfter(10, () -> {
                    List<Component> got = client.drainSystemChat();
                    assertTrue(helper, got.stream().anyMatch(c ->
                                    c.getContents() instanceof TranslatableContents t
                                            && t.getKey().equals("message.fissionreactoralerts.damage")),
                            "nearby player did not receive the alert; got: " + got);
                })
                .thenSucceed();
    }
}
