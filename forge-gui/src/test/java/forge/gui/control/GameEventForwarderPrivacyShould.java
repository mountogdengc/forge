package forge.gui.control;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import forge.game.GameLogEntryType;
import forge.game.event.GameEvent;
import forge.game.event.GameEventAddLog;
import forge.gui.interfaces.IGuiGame;

public class GameEventForwarderPrivacyShould {
    @Test
    public void privateLogDetailsNeverLeaveTheEngineEventBus() {
        List<GameEvent> delivered = new ArrayList<>();
        IGuiGame gui = (IGuiGame) Proxy.newProxyInstance(IGuiGame.class.getClassLoader(),
                new Class<?>[] { IGuiGame.class }, (proxy, method, args) -> {
                    if (method.getName().equals("handleGameEvents")) {
                        for (Object event : (List<?>) args[0]) {
                            delivered.add((GameEvent) event);
                        }
                    }
                    return null;
                });
        GameEventForwarder forwarder = new GameEventForwarder(gui);
        forwarder.receiveGameEvent(GameEventAddLog.postGameDetail(GameLogEntryType.INFORMATION,
                "Opponent draws Secret Card."));
        Assert.assertFalse(forwarder.hasPendingEvents());
        GameEvent publicEvent = new GameEventAddLog(GameLogEntryType.TURN, "Turn 1");
        forwarder.receiveGameEvent(publicEvent);
        forwarder.flush();
        Assert.assertEquals(delivered, List.of(publicEvent));
    }
}
