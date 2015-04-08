package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by Luc Bettaieb on 3/28/15.
 *
 * CreateAction.java
 *  A class to represent the STRIPS "CREATE" action.
 */

public class CreateAction implements StripsAction {
    public String getName(){return "CREATE";}

    public CreateAction(){
        //Hello, I am constructor.  Nice to meet you.  My life is constructor.  All I know is constructor.  When I don't constructor, I am sad.
    }

    @Override
    public String getSentence() {
        return "CREATED A PEASANT";
    }

    /**
     * Function to check if the necessary preconditions of creating a peasant are met.
     * @param state GameState to check if action is applicable
     * @return TRUE if GameState will be building peasants AND we have 400 gold AND there are no more than 3 peasants.
     */
    @Override
    public boolean preconditionsMet(GameState state) {
        //Needs to have buildPeasants true (check)
        //Needs 400 gold per peasant created (check)
        //Town Hall has 3 food and each peasant needs 1 food.  (ie, max of 3 peasants on the map at a time.) (check)
        return(state.getBuildPeasants() && state.getOwnedGold() >= 400 && state.getNumPeasants() <= 3);
    }

    /**
     * Function to apply the create action to the GameState
     * @param state State to apply action to
     * @return a new GameState that has the action applied to it
     */
    @Override
    public GameState apply(GameState state) {
        GameState postCreationState = new GameState(state, 1d, this); //TODO: Change the cost to this node...

        //Removes 400 gold from the townhall
        postCreationState.setOwnedGold(postCreationState.getOwnedGold() - 400);

        //Remove 1 food
        postCreationState.removeOneFood();

        //Increases the amount of peasants
        postCreationState.addPeasant();
        postCreationState.getPeasantTracker().get(postCreationState.getPeasantTracker().size() - 1).resetBools();       //makes sure nothing about it is true
        postCreationState.getPeasantTracker().get(postCreationState.getPeasantTracker().size() - 1).setBesideTH(true);  //makes sure it knows where it is

        return postCreationState;
    }
}
