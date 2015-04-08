package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.PlannerAgent;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * DepositAction.java
 * A class to represent the STRIPS "DEPOSIT" action.
 *
 * Aidan Campbell and Luc Bettaieb
 */

public class DepositAction implements StripsAction{
    public String getName() { return "DEPOSIT";}

    private GameState.ExistentialPeasant depositPeasant;

    public DepositAction(GameState.ExistentialPeasant p){
        depositPeasant = p;
    }

    @Override
    public String getSentence() {
        //TODO: check if peasant and resource of interest are real first?
        return "DEPOSIT("+depositPeasant.getPeasantID()+", "+depositPeasant.getCargoType().toString()+")";
    }

    @Override
    public String toString(){
        return getSentence();
    }

    /**
     * Function to return true if preconditions are met.
     * @param state GameState to check if action is applicable
     * @return TRUE if the necessary preconditions for depositing a resource are valid.  (You have a resource, and you're next to town hall.)
     */
    @Override
    //A peasant needs to have gold to deposit
    //The peasant in question needs to be next to a town hall
    //So,   1. Find peasant next to a town hall
    //      2. Check if that peasant is carrying cargo
    public boolean preconditionsMet(GameState state) {
        if(depositPeasant.isBesideTH() && (depositPeasant.isHasWood() || depositPeasant.isHasGold())){
            if(depositPeasant.isHasWood()) depositPeasant.setCargoType(ResourceType.WOOD);
            if(depositPeasant.isHasGold()) depositPeasant.setCargoType(ResourceType.GOLD);
            return true;
        } else return false;
    }

    /**
     * A function to apply the deposit action to the game state
     * @param state State to apply action to
     * @return a new state with the deposit action applied to it.
     */
    @Override
    //The peasant has no more gold
    //The town hall has more gold
    public GameState apply(GameState state) {
        if(!preconditionsMet(state)){
            System.err.println("ERROR: ATTEMPTED TO APPLY DEPOSIT WHEN PRECONDITIONS NOT MET.");
            return state;
        }

        GameState postDepositState = new GameState(state, 1d, this);
        GameState.ExistentialPeasant newPeasant;
        if(postDepositState.getPeasantTracker().size() > depositPeasant.getPeasantID()){
            newPeasant = postDepositState.getPeasantTracker().get(depositPeasant.getPeasantID());
        } else {
            newPeasant = postDepositState.initializePeasant();
        }
        
        
        newPeasant.setBesideGold(depositPeasant.isBesideGold());
        newPeasant.setBesideWood(depositPeasant.isBesideWood());
        newPeasant.setBesideTH(depositPeasant.isBesideTH());
        newPeasant.setHasGold(depositPeasant.isHasGold());
        newPeasant.setHasWood(depositPeasant.isHasWood());
        newPeasant.setCargoType(depositPeasant.getCargoType());
        
        if(newPeasant.getCargoType().equals(ResourceType.GOLD)){
            //Give gold to TownHall
            postDepositState.addToOwnedGold();

        } else if(newPeasant.getCargoType().equals(ResourceType.WOOD)){
            //Give wood to TownHall
            postDepositState.addToOwnedWood();

        } else {
            System.err.println("Error while getting peasant cargo type.  Perhaps peasant instantiation doesn't copy?");

        }
        //The townhall in the next state now has more gold or wood
        newPeasant.resetBools();
        newPeasant.setBesideTH(true);
        newPeasant.setCargoType(null);
        //The existential peasant's only life-affirming attribute is that it is indeed beside the town hall. :)
        return postDepositState;
    }
}