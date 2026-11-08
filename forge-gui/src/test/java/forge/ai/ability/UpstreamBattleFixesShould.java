package forge.ai.ability;

import com.google.common.eventbus.Subscribe;
import forge.ai.AiController;
import forge.ai.ComputerUtil;
import forge.ai.LobbyPlayerAi;
import forge.ai.PlayerControllerAi;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.effects.DigEffect;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactory;
import forge.game.card.CardView;
import forge.game.cost.Cost;
import forge.game.event.GameEventSpellResolved;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerView;
import forge.game.player.RegisteredPlayer;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.item.PaperCard;
import forge.util.Lang;
import forge.util.Localizer;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.testng.Assert.*;

/** Inline cards and real controllers exercise shared battles without loading the installed card catalog or a UI. */
@Test(singleThreaded = true)
public class UpstreamBattleFixesShould {
    private static final class RecordingAi extends PlayerControllerAi {
        final List<String> revealed = new ArrayList<>();
        RecordingAi(Game game, Player player, LobbyPlayerAi lobby) { super(game, player, lobby); }
        @Override public void reveal(List<CardView> cards, ZoneType zone, PlayerView owner, String message, boolean suffix) {
            for (CardView card : cards) revealed.add(card.getName());
        }
        @Override public void reveal(CardCollectionView cards, ZoneType zone, Player owner, String message, boolean suffix) {
            for (Card card : cards) revealed.add(card.getName());
        }
    }

    private static final class Fixture {
        final Game game;
        final Player ai, opponent;
        Fixture() {
            Path languages = Path.of("forge-gui/res/languages");
            if (!Files.isDirectory(languages)) languages = Path.of("res/languages");
            Localizer.getInstance().initialize("en-US", languages.toString());
            Lang.createInstance("en-US");
            List<RegisteredPlayer> players = List.of(new RegisteredPlayer(new Deck()).setPlayer(lobby("AI")),
                    new RegisteredPlayer(new Deck()).setPlayer(lobby("Opponent")));
            GameRules rules = new GameRules(GameType.Constructed);
            game = new Game(players, rules, new Match(rules, players, "Battle regression"));
            ai = game.getPlayers().get(0);
            opponent = game.getPlayers().get(1);
            ai.setTeam(0);
            opponent.setTeam(1);
            game.setAge(GameStage.Play);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
            game.getPhaseHandler().onStackResolved();
        }
        private static LobbyPlayerAi lobby(String name) {
            return new LobbyPlayerAi(name, null) {
                @Override public Player createIngamePlayer(Game game, int id) {
                    Player player = new Player(getName(), game, id);
                    player.setFirstController(new RecordingAi(game, player, this));
                    return player;
                }
            };
        }
        Card card(Player owner, ZoneType zone, String name, String mana, String... lines) {
            List<String> script = new ArrayList<>(List.of("Name:" + name, "ManaCost:" + mana));
            script.add("Oracle:Inline regression fixture.");
            script.addAll(List.of(lines));
            Card card = CardFactory.getCard(new PaperCard(CardRules.fromScript(script), "TST", CardRarity.Common), owner, game);
            card.setGameTimestamp(game.getNextTimestamp());
            if (zone == ZoneType.Stack) game.getStackZone().add(card);
            else owner.getZone(zone).add(card);
            return card;
        }
        SpellAbility ability(Card card, String script) {
            SpellAbility ability = AbilityFactory.getAbility(script, card);
            ability.setActivatingPlayer(card.getController());
            return ability;
        }
        RecordingAi controller(Player player) { return (RecordingAi) player.getController(); }
        void mana(int count) {
            for (int i = 0; i < count; i++) {
                card(ai, ZoneType.Battlefield, "Mana source " + i, "0", "Types:Land",
                        "A:AB$ Mana | Cost$ T | Produced$ U | SpellDescription$ Add blue mana.");
            }
        }
    }

    @Test
    public void aManaTaxRetainsTheOriginalWaterbendingLimit() {
        Cost waterbend = new Cost("Waterbend<3>", true);
        assertEquals(waterbend.getMaxWaterbend(), "3");
        waterbend.add(new Cost("2", true));
        assertEquals(waterbend.getTotalMana().getCMC(), 5);
        assertEquals(waterbend.getMaxWaterbend(), "3", "A mana tax cannot erase permission to tap for waterbending");
    }

    @Test
    public void anIncomingWaterbendingLimitTakesPrecedenceAndOrdinaryManaStaysOrdinary() {
        Cost cost = new Cost("Waterbend<3>", true);
        cost.add(new Cost("Waterbend<2>", true));
        assertEquals(cost.getMaxWaterbend(), "2");
        Cost ordinary = new Cost("3", true).add(new Cost("2", true));
        assertNull(ordinary.getMaxWaterbend());
    }

    @Test
    public void revealedSideboardChoicesDoNotRequireADestination() {
        Fixture f = new Fixture();
        Card host = f.card(f.ai, ZoneType.Stack, "Wish fixture", "1 B", "Types:Sorcery");
        SpellAbility wish = f.ability(host, "SP$ ChangeZone | Origin$ Sideboard | ChangeNum$ 2 | Hidden$ True | Reveal$ True");
        Card weak = f.card(f.ai, ZoneType.Sideboard, "Small creature", "1", "Types:Creature Beast", "PT:1/1");
        Card strong = f.card(f.ai, ZoneType.Sideboard, "Large creature", "6", "Types:Creature Beast", "PT:7/7");
        CardCollection choices = new CardCollection(List.of(weak, strong));
        AiController ai = f.controller(f.ai).getAi();
        assertSame(ai.chooseCardToHiddenOriginChangeZone(null, List.of(ZoneType.Sideboard), wish, choices, f.ai, f.ai), strong);
        assertSame(ChangeZoneAi.chooseCardToHiddenOriginChangeZone(null, List.of(ZoneType.Sideboard), wish,
                choices, f.ai, f.ai), strong);
        assertNull(ai.chooseCardToHiddenOriginChangeZone(null, List.of(ZoneType.Sideboard), wish,
                new CardCollection(), f.ai, f.ai));
    }

    @Test
    public void anOpponentHandsOverTheWorseRevealedSideboardCard() {
        Fixture f = new Fixture();
        Card host = f.card(f.ai, ZoneType.Stack, "Wish fixture", "1 B", "Types:Sorcery");
        SpellAbility wish = f.ability(host, "DB$ ChangeZone | Origin$ Sideboard | Destination$ Hand | Hidden$ True");
        Card weak = f.card(f.ai, ZoneType.Sideboard, "Small creature", "1", "Types:Creature Beast", "PT:1/1");
        Card strong = f.card(f.ai, ZoneType.Sideboard, "Large creature", "6", "Types:Creature Beast", "PT:7/7");
        CardCollection choices = new CardCollection(List.of(weak, strong));
        assertSame(ChangeZoneAi.chooseCardToHiddenOriginChangeZone(ZoneType.Hand, List.of(ZoneType.Sideboard),
                wish, choices, f.ai, f.opponent), weak);
    }

    @Test
    public void optionalCopiesUseSpareManaWithoutSacrificingTheNextHandSpell() {
        Fixture f = new Fixture();
        f.mana(4);
        Card source = f.card(f.ai, ZoneType.Battlefield, "Draw source", "2", "Types:Artifact");
        Card rings = f.card(f.ai, ZoneType.Battlefield, "Copy source", "3", "Types:Artifact");
        SpellAbility original = f.ability(source, "AB$ Draw | Cost$ 2 | Defined$ You | NumCards$ 1");
        SpellAbility copy = f.ability(rings, "AB$ CopySpellAbility | Cost$ 2 | Defined$ TriggeredSpellAbility | MayChooseTarget$ True");
        copy.setTriggeringObject(AbilityKey.SpellAbility, original);
        f.card(f.ai, ZoneType.Hand, "Affordable spell", "2", "Types:Sorcery");
        CopySpellAbilityAi logic = new CopySpellAbilityAi();
        assertTrue(logic.doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay(),
                "Four mana covers both the optional copy and the two-mana spell in hand");
        f.card(f.ai, ZoneType.Hand, "Another affordable spell", "2", "Types:Sorcery");
        assertFalse(logic.doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay(),
                "Copying must not reduce the number of otherwise affordable hand spells");
        copy.setTriggeringObject(AbilityKey.SpellAbility,
                f.ability(f.card(f.opponent, ZoneType.Battlefield, "Their source", "2", "Types:Artifact"),
                        "AB$ Draw | Cost$ 0 | Defined$ You | NumCards$ 1"));
        assertFalse(logic.doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay(), "This trigger only copies our own abilities");
    }

    @Test
    public void optionalCopiesDoNotRedirectOurRemovalOntoIvy() throws Exception {
        Fixture f = new Fixture();
        Path script = Path.of("forge-gui/res/cardsfolder/i/ivy_gleeful_spellthief.txt");
        if (!Files.isRegularFile(script)) script = Path.of("res/cardsfolder/i/ivy_gleeful_spellthief.txt");
        Card ivy = CardFactory.getCard(new PaperCard(CardRules.fromScript(Files.readAllLines(script)),
                "TST", CardRarity.Rare), f.ai, f.game);
        f.ai.getZone(ZoneType.Battlefield).add(ivy);
        Card victim = f.card(f.opponent, ZoneType.Battlefield, "Their creature", "3", "Types:Creature Beast", "PT:4/4");
        SpellAbility murder = f.ability(f.card(f.ai, ZoneType.Stack, "Murder", "1 B B", "Types:Instant"),
                "SP$ Destroy | Cost$ 1 B B | ValidTgts$ Creature");
        murder.getTargets().add(victim);
        SpellAbility copy = f.ability(ivy, ivy.getSVar("TrigCopyTarget"));
        copy.setTriggeringObject(AbilityKey.SpellAbility, murder);
        assertFalse(new CopySpellAbilityAi().doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay(),
                "Owning the original does not make its forced target on Ivy beneficial");
    }

    @Test
    public void aHarmlessFreeSubabilityCopyRemainsAvailable() {
        Fixture f = new Fixture();
        Card source = f.card(f.ai, ZoneType.Battlefield, "Free copy source", "2", "Types:Artifact");
        SpellAbility copy = f.ability(source, "DB$ CopySpellAbility | Defined$ TriggeredSpellAbility");
        copy.setTriggeringObject(AbilityKey.SpellAbility,
                f.ability(source, "AB$ Draw | Cost$ 0 | Defined$ You | NumCards$ 1"));
        assertTrue(new CopySpellAbilityAi().doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay());
    }

    @Test
    public void optionalCopiesRejectManaAbilitiesCopyLoopsAndCardsExcludedFromAiDecks() {
        Fixture f = new Fixture();
        f.mana(4);
        Card source = f.card(f.ai, ZoneType.Battlefield, "Source fixture", "2", "Types:Artifact");
        Card rings = f.card(f.ai, ZoneType.Battlefield, "Copy source", "3", "Types:Artifact");
        SpellAbility copy = f.ability(rings, "AB$ CopySpellAbility | Cost$ 2 | Defined$ TriggeredSpellAbility");
        CopySpellAbilityAi logic = new CopySpellAbilityAi();
        for (String script : List.of("AB$ Mana | Cost$ T | Produced$ U",
                "AB$ CopySpellAbility | Cost$ 0 | Defined$ TriggeredSpellAbility")) {
            copy.setTriggeringObject(AbilityKey.SpellAbility, f.ability(source, script));
            assertFalse(logic.doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay(), script);
        }
        Card unsuitable = f.card(f.ai, ZoneType.Battlefield, "Unsupported AI source", "2",
                "Types:Artifact", "AI:RemoveDeck:All");
        copy.setTriggeringObject(AbilityKey.SpellAbility,
                f.ability(unsuitable, "AB$ Draw | Cost$ 0 | Defined$ You | NumCards$ 1"));
        assertFalse(logic.doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay());
    }

    @Test
    public void copyBudgetOnAnOpponentsTurnReservesOnlyInstantSpeedHandPlays() {
        Fixture f = new Fixture();
        f.mana(2);
        Card source = f.card(f.ai, ZoneType.Battlefield, "Draw source", "2", "Types:Artifact");
        SpellAbility copy = f.ability(source, "AB$ CopySpellAbility | Cost$ 2 | Defined$ TriggeredSpellAbility");
        copy.setTriggeringObject(AbilityKey.SpellAbility,
                f.ability(source, "AB$ Draw | Cost$ 0 | Defined$ You | NumCards$ 1"));
        f.card(f.ai, ZoneType.Hand, "Sorcery in hand", "2", "Types:Sorcery");
        f.game.getPhaseHandler().devModeSet(PhaseType.END_OF_TURN, f.opponent);
        CopySpellAbilityAi logic = new CopySpellAbilityAi();
        assertTrue(logic.doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay());
        f.card(f.ai, ZoneType.Hand, "Instant in hand", "2", "Types:Instant");
        assertFalse(logic.doTriggerNoCostWithSubs(f.ai, copy, false).willingToPlay());
    }

    @Test
    public void copiedRemovalPredictsItsOwnTargetsInsteadOfTheOriginalTargets() {
        Fixture f = new Fixture();
        Card first = f.card(f.ai, ZoneType.Battlefield, "First victim", "1", "Types:Creature Beast", "PT:1/1");
        Card second = f.card(f.ai, ZoneType.Battlefield, "Second victim", "1", "Types:Creature Beast", "PT:1/1");
        Card source = f.card(f.opponent, ZoneType.Battlefield, "Removal source", "3", "Types:Artifact");
        SpellAbility original = f.ability(source, "AB$ Destroy | Cost$ 0 | ValidTgts$ Creature");
        original.getTargets().add(first);
        SpellAbility copy = original.copy(f.opponent);
        copy.resetTargets();
        copy.getTargets().add(second);
        f.game.copyLastState();
        f.game.getStack().add(copy);
        var threatened = ComputerUtil.predictThreatenedObjects(f.ai, null, true);
        assertTrue(threatened.contains(second), "The copied activation already threatens the second creature");
        assertFalse(threatened.contains(first), "Original targets must not replace the copy's targets");
    }

    public static final class ResolutionObserver {
        SpellAbility seen;
        final Game game;
        ResolutionObserver(Game game) { this.game = game; }
        @Subscribe public void resolved(GameEventSpellResolved event) { seen = resolvingAbility(game); }
    }

    private static SpellAbility resolvingAbility(Game game) {
        try { return (SpellAbility) game.getStack().getClass().getMethod("getResolvingAbility").invoke(game.getStack()); }
        catch (NoSuchMethodException beforeImport) { return null; }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }

    @Test
    public void theResolvingAbilityIsVisibleDuringResolutionAndClearedAfterward() {
        Fixture f = new Fixture();
        Card source = f.card(f.ai, ZoneType.Battlefield, "No-op source", "1", "Types:Artifact");
        SpellAbility ability = f.ability(source, "AB$ Draw | Cost$ 0 | Defined$ You | NumCards$ 0");
        ResolutionObserver observer = new ResolutionObserver(f.game);
        f.game.subscribeToEvents(observer);
        f.game.getStack().add(ability);
        SpellAbility stacked = f.game.getStack().peekAbility();
        f.game.getStack().resolveStack();
        assertSame(observer.seen, stacked);
        assertNull(resolvingAbility(f.game));
    }

    @DataProvider(name = "graveyardKeywords")
    public Object[][] graveyardKeywords() { return new Object[][]{{"Beam me up:2 U"}, {"Harmonize:3 G"}, {"Mayhem:1 R"}}; }

    @Test(dataProvider = "graveyardKeywords")
    public void graveyardAlternativeCastingActionsRemainVisible(String keyword) {
        Fixture f = new Fixture();
        Card card = f.card(f.ai, ZoneType.Graveyard, "Return spell", "3", "Types:Sorcery", "K:" + keyword);
        card.setDiscarded(true);
        card.setTurnInZone(f.game.getPhaseHandler().getTurn());
        assertTrue(StreamSupport.stream(f.ai.getZone(ZoneType.Graveyard).getCardsPlayerCanActivate(f.ai).spliterator(), false)
                .anyMatch(candidate -> candidate == card), keyword);
        assertFalse(StreamSupport.stream(f.ai.getZone(ZoneType.Graveyard).getCardsPlayerCanActivate(f.opponent).spliterator(), false)
                .anyMatch(candidate -> candidate == card), "The opponent gains no casting permission");
    }

    @Test
    public void mayhemRequiresADiscardThisTurnAndPlottedCardsStayAvailableInExile() {
        Fixture f = new Fixture();
        Card card = f.card(f.ai, ZoneType.Graveyard, "Mayhem spell", "3", "Types:Sorcery", "K:Mayhem:1 R");
        card.setTurnInZone(f.game.getPhaseHandler().getTurn());
        assertFalse(StreamSupport.stream(f.ai.getZone(ZoneType.Graveyard).getCardsPlayerCanActivate(f.ai).spliterator(), false)
                .anyMatch(candidate -> candidate == card));
        card.setDiscarded(true);
        card.setTurnInZone(f.game.getPhaseHandler().getTurn() - 1);
        assertFalse(StreamSupport.stream(f.ai.getZone(ZoneType.Graveyard).getCardsPlayerCanActivate(f.ai).spliterator(), false)
                .anyMatch(candidate -> candidate == card));
        Card plotted = f.card(f.ai, ZoneType.Exile, "Plotted spell", "2", "Types:Sorcery");
        assertFalse(StreamSupport.stream(f.ai.getZone(ZoneType.Exile).getCardsPlayerCanActivate(f.ai).spliterator(), false)
                .anyMatch(candidate -> candidate == plotted));
        plotted.setPlotted(true);
        assertTrue(StreamSupport.stream(f.ai.getZone(ZoneType.Exile).getCardsPlayerCanActivate(f.ai).spliterator(), false)
                .anyMatch(candidate -> candidate == plotted));
        assertFalse(StreamSupport.stream(f.ai.getZone(ZoneType.Exile).getCardsPlayerCanActivate(f.opponent).spliterator(), false)
                .anyMatch(candidate -> candidate == plotted));
    }

    @Test
    public void aDigWithNoEligibleCardsStillShowsOnlyItsChooserTheLookedAtCards() {
        Fixture f = new Fixture();
        Card first = f.card(f.ai, ZoneType.Library, "Private red card", "R", "Types:Creature Beast", "PT:1/1");
        Card second = f.card(f.ai, ZoneType.Library, "Private blue card", "U", "Types:Creature Beast", "PT:1/1");
        Card host = f.card(f.ai, ZoneType.Stack, "Look fixture", "G", "Types:Sorcery");
        SpellAbility dig = f.ability(host, "SP$ Dig | Defined$ You | DigNum$ 2 | ChangeNum$ 1 | ChangeValid$ Card.Colorless | Optional$ True");
        f.game.copyLastState();
        new DigEffect().resolve(dig);
        assertTrue(f.controller(f.ai).revealed.containsAll(List.of(first.getName(), second.getName())),
                "A no-match result must still show what was looked at");
        assertTrue(f.controller(f.opponent).revealed.isEmpty(), "Unchosen private cards cannot leak to the opponent");
        assertEquals(f.ai.getCardsIn(ZoneType.Library).size(), 2);
    }

    @Test
    public void theMigratedAncientStirringsScriptStillRevealsOnlyToItsChooser() throws Exception {
        Fixture f = new Fixture();
        f.card(f.ai, ZoneType.Library, "Private red card", "R", "Types:Creature Beast", "PT:1/1");
        Path script = Path.of("forge-gui/res/cardsfolder/a/ancient_stirrings.txt");
        if (!Files.isRegularFile(script)) script = Path.of("res/cardsfolder/a/ancient_stirrings.txt");
        Card host = CardFactory.getCard(new PaperCard(CardRules.fromScript(Files.readAllLines(script)),
                "TST", CardRarity.Common), f.ai, f.game);
        f.game.getStackZone().add(host);
        SpellAbility dig = host.getFirstSpellAbility();
        dig.setActivatingPlayer(f.ai);
        f.game.copyLastState();
        new DigEffect().resolve(dig);
        assertEquals(f.controller(f.ai).revealed, List.of("Private red card"));
        assertTrue(f.controller(f.opponent).revealed.isEmpty());
    }

    @Test
    public void noLookingDoesNotExposeTheCardsWhenNothingQualifies() {
        Fixture f = new Fixture();
        f.card(f.ai, ZoneType.Library, "Unseen red card", "R", "Types:Creature Beast", "PT:1/1");
        Card host = f.card(f.ai, ZoneType.Stack, "Blind dig fixture", "G", "Types:Sorcery");
        SpellAbility dig = f.ability(host, "SP$ Dig | Defined$ You | DigNum$ 1 | ChangeNum$ 1 | ChangeValid$ Card.Colorless | NoLooking$ True");
        f.game.copyLastState();
        new DigEffect().resolve(dig);
        assertTrue(f.controller(f.ai).revealed.isEmpty());
        assertTrue(f.controller(f.opponent).revealed.isEmpty());
    }
}
