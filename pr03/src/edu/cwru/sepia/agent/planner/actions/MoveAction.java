package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.GameState_old;
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
        GameState returnVar = new GameState(state,1d,this);
        peasantOfInterest.setBesideGold(false);
        peasantOfInterest.setBesideWood(false);
        peasantOfInterest.setBesideTH(false);
        if(this.resourceType == null)peasantOfInterest.setBesideTH(true);
        else if(this.resourceType == ResourceType.WOOD) peasantOfInterest.setBesideWood(true);
        else if(this.resourceType == ResourceType.GOLD) peasantOfInterest.setBesideGold(true);
        return returnVar;
    }
}
