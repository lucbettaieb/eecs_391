package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by luc on 3/28/15.
 */
public class HarvestAction implements StripsAction {

    @Override
    public String getSentence() {
        return "HARVEST";
    }

    @Override
    //The peasant has to have nothing
    //The peasant must be next to either a mine or a forest
    public boolean preconditionsMet(GameState state) {
        return false;
    }

    @Override
    //The peasant now has more gold or wood
    //The mine or forest has less gold or wood
    public GameState apply(GameState state) {
        return null;
    }
}
