package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * This class stores all of the information the agent
 * needs to know about the state of the game. For example this
 * might include things like footmen HP and positions.
 *
 * Add any information or methods you would like to this class,
 * but do not delete or change the signatures of the provided methods.
 */
public class GameState {
    
    protected int xExtent, yExtent;
    protected List<ResourceNode.ResourceView> blockObjects;//things that will get in your way
    protected State.StateView stateView;
    protected List<Unit.UnitView> units;
    protected List<Unit.UnitView> footmen = new ArrayList<Unit.UnitView>();
    protected List<Unit.UnitView> archers = new ArrayList<Unit.UnitView>();
    
    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
     *
     * You may find the following state methods useful:
     *
     * state.getXExtent() and state.getYExtent(): get the map dimensions
     * state.getAllResourceIDs(): returns all of the obstacles in the map
     * state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
     *
     * For a given ResourceView you can query the position using
     * resource.getXPosition() and resource.getYPosition()
     *
     * For a given unit you will need to find the attack damage, range and max HP
     * unitView.getTemplateView().getRange(): This gives you the attack range
     * unitView.getTemplateView().getBasicAttack(): The amount of damage this unit deals
     * unitView.getTemplateView().getBaseHealth(): The maximum amount of health of this unit
     *
     * @param state Current state of the episode
     */
    public GameState(State.StateView state) {
        this.xExtent = state.getXExtent();
        this.yExtent = state.getYExtent();
        this.blockObjects = state.getAllResourceNodes();
        this.units = state.getAllUnits();
        parseUnits();//puts the archers and footmen into their own lists


        this.stateView = state;//just reference the whole damn thing.
    }
    
    private void parseUnits(){
        for (Unit.UnitView unit : this.units) {//categorize dem units
            if (unit.getTemplateView().getName().equals("Footman")) {//I'm guessing here.
                footmen.add(unit);                      //debug it at runtime to ensure it's right
            } else if(unit.getTemplateView().getName().equals("Archer")) {
                archers.add(unit);
            }
        }
    }
        //state.getResourceNode(Integer resourceID): Return a ResourceView for the given ID
    

    /**
     * You will implement this function.
     *
     * You should use weighted linear combination of features.
     * The features may be primitives from the state (such as hp of a unit)
     * or they may be higher level summaries of information from the state such
     * as distance to a specific location. Come up with whatever features you think
     * are useful and weight them appropriately.
     *
     * It is recommended that you start simple until you have your algorithm working. Then watch
     * your agent play and try to add features that correct mistakes it makes. However, remember that
     * your features should be as fast as possible to compute. If the features are slow then you will be
     * able to do less plys in a turn.
     *
     * Add a good comment about what is in your utility and why you chose those features.
     *
     * @return The weighted linear combination of the features
     */
    public double getUtility() {//I'll say I'm MAX, and I use footmen.
        double goodHealth = 0d;//health of footmen
        double badHealth = 0d;//health of archers
        int distance = 0;//distance between each footman, and what's probably its target
        
        for(Unit.UnitView archer : this.archers) badHealth  += archer.getHP();
        
        for(Unit.UnitView footman: this.footmen){
            goodHealth += footman.getHP();//popped it into the loop to prevent 2 extra instructions...
            int mindistance = Integer.MAX_VALUE;
            for(Unit.UnitView archer: this.archers){
                int currentDistance = manhattanDistance(footman, archer);
                if(currentDistance<mindistance) mindistance = currentDistance;
            }
            distance += mindistance;
        }
        return goodHealth-badHealth+distance;
    }
    
    //delta x plus delta y, aka taxicab distance
    private int manhattanDistance(Unit.UnitView source, Unit.UnitView destination){
        return Math.abs(source.getXPosition()-destination.getXPosition())+
                Math.abs(source.getYPosition()-destination.getYPosition());
    }

    /**
     * You will implement this function.
     *
     * This will return a list of GameStateChild objects. You will generate all of the possible
     * actions in a step and then determine the resulting game state from that action. These are your GameStateChildren.
     *
     * You may find it useful to iterate over all the different directions in SEPIA.
     *
     * for(Direction direction : Directions.values())
     *
     * To get the resulting position from a move in that direction you can do the following
     * x += direction.xComponent()
     * y += direction.yComponent()
     *
     * @return All possible actions and their associated resulting game state
     */
    public List<GameStateChild> getChildren() {
        
        
        //moves:
        for(Direction direction : Direction.values()){
            
        }
        return null;
    }
}
