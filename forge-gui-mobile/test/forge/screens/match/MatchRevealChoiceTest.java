package forge.screens.match;

import forge.Forge;
import forge.game.GameEntityView;
import forge.game.ability.effects.FlipOntoBattlefieldEffect;
import forge.game.card.CardView;
import forge.game.player.DelayedReveal;
import forge.game.zone.ZoneType;
import forge.gui.GuiBase;
import forge.gui.interfaces.IGuiBase;
import forge.util.Localizer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.testng.Assert.*;

/** Exercises the real mobile controller at its dialog boundary without a graphics context. */
@Test(singleThreaded = true)
public class MatchRevealChoiceTest {
    private IGuiBase oldGui;
    private Object oldLocalizer, oldForgeLocalizer;
    private boolean oldBoardDecidesHits;
    private final List<String> dialogs = new ArrayList<>();
    private List<?> revealed, offered;
    private int remainingMin, remainingMax;

    private static Field field(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    @BeforeMethod
    public void setUp() throws Exception {
        oldGui = GuiBase.getInterface();
        oldLocalizer = field(Localizer.class, "instance").get(null);
        oldForgeLocalizer = field(Forge.class, "localizer").get(null);
        oldBoardDecidesHits = FlipOntoBattlefieldEffect.boardDecidesHits;
        Path resources = Path.of("forge-gui").toAbsolutePath();
        if (!resources.toFile().isDirectory()) resources = Path.of("../forge-gui").toAbsolutePath();
        field(Localizer.class, "instance").set(null, null);
        Localizer.getInstance().initialize("en-US", resources.resolve("res/languages").toString());
        field(Forge.class, "localizer").set(null, Localizer.getInstance());
        dialogs.clear();
        revealed = offered = null;
        GuiBase.setInterface((IGuiBase) Proxy.newProxyInstance(IGuiBase.class.getClassLoader(),
                new Class<?>[]{IGuiBase.class}, (proxy, method, args) -> {
                    if (method.getName().equals("getChoices")) {
                        dialogs.add("reveal");
                        assertEquals(args[1], -1);
                        assertEquals(args[2], -1);
                        revealed = List.copyOf((List<?>) args[3]);
                        return List.of();
                    }
                    if (method.getName().equals("order")) {
                        dialogs.add("select");
                        remainingMin = (int) args[2];
                        remainingMax = (int) args[3];
                        offered = List.copyOf((List<?>) args[4]);
                        return List.of(offered.get(0));
                    }
                    throw new AssertionError("Unexpected GUI request: " + method.getName());
                }));
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        GuiBase.setInterface(oldGui);
        field(Localizer.class, "instance").set(null, oldLocalizer);
        field(Forge.class, "localizer").set(null, oldForgeLocalizer);
        FlipOntoBattlefieldEffect.boardDecidesHits = oldBoardDecidesHits;
    }

    @Test
    public void lookedAtCardsArePresentedBeforeChoosingOnlyEligibleCards() {
        CardView eligible = new CardView(1, null, "Eligible colorless card");
        CardView ineligible = new CardView(2, null, "Looked-at colored card");
        // The client consumes card views, just as it does after receiving a delayed reveal over the network.
        DelayedReveal delayed = new DelayedReveal(List.of(), Set.of(ZoneType.Library), null, "Looked at cards");
        delayed.getCards().add(eligible);
        delayed.getCards().add(ineligible);
        List<GameEntityView> options = List.of(eligible);
        List<GameEntityView> selected = MatchController.instance.chooseEntitiesForEffect("Choose a card", options, 0, 1, delayed);
        assertEquals(dialogs, List.of("reveal", "select"));
        assertEquals(revealed, List.of(eligible, ineligible));
        assertEquals(offered, options, "Looking at a card does not make it eligible to select");
        assertEquals(selected, List.of(eligible));
        assertEquals(remainingMin, 0);
        assertEquals(remainingMax, 1);
    }

    @Test
    public void ordinaryChoicesDoNotAddARevealDialog() {
        CardView eligible = new CardView(1, null, "Eligible card");
        assertEquals(MatchController.instance.chooseEntitiesForEffect("Choose", List.of(eligible), 1, 1, null), List.of(eligible));
        assertEquals(dialogs, List.of("select"));
        assertNull(revealed);
    }
}
