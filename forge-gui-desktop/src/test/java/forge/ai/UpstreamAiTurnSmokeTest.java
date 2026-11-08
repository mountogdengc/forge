package forge.ai;

import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Random;

import static org.testng.Assert.*;

/** A bounded real turn shared with the pre-import assembly for a small, reproducible timing comparison. */
public class UpstreamAiTurnSmokeTest extends AITest {
    @DataProvider(name = "searchModes")
    public Object[][] searchModes() { return new Object[][]{{false}, {true}}; }

    @Test(dataProvider = "searchModes", timeOut = 90000)
    public void anOrdinaryCreatureTurnCompletesAndPlaysItsCards(boolean flatMc) {
        Random previous = MyRandom.getRandom();
        MyRandom.setRandom(new Random(20261107));
        try {
            Game game = initAndCreateGame();
            game.AI_FLAT_MONTE_CARLO = flatMc;
            game.AI_TIMEOUT = 2;
            Player ai = game.getPlayers().get(1);
            Player opponent = game.getPlayers().get(0);
            ai.setTeam(0);
            opponent.setTeam(1);
            AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
            brain.setUseSimulation(null);
            assertEquals(brain.flatMonteCarloActive(), flatMc, "The requested search mode must actually run");
            for (int i = 0; i < 20; i++) {
                addCardToZone("Forest", ai, ZoneType.Library);
                addCardToZone("Mountain", opponent, ZoneType.Library);
            }
            addCardToZone("Forest", ai, ZoneType.Hand);
            addCardToZone("Llanowar Elves", ai, ZoneType.Hand);
            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
            game.getAction().checkStateEffects(true);
            long start = System.nanoTime();
            int steps = 0;
            while (!game.isGameOver() && !game.getPhaseHandler().is(PhaseType.END_OF_TURN)
                    && game.getPhaseHandler().getPlayerTurn() == ai && steps < 300) {
                game.getPhaseHandler().mainLoopStep();
                steps++;
            }
            long millis = (System.nanoTime() - start) / 1_000_000;
            assertTrue(game.getPhaseHandler().is(PhaseType.END_OF_TURN), "The turn must finish within the step bound");
            assertEquals(countCardsWithName(game, "Forest", ZoneType.Battlefield), 1);
            assertEquals(countCardsWithName(game, "Llanowar Elves", ZoneType.Battlefield), 1);
            System.out.println("AI_TURN_PASS mode=" + (flatMc ? "flatmc" : "heuristic") + " steps=" + steps + " elapsed_ms=" + millis);
        } finally {
            MyRandom.setRandom(previous);
        }
    }
}
