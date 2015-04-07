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
            return true;
        } else return false;
    }

    @Override
    //The peasant has no more gold
    //The town hall has more gold
    public GameState apply(GameState state) {
        GameState postDepositState = new GameState(state, 0d, this); //TODO: Maybe change 0d to something that makes sense for Astar
        for (GameState.ExistentialPeasant p : postDepositState.getPeasantTracker()){
            if(p.getPeasantID() == depositPeasant.getPeasantID()){
                if(p.getCargoType().equals(ResourceType.GOLD)){
                    //Give gold to TownHall
                    postDepositState.addToOwnedGold();
                } else{
                    //Give wood to TownHall
                    postDepositState.addToOwnedWood();
                }
                //The townhall in the next state now has more gold or wood

                p.resetBools();
                p.setBesideTH(true);
                //The existential peasant's only life-affirming attribute is that it is indeed beside the town hall. :)

            }
        }
        return postDepositState;
    }
}
