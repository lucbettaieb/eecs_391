package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * Created by luc on 3/28/15.
 */
public class HarvestAction implements StripsAction {
    private int peasantOfInterest = -1;
    private ResourceType resourceType;
    public HarvestAction(int peasantOfInterest, ResourceType resourceType){
        this.peasantOfInterest = peasantOfInterest - 1;
        this.resourceType = resourceType;
    }

    @Override
    public String getSentence() {
        return "HARVEST";
    }

    @Override
    //The peasant has to have nothing
    //The peasant must be next to either a mine or a forest
    public boolean preconditionsMet(GameState state) {
        GameState.ExistentialPeasant peasant =  state.getPeasantTracker().get(peasantOfInterest);
        if(peasant.isHasGold() || peasant.isHasWood()) return false;
        switch (resourceType){
            case WOOD:
                return state.getWoodOnField() >0 && peasant.isBesideWood();
            case GOLD:
                return state.getGoldOnField() >0 && peasant.isBesideGold();
        }
        return false;
    }
    
    public static boolean canHarvest(GameState.ExistentialPeasant peasant, GameState state, ResourceType resourceType){
        if(peasant.isHasGold() || peasant.isHasWood()) return false;
        switch (resourceType){
            case WOOD:
                return state.getWoodOnField() >0 && peasant.isBesideWood();
            case GOLD:
                return state.getGoldOnField() >0 && peasant.isBesideGold();
        }
        return false;
    }

    @Override
    //The peasant now has more gold or wood
    //The mine or forest has less gold or wood
    public GameState apply(GameState state) {
        if(!preconditionsMet(state)) return null;
        GameState.ExistentialPeasant peasant = state.getPeasantTracker().get(peasantOfInterest);
        GameState returnVar = new GameState(state,0d,this);
        if(this.resourceType == ResourceType.WOOD) {
            peasant.setHasWood(true);
            returnVar.setWoodOnField(returnVar.getWoodOnField()-100);

        }
        else if(this.resourceType == ResourceType.GOLD) {
            peasant.setHasGold(true);
            returnVar.setGoldOnField(returnVar.getGoldOnField()-100);
        }
        return returnVar;
    }
}
