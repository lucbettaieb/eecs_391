package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
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
    protected boolean isMAX = false;

    /**
     * You will implement this constructor. It will
     * extract all of the needed state information from the built in
     * SEPIA state view.
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

    /**
     * I take the 'units' field, and use it to fill
     * the 'footmen' and 'archers' fields.
     */
    private void parseUnits() {
        for (Unit.UnitView unit : this.units) {//categorize dem units
            if (unit.getTemplateView().getName().equals("Footman")) {//I'm guessing here.
                footmen.add(unit);                      //debug it at runtime to ensure it's right
            } else if (unit.getTemplateView().getName().equals("Archer")) {
                archers.add(unit);
            }
        }
    }


    /**
     * You will implement this function.
     * <p/>
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

        for (Unit.UnitView archer : this.archers) badHealth += archer.getHP();

        for (Unit.UnitView footman : this.footmen) {
            goodHealth += footman.getHP();//popped it into the loop to prevent 2 extra instructions...
            int minDistance = Integer.MAX_VALUE;
            for (Unit.UnitView archer : this.archers) {
                int currentDistance = manhattanDistance(footman, archer);
                if (currentDistance < minDistance) minDistance = currentDistance;
            }
            distance += minDistance;
        }
        return goodHealth - badHealth + distance;
    }

    /**
     * * delta x plus delta y, aka taxicab distance
     * you give me a source and destination UnitView,
     * I give you their manhattan distance
     */
    private int manhattanDistance(Unit.UnitView source, Unit.UnitView destination) {
        return Math.abs(source.getXPosition() - destination.getXPosition()) +
                Math.abs(source.getYPosition() - destination.getYPosition());
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
    /**
     * NOTE: isMAX field is required to be set for this.  We need to know if footmen or archers are being played 
     * @return the possible future game states from the current state
     */
    public List<GameStateChild> getChildren() {//TODO: add memoization?  (is this called more than once per state?)

        ArrayList<List<Action>> unitActions = new ArrayList<List<Action>>();
        //we keep a list of every action for every unit.  Making it a 2D array
        ArrayList<Integer> unitIDs = new ArrayList<Integer>();//index-aligned IDs for the units
        ArrayList<Map<Integer, Action>> actions = new ArrayList<Map<Integer, Action>>();
        // combination of uniActions and unitIDs into one data structure, but unitActions is flattened.

        if (isMAX) {
            for (Unit.UnitView footman : footmen) {
                unitIDs.add(footman.getID());//populate the ID array entry
                unitActions.add(getActions(footman, archers));//all actions possible from this footman to the enemies
            }
        } else {
            for (Unit.UnitView archer : archers) {
                unitIDs.add(archer.getID());//populate the current index with the ID
                unitActions.add(getActions(archer, footmen));//populate the aligned index with the list of possible actions
            }
        }//end of move creation

        //add the moves to the full map
        int i = 0;
        for (List<Action> unitActionList : unitActions) {
            for (Action unitAction : unitActionList) {//fill the map with this unit's actions
                HashMap<Integer, Action> unitActionMap = new HashMap<Integer, Action>();
                //temprary map that's gonna get added to the actions arrayList
                unitActionMap.put(unitIDs.get(i), unitAction);
                //NOTE: This is a crappy scheme.  Each hashmap will be a single tuple.
                actions.add(unitActionMap);//put the single tuple into the actions list.
            }
            i++;
        }

        //time to mix-and-match to create all possible combinations
        i = 0;
        for (List<Action> unitActionList : unitActions) {//for every unit's set of actions
            for (Action unitAction : unitActionList) {//for every action within that set
                for (Map<Integer, Action> unitActionMap : actions) {//for every state's possible action set
                    unitActionMap.put(unitIDs.get(0), unitAction);//combine them all.
                }
            }
            i++;
        }

        ArrayList<GameStateChild> children = new ArrayList<GameStateChild>();
        for (Map<Integer, Action> actionMap : actions) {
            children.add(new GameStateChild(actionMap, this));
        }
        children = MinimaxAlphaBeta.orderChildrenWithHeuristics(children);
        return children;
    }

    /**
     *  you give me a player and its enemy set, I give you the list of all actions that player can take
     * @param player source player
     * @param enemies destination enemies
     * @return legal moves that the source player can make onto the destination enemies
     */
    private List<Action> getActions(Unit.UnitView player, List<Unit.UnitView> enemies) {
        List<Action> actions = new ArrayList<Action>();
        
        // Add all possible moves to the action list for this player
        for (Direction direction : Direction.values()) {
            if (isLegalMove(player.getXPosition() + direction.xComponent(), player.getYPosition() + direction.yComponent())) {
                actions.add(Action.createPrimitiveMove(player.getID(), direction));
            }
        }

        // Add all possible attacks to the action list for this player
        for (Unit.UnitView enemy : enemiesInRange(player)) {
            actions.add(Action.createCompoundAttack(player.getID(), enemy.getID()));
        }
        return actions;

    }

    /**
     *  you give me a destination coordinate pair, I tell you if something is there, or if it's out of bounds
     * @param x intended x coordinate to move to
     * @param y intended y coordinate to move to
     * @return whether the move is projected to succeed
     */
    private boolean isLegalMove(int x, int y){
        for(ResourceNode.ResourceView block :blockObjects){
            if (block.getXPosition() == x && block.getYPosition() == y) return false;
        }//if nothing's blocking it, then just make sure it's within bounds
        return stateView.inBounds(x,y);
    }

    /**
     * @param loc1 source location 1
     * @param loc2 source location 2
     * @return difference in x-coordinates of the locations
     */
    private int deltaX(Unit.UnitView loc1, Unit.UnitView loc2){
        return Math.abs(loc1.getXPosition() - loc2.getXPosition());
    }

    /**
     * @param loc1 source location 1
     * @param loc2 source location 2
     * @return difference in y-coordinates of the locations
     */
    private int deltaY(Unit.UnitView loc1, Unit.UnitView loc2){
        return Math.abs(loc1.getYPosition() - loc2.getYPosition());
    }

    /**
     * you give me the player we're working from, I give you the enemies within attack range 
     * @param player player we're working from
     * @return list of enemies within attacking range
     */
    private List<Unit.UnitView> enemiesInRange(Unit.UnitView player) {
        int range = 0;
        int xDiff = 0;
        int yDiff = 0;
        List<Unit.UnitView> enemies;
        List<Unit.UnitView> enemiesInRange = new ArrayList<Unit.UnitView>();

        if (isMAX) {
            enemies = archers;
            range = 1;
        } else {
            enemies = footmen;
            range = 15;//yay magic numbers!
        }

        for (Unit.UnitView enemy : enemies) {
            xDiff = deltaX(player, enemy);
            yDiff = deltaY(player, enemy);
            if (range >= (xDiff + yDiff))  enemiesInRange.add(enemy);
        }
        return enemiesInRange;
    }

    /**
     * swaps the current player (MAX or MIN) 
     * @return this, for ease of chaining
     */
    public GameState flipPlayer(){
        this.isMAX = !this.isMAX;
        return this;
    }
}
