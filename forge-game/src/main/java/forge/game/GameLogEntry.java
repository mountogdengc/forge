package forge.game;

import java.io.Serializable;

import forge.game.card.CardView;

public record GameLogEntry(GameLogEntryType type, String message, CardView sourceCard, boolean postGameOnly) implements Serializable {
    public GameLogEntry {
        // Private history must never retain a live card link that a UI could expose.
        if (postGameOnly) {
            sourceCard = null;
        }
    }

    public GameLogEntry(final GameLogEntryType type, final String message, final CardView sourceCard) {
        this(type, message, sourceCard, false);
    }

    GameLogEntry(final GameLogEntryType type, final String message) {
        this(type, message, null);
    }

    @Override
    public String toString() {
        return type.getCaption() + ": " + message;
    }
}
