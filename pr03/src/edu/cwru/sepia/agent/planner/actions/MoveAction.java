package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.PlannerAgent;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * Created by luc on 3/28/15.
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
    //The peasant must exist?  We're getting into some existential questions here.
    //I don't know
    public boolean preconditionsMet(GameState state) {
        return true;//no preconditions
    }
    
    public static boolean canMove(GameState.ExistentialPeasant peasant, ResourceType resourceType){
        if(peasant.isBesideTH() && resourceType == null) return false;
        if(peasant.isBesideGold() && resourceType != null) return false;
        if(peasant.isBesideWood() && resourceType != null) return false;
        return true;
    }

    @Override
    //Make dat sucka move.
    public GameState apply(GameState state) {
        if(PlannerAgent.debug)System.out.println("Applying a MOVE: "+getSentence());
        GameState returnVar = new GameState(state,1d,this);
        GameState.ExistentialPeasant newPeasant = returnVar.getPeasantTracker().get(peasantOfInterest.getPeasantID());
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
