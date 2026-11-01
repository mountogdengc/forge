package forge.screens.match.views;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinFont;
import forge.assets.TextRenderer;
import forge.game.phase.PhaseType;
import forge.menu.FDropDown;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.toolbox.FOverlay;
import forge.util.Utils;

/** Match guidance shared by the menu and the controls it explains. */
public final class VMatchHelp {
    private VMatchHelp() { }

    static String text(String key, String fallback) {
        return Forge.getLocalizer().getMessageorUseDefault(key, fallback);
    }

    public static void show() {
        FOptionPane.showOptionDialog(text("lblMatchHelpChoose", "Choose a topic. You can also hover over match controls, or hold the prompt buttons, for a quick explanation."),
                text("lblMatchHelp", "Help: playing a match"), FOptionPane.INFORMATION_ICON,
                Arrays.asList(text("lblMatchHelpPhases", "Turn phases"), text("lblMatchHelpStops", "Phase stops"),
                        text("lblMatchHelpButtons", "OK and End Turn"), Forge.getLocalizer().getMessage("lblClose")), result -> {
                    if (result == 0) showPhases();
                    else if (result == 1) showStops();
                    else if (result == 2) showButtons();
                });
    }

    static void showPhases() {
        FOptionPane.showMessageDialog(text("lblMatchHelpPhasesBody",
                "A turn follows these steps:\n\n"
                + "Untap: ready your cards. UP (Upkeep): start-of-turn abilities. DR (Draw): draw a card.\n\n"
                + "M1 (Main 1): usually play your land, creatures and sorceries here.\n\n"
                + "BC: begin combat. DA: choose attackers. DB: choose blockers. FS / CD: first-strike / normal combat damage. EC: end combat.\n\n"
                + "M2 (Main 2): another chance to play cards after combat. ET (End Step): end-of-turn abilities. CL (Cleanup): discard down to your hand limit; damage wears off.\n\n"
                + "Instants and many abilities can be used on either player's turn when you get a chance to act. Untap and most cleanup steps do not offer that chance."),
                text("lblMatchHelpPhases", "Turn phases"));
    }

    static void showStops() {
        FOptionPane.showMessageDialog(text("lblMatchHelpStopsBody",
                "If the stops are folded away, tap the phase name to open them. Tap a phase cell to turn its stop on or off.\n\n"
                + "Use your own row for pauses during your turns; use an opponent's row for pauses during that opponent's turns. These settings control YOUR chances to act, not the opponent's decisions.\n\n"
                + "On: the cell is colored and has a line along the bottom. Off: the cell is dim. The current phase is highlighted separately, even if its stop is off. Colors depend on your theme.\n\n"
                + "A stop asks you to act when the stack is empty. Turning it off lets that phase pass automatically; you can still respond to spells and abilities. It does not skip the phase's rules.\n\n"
                + "The stack is the queue of spells and abilities waiting to resolve. For example, stop at Begin Combat to use an instant before attackers are chosen. Turn off pauses you rarely use to play faster.\n\n"
                + "Hold a phase cell to pass automatically until that player's selected phase. Orange arrows mark the destination. Hold it again to cancel. If that phase has already arrived, the marker waits for its next occurrence.\n\n"
                + "End Turn and other auto-pass settings can pass through enabled stops. Use Yield Settings in the menu to choose which events interrupt auto-pass."),
                text("lblMatchHelpStops", "Phase stops"));
    }

    static void showButtons() {
        FOptionPane.showMessageDialog(text("lblMatchHelpButtonsBody",
                "Read the message beside the buttons: their job depends on what the game is asking.\n\n"
                + "OK during normal play: pass this one chance to act. If a spell or ability is waiting, it may resolve after everyone passes. With nothing waiting, the game may advance to the next step. You can still act at later stops.\n\n"
                + "End Turn: automatically pass your remaining chances to act for the CURRENT turn, including enabled phase stops. On an opponent's turn it passes the rest of that opponent's turn. Required choices still appear. Events selected in Yield Settings can interrupt it.\n\n"
                + "While auto-passing, use the cancel-yield button to take control again.\n\n"
                + "In a choice or targeting prompt, OK confirms that choice instead. Cancel backs out when allowed. Undo reverses an available action; when shown as a separate arrow, End Turn keeps its usual meaning."),
                text("lblMatchHelpButtons", "OK and End Turn"));
    }

    static String phaseTip(PhaseType phase, String player, boolean stop) {
        return VUnifiedPhase.getPhaseName(phase) + " — " + player + "\n"
                + (stop ? text("lblMatchStopOnTip", "Stop ON: ask me to act here when the stack is empty.")
                        : text("lblMatchStopOffTip", "Stop OFF: pass automatically here when the stack is empty."))
                + "\n" + text("lblMatchStopGestureTip", "Tap to toggle. Hold to pass until this phase (or cancel its marker).");
    }

    /** A non-interactive hover popup using the match dropdown's wrapping and screen bounds. */
    static final class Tip extends FDropDown {
        private final FDisplayObject owner;
        private final Supplier<String> source;
        private final TextRenderer renderer = new TextRenderer(false);
        private final FSkinFont font = FSkinFont.get(13);
        private final float pad = Utils.scale(8);
        private Timer.Task pending;
        private String message;
        private Object screen;

        Tip(FDisplayObject owner0, Supplier<String> source0) {
            owner = owner0;
            source = source0;
        }

        void hover(boolean hovered) {
            if (!hovered) {
                if (pending != null) { pending.cancel(); pending = null; }
                if (isVisible()) { hide(); }
            } else if (pending == null && !isVisible()) {
                screen = Forge.getCurrentScreen();
                pending = Timer.schedule(new Timer.Task() {
                    @Override public void run() {
                        pending = null;
                        if (owner.isHovered() && owner.isVisible() && owner.isEnabled()
                                && screen == Forge.getCurrentScreen() && FOverlay.getTopOverlay() == null) {
                            message = source.get();
                            if (message != null && !message.isEmpty()) { show(); }
                        }
                    }
                }, 0.65f);
            }
        }

        boolean explain() {
            hover(false);
            FOptionPane.showMessageDialog(source.get(), text("lblMatchControlHelp", "Control help"));
            return true;
        }

        @Override public void show() {
            // Hide must remove from the same screen even if a dialog opens or
            // the match screen changes before the pointer leaves this control.
            setDropDownContainer(Forge.getCurrentScreen());
            super.show();
        }

        @Override protected boolean autoHide() { return false; }
        @Override protected FDisplayObject getDropDownOwner() { return owner; }
        @Override public void buildTouchListeners(float x, float y, List<FDisplayObject> listeners) { }

        @Override protected ScrollBounds updateAndGetPaneSize(float maxWidth, float maxHeight) {
            float width = Math.min(Utils.scale(340), maxWidth);
            return new ScrollBounds(width, renderer.getWrappedBounds(message, font, width - 2 * pad).height + 2 * pad);
        }

        @Override public void draw(Graphics g) {
            if (screen != Forge.getCurrentScreen() || !owner.isVisible() || !owner.isEnabled()
                    || !owner.isHovered() || FOverlay.getTopOverlay() != null || !message.equals(source.get())) {
                hide();
                return;
            }
            super.draw(g);
        }

        @Override public void drawBackground(Graphics g) {
            g.fillRect(VPrompt.GLASS_SOLID, 0, 0, getWidth(), getHeight());
            renderer.drawText(g, message, font, VPrompt.getForeColor(), pad, pad,
                    getWidth() - 2 * pad, getHeight() - 2 * pad, 0, getHeight(), true, Align.left, false);
        }
    }
}
