package forge.ai;

import forge.ai.simulation.ComboGuard;
import forge.ai.simulation.ComboPlan;
import forge.ai.simulation.AiCardLists;
import forge.ai.simulation.ParadoxOutcome;
import forge.ai.simulation.OathTiming;
import forge.card.CardAiHints;
import forge.card.CardRulesPredicates;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.CardType;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.effects.DelayedTriggerEffect;
import forge.game.card.Card;
import forge.game.card.CardFactory;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.FileSection;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.*;

/** Real card scripts and game objects; no installed catalog, model initialization or graphics. */
@Test(singleThreaded = true)
public class UpstreamAiPolicyShould {
    private static final class Fixture {
        final Path resources;
        final Game game;
        final Player ai;
        Fixture() throws Exception {
            resources = Files.isDirectory(Path.of("forge-gui/res")) ? Path.of("forge-gui/res") : Path.of("res");
            Localizer.getInstance().initialize("en-US", resources.resolve("languages").toString());
            Lang.createInstance("en-US");
            if (!CardType.Constant.LOADED.isSet()) {
                for (var section : FileSection.parseSections(Files.readAllLines(resources.resolve("lists/typeLists.txt"))).entrySet()) {
                    CardType.Helper.parseTypes(section.getKey(), section.getValue());
                }
                CardType.Constant.LOADED.set();
            }
            List<RegisteredPlayer> players = List.of(new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("AI", null)),
                    new RegisteredPlayer(new Deck()).setPlayer(new LobbyPlayerAi("Opponent", null)));
            GameRules rules = new GameRules(GameType.Constructed);
            game = new Game(players, rules, new Match(rules, players, "AI safety regression"));
            ai = game.getPlayers().get(0);
            ai.setTeam(0);
            game.getPlayers().get(1).setTeam(1);
            game.setAge(GameStage.Play);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
            game.getPhaseHandler().onStackResolved();
        }
        Card card(String fileName, ZoneType zone) throws Exception {
            Path script = resources.resolve("cardsfolder/" + fileName.charAt(0) + "/" + fileName + ".txt");
            Card card = CardFactory.getCard(new PaperCard(CardRules.fromScript(Files.readAllLines(script)),
                    "TST", CardRarity.Common), ai, game);
            card.setGameTimestamp(game.getNextTimestamp());
            ai.getZone(zone).add(card);
            return card;
        }
        void lands(String name, int count) throws Exception {
            for (int i = 0; i < count; i++) {
                Card land = card(name, ZoneType.Battlefield);
                assertFalse(land.getManaAbilities().isEmpty(), "Fixture land needs its intrinsic mana ability");
                for (SpellAbility mana : land.getManaAbilities()) {
                    mana.setActivatingPlayer(ai);
                    assertEquals(mana.amountOfManaGenerated(false), 1);
                    assertTrue(mana.canProduce(name.equals("island") ? "U" : name.equals("swamp") ? "B" : "R"));
                }
            }
        }
        SpellAbility pact() throws Exception {
            SpellAbility ability = card("pact_of_negation", ZoneType.Hand).getFirstSpellAbility();
            ability.setActivatingPlayer(ai);
            return ability;
        }
        SpellAbility secondPact(int duals, int wastes) throws Exception {
            for (int i = 0; i < duals; i++) card("breeding_pool", ZoneType.Battlefield);
            for (int i = 0; i < wastes; i++) card("wastes", ZoneType.Battlefield);
            game.getStack().getSpellsCastThisTurn().add(pact());
            SpellAbility second = card("summoners_pact", ZoneType.Hand).getFirstSpellAbility();
            second.setActivatingPlayer(ai);
            return second;
        }
        void outcomeBoard() throws Exception {
            lands("island", 4);
            lands("swamp", 1);
            for (int i = 0; i < 4; i++) card("memnite", ZoneType.Battlefield);
            for (int i = 0; i < 12; i++) card("forest", ZoneType.Library);
        }
        void resolvePactObligation(SpellAbility pact) {
            game.getStack().getSpellsCastThisTurn().add(pact);
            SpellAbility delayed = pact.getSubAbility();
            delayed.setActivatingPlayer(ai);
            new DelayedTriggerEffect().resolve(delayed);
        }
    }

    @Test
    public void anUnpayablePactIsRefusedBeforeItCanLoseTheGame() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 3);
        assertEquals(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY), "pact-unpayable");
    }

    @Test
    public void enoughGenericManaDoesNotReplaceThePactsRequiredColor() throws Exception {
        Fixture f = new Fixture();
        f.lands("mountain", 6);
        assertEquals(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY), "pact-unpayable");
    }

    @Test
    public void renewableManaOfTheRequiredColorsAllowsThePact() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 3);
        f.lands("swamp", 2);
        assertNull(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY));
    }

    @Test
    public void oneShotArtifactsDoNotPromiseNextUpkeepsPayment() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 3);
        f.card("lotus_petal", ZoneType.Battlefield);
        f.card("lions_eye_diamond", ZoneType.Battlefield);
        assertEquals(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY), "pact-unpayable");
    }

    @Test
    public void dualSourcesCannotPayTwoPactsColoredObligationsTwice() throws Exception {
        Fixture f = new Fixture();
        assertEquals(ComboGuard.problem(f.ai, f.secondPact(2, 7), null, ComboPlan.EMPTY), "pact-unpayable");
    }

    @Test
    public void enoughIndependentDualSourcesCanPayBothPacts() throws Exception {
        Fixture f = new Fixture();
        assertNull(ComboGuard.problem(f.ai, f.secondPact(4, 5), null, ComboPlan.EMPTY));
    }

    @Test
    public void emptyCounterArtifactsCannotPromisePactMana() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 2);
        for (int i = 0; i < 3; i++) f.card("pentad_prism", ZoneType.Battlefield);
        assertEquals(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY), "pact-unpayable");
    }

    @Test
    public void aTappedVaultCannotPromiseManaBeforeItsUntapPayment() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 2);
        f.card("mana_vault", ZoneType.Battlefield).setTapped(true);
        f.card("mana_vault", ZoneType.Battlefield).setTapped(true);
        f.card("mana_vault", ZoneType.Battlefield).setTapped(true);
        assertEquals(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY), "pact-unpayable");
    }

    @Test
    public void aLandWithAnUnmetActivationConditionCannotPromisePactMana() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 2);
        f.card("temple_of_the_false_god", ZoneType.Battlefield);
        f.card("wastes", ZoneType.Battlefield);
        assertEquals(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY), "pact-unpayable");
    }

    @Test
    public void pendingPactPaymentsSurviveTheTurnHistoryReset() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 5);
        f.resolvePactObligation(f.pact());
        f.game.getStack().onNextTurn();
        f.game.getPhaseHandler().devModeSet(PhaseType.MAIN2, f.game.getPlayers().get(1));
        SpellAbility second = f.pact();
        assertEquals(ComboGuard.problem(f.ai, second, null, ComboPlan.EMPTY), "pact-unpayable");
        f.game.getTriggerHandler().clearDelayedTrigger();
        assertNull(ComboGuard.problem(f.ai, second, null, ComboPlan.EMPTY), "Settled obligations cannot remain cached");
    }

    @Test
    public void aResolvedPactIsNotCountedTwiceInItsCastingTurn() throws Exception {
        Fixture f = new Fixture();
        f.lands("island", 10);
        f.resolvePactObligation(f.pact());
        assertNull(ComboGuard.problem(f.ai, f.pact(), null, ComboPlan.EMPTY));
    }

    @DataProvider(name = "unrelatedBounceSpells")
    public Object[][] unrelatedBounceSpells() { return new Object[][]{{"rescue"}, {"disentomb"}}; }

    @Test(dataProvider = "unrelatedBounceSpells")
    public void outcomeShortcutDoesNotHijackSingleTargetOrWrongZoneSpells(String name) throws Exception {
        Fixture f = new Fixture();
        f.outcomeBoard();
        SpellAbility spell = f.card(name, ZoneType.Hand).getFirstSpellAbility();
        spell.setActivatingPlayer(f.ai);
        assertNull(ParadoxOutcome.sureCast(f.ai, List.of(spell)), name);
        assertTrue(spell.getTargets().isEmpty(), "Rejecting the shortcut cannot leave illegal targets on " + name);
    }

    @Test
    public void outcomeShortcutRetainsARealLegalMultiTargetDraw() throws Exception {
        Fixture f = new Fixture();
        f.outcomeBoard();
        SpellAbility spell = f.card("paradoxical_outcome", ZoneType.Hand).getFirstSpellAbility();
        spell.setActivatingPlayer(f.ai);
        assertSame(ParadoxOutcome.sureCast(f.ai, List.of(spell)), spell);
        assertEquals(spell.getTargets().size(), 4);
        assertTrue(spell.isTargetNumberValid());
    }

    @Test
    public void deterministicShortcutsDoNotRetryASpellThatAlreadyFailedThisTurn() throws Exception {
        Fixture f = new Fixture();
        f.outcomeBoard();
        SpellAbility spell = f.card("paradoxical_outcome", ZoneType.Hand).getFirstSpellAbility();
        spell.setActivatingPlayer(f.ai);
        f.game.AI_FLAT_MONTE_CARLO = true;
        AiController brain = ((PlayerControllerAi) f.ai.getController()).getAi();
        brain.setUseSimulation(null);
        assertTrue(brain.flatMonteCarloActive(), "The shortcut's search mode must actually be enabled");
        brain.noteFailedCast(AiController.castKey(spell));
        List<SpellAbility> chosen = brain.chooseSpellAbilityToPlay();
        assertTrue(chosen == null || chosen.stream().noneMatch(sa -> sa.getHostCard() == spell.getHostCard()),
                "A shortcut cannot bypass failed-cast memory and stall the priority loop");
    }

    @Test
    public void oathTimingCannotReadTheUnrevealedOrderOfTheLibrary() throws Exception {
        Fixture f = new Fixture();
        Card creature = f.card("griselbrand", ZoneType.Library);
        for (int i = 0; i < 10; i++) f.card("forest", ZoneType.Library);
        var wants = OathTiming.class.getDeclaredMethod("wantsOath", Player.class);
        wants.setAccessible(true);
        boolean before = (boolean) wants.invoke(null, f.ai);
        f.ai.getZone(ZoneType.Library).remove(creature);
        f.ai.getZone(ZoneType.Library).add(creature);
        assertEquals((boolean) wants.invoke(null, f.ai), before,
                "Unrevealed top/bottom position cannot change an Oath decision for the same remaining deck");
    }

    @DataProvider(name = "selfMillRoutes")
    public Object[][] selfMillRoutes() { return new Object[][]{{true}, {false}}; }

    @Test(dataProvider = "selfMillRoutes")
    public void selfMillRecursionNeedsAReachableAffordableWinner(boolean winnerInHand) throws Exception {
        Fixture f = new Fixture();
        Map<Field, Object> oldState = new LinkedHashMap<>();
        for (String name : List.of("rolesFile", "comboFiles", "data", "warned")) {
            Field field = AiCardLists.class.getDeclaredField(name);
            field.setAccessible(true);
            oldState.put(field, field.get(null));
        }
        try {
            AiCardLists.configure(() -> {
                try { return Files.readAllLines(f.resources.resolve("lists/ai-card-roles.txt")); }
                catch (Exception error) { throw new AssertionError(error); }
            });
            for (int i = 0; i < 4; i++) {
                f.card("balustrade_spy", ZoneType.Library);
                f.card("dark_ritual", ZoneType.Library);
                f.card("lotus_petal", ZoneType.Library);
            }
            Card oracle = f.card("thassas_oracle", winnerInHand ? ZoneType.Hand : ZoneType.Library);
            if (winnerInHand) f.card("dread_return", ZoneType.Graveyard);
            else f.card("reanimate", ZoneType.Hand);
            f.card("memnite", ZoneType.Battlefield);
            f.card("memnite", ZoneType.Battlefield);
            f.lands("swamp", 4);
            SpellAbility spy = f.card("balustrade_spy", ZoneType.Hand).getFirstSpellAbility();
            spy.setActivatingPlayer(f.ai);
            ComboPlan plan = ComboPlan.scan(f.ai);
            assertTrue(plan.has(ComboPlan.Kind.LIBRARY_OUT), plan.summary());
            assertEquals(ComboGuard.problem(f.ai, spy, null, plan), "self-mill-no-winner",
                    "The winning follow-up must be reachable and payable after the Spy");
            if (winnerInHand) {
                f.ai.getZone(ZoneType.Hand).remove(oracle);
                f.ai.getZone(ZoneType.Graveyard).add(oracle);
            } else {
                f.lands("swamp", 1);
            }
            assertNull(ComboGuard.problem(f.ai, spy, null, plan), "The now-payable winning follow-up is safe");
        } finally {
            for (var entry : oldState.entrySet()) entry.getKey().set(null, entry.getValue());
        }
    }

    @Test
    public void roleLookupNormalizesAndReloadsWithoutAdmittingObservationOnlyNames() throws Exception {
        Map<Field, Object> oldState = new LinkedHashMap<>();
        for (String name : List.of("rolesFile", "comboFiles", "data", "warned")) {
            Field field = AiCardLists.class.getDeclaredField(name);
            field.setAccessible(true);
            oldState.put(field, field.get(null));
        }
        try {
            AtomicReference<List<String>> roles = new AtomicReference<>(List.of(
                    "storm_payoff | Plain ASCII | Éomer,  Marshal",
                    "anti_combo_seen | Observed Only"));
            AiCardLists.configure(roles::get);
            assertFalse(AiCardLists.isFlatMcPlayable(null));
            assertTrue(AiCardLists.isFlatMcPlayable("PLAIN ASCII"));
            assertTrue(AiCardLists.isFlatMcPlayable("  eomer,\tmarshal  "));
            assertFalse(AiCardLists.isFlatMcPlayable("Observed Only"));
            assertFalse(AiCardLists.isFlatMcPlayable("Unlisted"));
            assertThrows(UnsupportedOperationException.class,
                    () -> AiCardLists.names(AiCardLists.Role.STORM_PAYOFF).add("mutation"));
            roles.set(List.of("storm_payoff | Replacement"));
            assertFalse(AiCardLists.isFlatMcPlayable("Replacement"), "Loaded role data remains a snapshot until reloaded");
            AiCardLists.reload();
            assertTrue(AiCardLists.isFlatMcPlayable("Replacement"));
            assertFalse(AiCardLists.isFlatMcPlayable("Plain ASCII"));
        } finally {
            for (var entry : oldState.entrySet()) entry.getKey().set(null, entry.getValue());
        }
    }

    @Test
    public void conditionalDeckEligibilityDefaultsToStrictAndKeepsCommanderRestrictions() throws Exception {
        Field predicate = CardAiHints.class.getDeclaredField("keptForFlatMc");
        predicate.setAccessible(true);
        Object previous = predicate.get(null);
        CardRules supported = CardRules.fromScript(List.of("Name:Supported", "ManaCost:1", "Types:Sorcery",
                "Oracle:Fixture.", "AI:RemoveDeck:All", "AI:RemoveDeck:Random", "AI:RemoveDeck:NonCommander"));
        CardRules unsupported = CardRules.fromScript(List.of("Name:Unsupported", "ManaCost:1", "Types:Sorcery",
                "Oracle:Fixture.", "AI:RemoveDeck:All"));
        try {
            CardAiHints.setKeptForFlatMc(null);
            assertTrue(CardAiHints.isRemovedFromAiDecks(supported));
            assertTrue(CardAiHints.isRemovedFromRandomDecks(supported));
            AtomicBoolean enabled = new AtomicBoolean(true);
            CardAiHints.setKeptForFlatMc(name -> enabled.get() && name.equals("Supported"));
            assertTrue(CardRulesPredicates.IS_KEPT_IN_AI_DECKS.test(supported));
            assertTrue(CardRulesPredicates.IS_KEPT_IN_RANDOM_DECKS.test(supported));
            assertFalse(CardRulesPredicates.IS_KEPT_IN_AI_LIMITED_DECKS.test(supported));
            assertTrue(CardAiHints.isRemovedFromAiDecks(unsupported));
            enabled.set(false);
            assertTrue(CardAiHints.isRemovedFromAiDecks(supported));
        } finally {
            predicate.set(null, previous);
        }
    }
}
