package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * Created by luc on 3/28/15.
 */
public class CreateAction implements StripsAction {
    public static String name = "CREATE";
    public CreateAction(){
        //Hello, I am constructor.  Nice to meet you.
    }

    @Override
    public String getSentence() {
        return "CREATED A PEASANT";
    }

    @Override
    public boolean preconditionsMet(GameState state) {
        //Needs to have buildPeasants true (check)
        //Needs 400 gold per peasant created (check)
        //Town Hall has 3 food and each peasant needs 1 food.  (ie, max of 3 peasants on the map at a time.) (check)
        return(state.getBuildPeasants() && state.getOwnedGold() >= 400 && state.getNumPeasants() <= 3);
    }

    @Override
    public GameState apply(GameState state) {
        GameState postCreationState = new GameState(state, 0d, this); //TODO: Change the cost to this node...

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
