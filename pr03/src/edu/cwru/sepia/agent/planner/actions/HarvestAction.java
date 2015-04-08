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

public class HarvestAction implements StripsAction {
    public String getName() { return"HARVEST";}
    private GameState.ExistentialPeasant peasantOfInterest;
    private ResourceType resourceType;
    public HarvestAction(GameState.ExistentialPeasant peasantOfInterest, ResourceType resourceType){
        this.peasantOfInterest = peasantOfInterest;
        this.resourceType = resourceType;
    }

    @Override
    public String getSentence() {
        return "HARVEST("+peasantOfInterest.getPeasantID()+", "+resourceType.toString()+")";
    }

    @Override
    public String toString(){
        return getSentence();
    }

    @Override
    //The peasant has to have nothing
    //The peasant must be next to either a mine or a forest
    public boolean preconditionsMet(GameState state) {
        GameState.ExistentialPeasant peasant =  state.getPeasantTracker().get(peasantOfInterest.getPeasantID());
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
        if(peasant.isHasGold() || peasant.isHasWood()) {
            if(PlannerAgent.debug) System.out.println("Peasant was carrying something.  Cannot consider Harvesting.");
            return false;
        }
        switch (resourceType){
            case WOOD:
                boolean canHarvest = state.getWoodOnField() >0 && peasant.isBesideWood();
                if(PlannerAgent.debug) {
                    System.out.println("Calculating if peasant: "+peasant.toString()+" can harvest WOOD, result is "+canHarvest);
                    System.out.println("Wood on field: "+state.getWoodOnField()+"\t Beside wood: "+peasant.isBesideWood());
                }
                return canHarvest;
            case GOLD:
                canHarvest = state.getGoldOnField() >0 && peasant.isBesideGold();
                if(PlannerAgent.debug) {
                    System.out.println("Calculating if peasant: "+peasant.toString()+" can harvest GOLD, result is "+canHarvest);
                    System.out.println("Gold on field: "+state.getGoldOnField()+"\t Beside gold: "+peasant.isBesideGold());
                }
                return canHarvest;
            default:
                if(PlannerAgent.debug) System.out.println("Peasant wanted to harvest unknown type. failing.");
                return false;
        }
    }

    @Override
    //The peasant now has more gold or wood
    //The mine or forest has less gold or wood
    public GameState apply(GameState state) {
        if(!preconditionsMet(state)) {
            System.err.println("ERROR! ATTEMPTED TO HARVEST WHEN NOT POSSIBLE");
            return null;
        }
        GameState postHarvestState = new GameState(state,1d,this);
        GameState.ExistentialPeasant peasant = postHarvestState.getPeasantTracker().get(peasantOfInterest.getPeasantID());
        
        if(this.resourceType == ResourceType.WOOD) {
            peasant.setHasWood(true);
            peasant.setBesideWood(true);
            postHarvestState.setWoodOnField(postHarvestState.getWoodOnField()-100);
            peasant.setCargoType(ResourceType.WOOD); //need to keep your resource type updated for use in DepositAction
        } else if(this.resourceType == ResourceType.GOLD) {
            peasant.setHasGold(true);
            peasant.setBesideGold(true);
            postHarvestState.setGoldOnField(postHarvestState.getGoldOnField()-100);
            peasant.setCargoType(ResourceType.GOLD); //need to keep your resource type updated for use in DepositAction
        }
        return postHarvestState;
    }
}
