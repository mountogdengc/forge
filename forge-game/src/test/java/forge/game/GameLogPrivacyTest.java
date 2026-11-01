package forge.game;

import java.util.EnumSet;
import java.nio.file.Files;
import java.nio.file.Path;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.game.card.CardView;
import forge.game.event.GameEventAddLog;
import forge.game.event.GameEventCardChangeZone;
import forge.game.zone.ZoneType;
import forge.game.zone.ZoneView;
import forge.util.Localizer;

public class GameLogPrivacyTest {
    @Test
    public void privateDetailsNeverAppearInOrdinaryLogQueries() {
        GameLog log = new GameLog();
        log.add(GameLogEntryType.TURN, "Turn 1");
        CardView hiddenCard = new CardView(-1, null, "Secret Card");
        log.add(new GameLogEntry(GameLogEntryType.DISCARD, "Opponent discards Secret Card.", hiddenCard, true));

        Assert.assertEquals(log.getAllEntries().size(), 1);
        Assert.assertEquals(log.getLogEntries(null).size(), 1);
        Assert.assertTrue(log.getLogEntriesExact(GameLogEntryType.DISCARD).isEmpty());
        Assert.assertEquals(log.getLogEntriesExact(null).size(), 1);
        Assert.assertEquals(log.getLogEntriesForVerbosity(GameLogVerbosity.HIGH).size(), 1);
        Assert.assertEquals(log.getLogEntriesForTypes(EnumSet.allOf(GameLogEntryType.class)).size(), 1);
        Assert.assertEquals(log.getPostGameEntries().get(0).message(), "Opponent discards Secret Card.");
        Assert.assertNull(log.getPostGameEntries().get(0).sourceCard());
        Assert.assertEquals(log.size(), 2);

        log.truncateTo(1);
        Assert.assertEquals(log.getPostGameEntries().size(), 1);
    }

    @Test
    public void namedActionDetailsAndZoneChangesStayOutOfLiveHistory() {
        Path languages = Path.of("forge-gui", "res", "languages");
        if (!Files.isDirectory(languages)) {
            languages = Path.of("..", "forge-gui", "res", "languages");
        }
        Localizer.getInstance().initialize("en-US", languages.toString());
        GameLog log = new GameLog();
        CardView card = new CardView(-1, null, "Secret Card");
        GameLogFormatter formatter = log.getEventVisitor();
        formatter.recieve(GameEventAddLog.postGameDetail(GameLogEntryType.INFORMATION,
                "Opponent draws Secret Card."));
        formatter.recieve(new GameEventCardChangeZone(card,
                new ZoneView(null, ZoneType.Library), new ZoneView(null, ZoneType.Graveyard)));

        Assert.assertTrue(log.getAllEntries().isEmpty());
        Assert.assertEquals(log.getPostGameEntries().size(), 2);
        for (GameLogEntry entry : log.getPostGameEntries()) {
            Assert.assertTrue(entry.postGameOnly());
            Assert.assertNull(entry.sourceCard());
        }
        // A tutor is a zone move, not a draw, and must still be recorded.
        Assert.assertNotNull(formatter.visit(new GameEventCardChangeZone(card,
                new ZoneView(null, ZoneType.Library), new ZoneView(null, ZoneType.Hand))));
        Assert.assertNotNull(formatter.visit(new GameEventCardChangeZone(card,
                new ZoneView(null, ZoneType.Hand), new ZoneView(null, ZoneType.Battlefield))));
        // An actual draw/discard gets a dedicated action entry instead of duplicates.
        Assert.assertNull(formatter.visit(new GameEventCardChangeZone(card,
                new ZoneView(null, ZoneType.Library), new ZoneView(null, ZoneType.Hand), true)));
        Assert.assertNull(formatter.visit(new GameEventCardChangeZone(card,
                new ZoneView(null, ZoneType.Hand), new ZoneView(null, ZoneType.Graveyard), true)));
        // Existing public battlefield-exit events retain their previous visibility.
        Assert.assertFalse(formatter.visit(new GameEventCardChangeZone(card,
                new ZoneView(null, ZoneType.Battlefield), new ZoneView(null, ZoneType.Graveyard))).postGameOnly());
    }
}
