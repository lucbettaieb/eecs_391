package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.environment.model.state.ResourceType;

/**
 * Created by luc on 3/28/15.
 */
public class DepositAction implements StripsAction{
    public String getName() { return"DEPOSIT";}
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
    //A peasant needs to have gold to deposit
    //The peasant in question needs to be next to a town hall
    //So,   1. Find peasant next to a town hall
    //      2. Check if that peasant is carrying cargo
    public boolean preconditionsMet(GameState state) {
        if(depositPeasant.isBesideTH() && depositPeasant.isHasWood() || depositPeasant.isHasGold()){
            if(depositPeasant.isHasWood()) depositPeasant.setCargoType(ResourceType.WOOD);
            if(depositPeasant.isHasGold()) depositPeasant.setCargoType(ResourceType.GOLD);
            return true;
        } else return false;
    }

    @Override
    //The peasant has no more gold
    //The town hall has more gold
    public GameState apply(GameState state) {
        if(!preconditionsMet(state)){
            System.err.println("ERROR: ATTEMPTED TO APPLY DEPOSIT WHEN PRECONDITIONS NOT MET.");
            return state;
        }

        GameState postDepositState = new GameState(state, 1d, this); //TODO: Maybe change 0d to something that makes sense for Astar
        if(depositPeasant.getCargoType() == null){
            System.err.println("ERROR! PEASANT'S CARGO WAS NULL!");
            System.err.println(depositPeasant.toString());
        }

        GameState.ExistentialPeasant newPeasant = postDepositState.getPeasantTracker().get(depositPeasant.getPeasantID());

        if(depositPeasant.getCargoType().equals(ResourceType.GOLD)){
            //Give gold to TownHall
            postDepositState.addToOwnedGold();
        } else{
            //Give wood to TownHall
            postDepositState.addToOwnedWood();
        }
        //The townhall in the next state now has more gold or wood
        newPeasant.resetBools();
        newPeasant.setBesideTH(true);
        //The existential peasant's only life-affirming attribute is that it is indeed beside the town hall. :)
        return postDepositState;
    }
}
