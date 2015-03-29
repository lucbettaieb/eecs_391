package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by luc on 3/28/15.
 */
public class MoveAction implements StripsAction {
    @Override
    public String getSentence() {
        return null;
    }

    @Override
    //The peasant must exist?  We're getting into some existential questions here.
    //I don't know
    public boolean preconditionsMet(GameState state) {
        return false;
    }

    @Override
    //Make dat sucka move.
    public GameState apply(GameState state) {
        return null;
    }
}
