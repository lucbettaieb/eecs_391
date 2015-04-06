package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState_old;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * Created by luc on 3/28/15.
 */
public class MoveAction implements StripsAction {
    private int peasantOfInterest = -1;
    private ResourceType resourceType;
    
    public MoveAction(int peasantOfInterest, ResourceType resourceType){
        this.peasantOfInterest = peasantOfInterest;
        this.resourceType = resourceType;
    }
    
    //if you're moving to TH, don't specify a resource.
    public MoveAction(int peasantOfInterest){
        this.peasantOfInterest = peasantOfInterest;
    }
    
    
    @Override
    public String getSentence() {
        if(resourceType == null) return "MOVE("+peasantOfInterest+",TOWNHALL)";
        
        return "MOVE("+peasantOfInterest+","+resourceType.toString()+")";
    }

    @Override
    //The peasant must exist?  We're getting into some existential questions here.
    //I don't know
    public boolean preconditionsMet(GameState state) {
        return true;//no preconditions
    }

    @Override
    //Make dat sucka move.
    public GameState apply(GameState state) {
        GameState returnVar = new GameState(state,0d,this);
        state.getPeasantTracker().get(peasantOfInterest).resetBools();
        if(this.resourceType == null)state.getPeasantTracker().get(peasantOfInterest).setBesideTH(true);
        else if(this.resourceType == ResourceType.WOOD) state.getPeasantTracker().get(peasantOfInterest).setBesideWood(true);
        else if(this.resourceType == ResourceType.GOLD) state.getPeasantTracker().get(peasantOfInterest).setBesideGold(true);
        return returnVar;
    }
}
