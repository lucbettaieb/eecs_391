package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.PlannerAgent;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * MoveAction.java
 * A class to represent the STRIPS "MOVE" action.
 *
 * Aidan Campbell and Luc Bettaieb
 */

public class MoveAction implements StripsAction {
    public String getName() { return "MOVE";}
    private GameState.ExistentialPeasant peasantOfInterest;
    private ResourceType resourceType;
    
    public MoveAction(GameState.ExistentialPeasant peasantOfInterest, ResourceType resourceType){
        this.peasantOfInterest = peasantOfInterest;
        this.resourceType = resourceType;
    }
    
    //if you're moving to TH, don't specify a resource.
    public MoveAction(GameState.ExistentialPeasant peasant){
        this.peasantOfInterest = peasant;
    }


    @Override
    public String getSentence() {
        if(resourceType == null) return "MOVE("+peasantOfInterest.getPeasantID()+",TOWNHALL)";
        return "MOVE("+peasantOfInterest.getPeasantID()+","+resourceType.toString()+")";
    }

    @Override
    public boolean preconditionsMet(GameState state) {
        return true;//no preconditions
    }

    /**
     * Static method to check if the preconditions for moving are true for use in the generateChildren function.
     * @param peasant to see if can Move
     * @param resourceType the resource we're checking to see if we can move to.  null is townhall
     * @return true if we can move to resourceType (null is townhall)
     */
    public static boolean canMove(GameState.ExistentialPeasant peasant, ResourceType resourceType){
        if(peasant.isBesideTH() && resourceType == null) return false;
        if(peasant.isBesideGold() && resourceType != null) return false;
        if(peasant.isBesideWood() && resourceType != null) return false;
        return true;
    }

    /**
     * Function to apply the move action to a new state and return it
     * @param state State to apply action to
     * @return a new state in which a move action is applied.
     */
    @Override
    public GameState apply(GameState state) {
        GameState returnVar = new GameState(state,1d,this);
        GameState.ExistentialPeasant newPeasant;
        
        if(returnVar.getPeasantTracker().size() > peasantOfInterest.getPeasantID()){
             newPeasant = returnVar.getPeasantTracker().get(peasantOfInterest.getPeasantID());
        } else {
            newPeasant = returnVar.initializePeasant();
        }
       
        newPeasant.setBesideGold(false);
        newPeasant.setBesideWood(false);
        newPeasant.setBesideTH(false);
        newPeasant.setHasGold(peasantOfInterest.isHasGold());
        newPeasant.setHasWood(peasantOfInterest.isHasWood());
        newPeasant.setCargoType(peasantOfInterest.getCargoType());
        if(this.resourceType == null)newPeasant.setBesideTH(true);
        else if(this.resourceType == ResourceType.WOOD) newPeasant.setBesideWood(true);
        else if(this.resourceType == ResourceType.GOLD) newPeasant.setBesideGold(true);
        return returnVar;
    }
    
    @Override 
    public String toString(){
        return getSentence();
    }
}
