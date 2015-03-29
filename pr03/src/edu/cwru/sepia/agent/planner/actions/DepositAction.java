package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by luc on 3/28/15.
 */
public class DepositAction implements StripsAction{
    @Override
    public String getSentence() {
        return "DEPOSIT";
    }

    @Override
    //A peasant needs to have gold to deposit
    //The peasant in question needs to be next to a town hall
    public boolean preconditionsMet(GameState state) {
        return false;
    }

    @Override
    //The peasant has no more gold
    //The town hall has more gold
    public GameState apply(GameState state) {
        return null;
    }
}
