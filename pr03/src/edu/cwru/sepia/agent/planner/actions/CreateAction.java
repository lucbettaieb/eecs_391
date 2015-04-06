package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by luc on 3/28/15.
 */
public class CreateAction implements StripsAction {

    @Override
    public String getSentence() {
        return " ?CREATE? ";
    }

    @Override
    public boolean preconditionsMet(GameState state) {
        //Needs to have buildPeasants true
        //Needs 400 gold per peasant created
        //Town Hall has 3 food and each peasant needs 1 food.  (ie, max of 3 peasants on the map at a time.)
        return(state.getBuildPeasants() && (state.getRequiredGold() - state.getRemainingGold()) >= 400 && state.getNumPeasants() < 3); //TODO: Consider changing this, the required - remaining is weird af.
    }

    @Override
    public GameState apply(GameState state) {
        //Removes 400 gold from the townhall
        //Increases the amount of peasants
        //anything else?
        state.removeOneFood();
        state.addPeasant(); //a new peasant has been theoretically created.
        state.iBuiltAPeasant();
        return null;
    }
}
