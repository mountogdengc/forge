package forge.ai.simulation;

import java.util.Arrays;

import forge.ai.ComputerUtilMana;
import forge.game.card.Card;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.ability.ApiType;
import forge.game.cost.CostSacrifice;
import forge.game.zone.ZoneType;

/**
 * Oops! All Spells: the win that follows the Balustrade Spy has to be certain. With the Oracle (or Jace, Wielder of
 * Mysteries) in hand it only is when the mana for it is there after the Spy, in the same turn: the library is empty for
 * good, and the next draw step loses the game before the Oracle can be cast. The other way, the flashback of Dread
 * Return with three creatures, costs no mana and is judged by {@link ComboPlan#libraryOutSelfMillSafe}.
 */
final class LibraryOutWin {
    private LibraryOutWin() {
    }

    /** Creatures the flashback of Dread Return sacrifices. */
    private static final int FLASHBACK_CREATURES = 3;

    /**
     * Whether the win can still be made after this play (the cast of the Spy or an activated outlet). True when no winner
     * is in hand (the other route is judged elsewhere), when one can be paid for together with the play, or when the
     * flashback of Dread Return is there to be paid with creatures.
     */
    static boolean castable(final Player ai, final ComboPlan plan, final SpellAbility play, final int extraCreatures) {
        if (ai == null || plan == null || play == null || play.getPayCosts() == null
                || !plan.has(ComboPlan.Kind.LIBRARY_OUT)) {
            return true;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if ("Jace, Wielder of Mysteries".equals(c.getName()) || "Laboratory Maniac".equals(c.getName())) {
                return true;
            }
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (c == play.getHostCard() || c.isLand() || !AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_PAYOFF, c.getName())) {
                continue;
            }
            if (canPayTogether(ai, play, c.getFirstSpellAbility())) {
                return true;
            }
        }
        boolean recursion = false;
        boolean reachableWinner = false;
        int fodder = ai.getCreaturesInPlay().size() + extraCreatures;
        if (play.getHostCard().isCreature() && play.getHostCard().isInZone(ZoneType.Battlefield)
                && play.getPayCosts().getCostParts().stream().anyMatch(part -> part instanceof CostSacrifice && part.payCostFromSource())) {
            fodder--;
        }
        for (final Card c : ai.getCardsIn(Arrays.asList(ZoneType.Graveyard, ZoneType.Library))) {
            reachableWinner |= c.isCreature() && AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_PAYOFF, c.getName());
            recursion |= AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_RECURSION, c.getName());
            if (c.isInZone(ZoneType.Library) && c.isCreature() && c.getTriggers().stream().anyMatch(trigger ->
                    "Library".equals(trigger.getParam("Origin")) && "Graveyard".equals(trigger.getParam("Destination")))) {
                fodder++;
            }
        }
        if (!reachableWinner) {
            return false;
        }
        if (recursion && fodder >= FLASHBACK_CREATURES) {
            return true;
        }
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            if (AiCardLists.has(AiCardLists.Role.REANIMATION_SPELL, c.getName())
                    || AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_RECURSION, c.getName())) {
                final SpellAbility reanimate = c.getFirstSpellAbility();
                if (reanimate != null && reanimate.getApi() == ApiType.ChangeZone
                        && "Graveyard".equals(reanimate.getParam("Origin"))
                        && "Battlefield".equals(reanimate.getParam("Destination"))
                        && reanimate.usesTargeting() && reanimate.getPayCosts().isOnlyManaCost()
                        && canPayTogether(ai, play, reanimate)) {
                    for (final Card winner : ai.getCardsIn(Arrays.asList(ZoneType.Graveyard, ZoneType.Library))) {
                        if (winner.isCreature() && AiCardLists.has(AiCardLists.Role.LIBRARY_OUT_PAYOFF, winner.getName())
                                && winner.isValid(reanimate.getTargetRestrictions().getValidTgts(), ai, c, reanimate)
                                && (!"Reanimate".equals(c.getName()) || ai.getLife() > winner.getCMC())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean canPayTogether(Player ai, SpellAbility play, SpellAbility followUp) {
        if (followUp == null || followUp.getPayCosts() == null) {
            return false;
        }
        try {
            final ManaCostBeingPaid total = new ManaCostBeingPaid(play.getPayCosts().getTotalMana());
            total.addManaCost(followUp.getPayCosts().getTotalMana());
            play.setActivatingPlayer(ai);
            return ComputerUtilMana.canPayManaCost(total, play, ai, false);
        } catch (final RuntimeException e) {
            return false; // an unreadable follow-up cannot promise a win before the empty-library draw
        }
    }
}
