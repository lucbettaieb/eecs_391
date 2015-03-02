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
    protected boolean AMIMAX = false;

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
    public double getUtility() {

        int footmanHP = 0;
        for (Unit.UnitView footman : footmen) footmanHP += footman.getHP();

        int archerHP = 0;
        for (Unit.UnitView archer : archers)  archerHP += archer.getHP();

        int footmenAlive = footmen.size();
        int archersAlive = archers.size();

        int distance = 0;
        for (Unit.UnitView footman : footmen) {
            int x = footman.getXPosition();
            int y = footman.getYPosition();

            List<Integer> distances = new ArrayList<Integer>();
            for (Unit.UnitView archer : archers) {
                distances.add(Math.max(
                        Math.abs(x - archer.getXPosition()),
                        Math.abs(y - archer.getYPosition())));
            }
            distance += distances.isEmpty() ? 0 : Collections.min(distances);
        }
        return  footmanHP - distance - 10 * archerHP + 10 * footmenAlive - 100 * archersAlive;
    }

    /**
     * * delta x plus delta y, aka taxicab distance
     * you give me a source and destination UnitView,
     * I give you their manhattan distance
     */
    public static int manhattanDistance(Unit.UnitView source, Unit.UnitView destination) {
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
     * NOTE: AMIMAX field is required to be set for this.  We need to know if footmen or archers are being played
     * @return the possible future game states from the current state
     */
    public List<GameStateChild> getChildren() {//TODO: add memoization?  (is this called more than once per state?)

        ArrayList<List<Action>> unitActions = new ArrayList<List<Action>>();
        //we keep a list of every action for every unit.  Making it a 2D array
        ArrayList<Integer> unitIDs = new ArrayList<Integer>();//index-aligned IDs for the units
        ArrayList<Map<Integer, Action>> actionMapList = new ArrayList<Map<Integer, Action>>();
        // combination of uniActions and unitIDs into one data structure, but unitActions is flattened.

        if (AMIMAX) {
            for (Unit.UnitView footman : footmen) {
                unitIDs.add(footman.getID());//populate the ID array entry
                unitActions.add(getActions(footman));//all actions possible from this footman to the enemies
            }
        } else {
            for (Unit.UnitView archer : archers) {
                unitIDs.add(archer.getID());//populate the current index with the ID
                unitActions.add(getActions(archer));//populate the aligned index with the list of possible actions
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
                actionMapList.add(unitActionMap);//put the single tuple into the actions list.
            }
            i++;
        }

        //time to mix-and-match to create all possible combinations
        i = 0;
        for (List<Action> unitActionList : unitActions) {//for every unit's set of actions
            for (Action unitAction : unitActionList) {//for every action within that set
                for (Map<Integer, Action> unitActionMap : actionMapList) {//for every state's possible action set
                    //TODO: sweet baby jesus I need to fix this complexity
                    unitActionMap.put(unitIDs.get(i), unitAction);//combine them all.
                }
            }
            i++;
        }

        ArrayList<GameStateChild> children = new ArrayList<GameStateChild>();
        for (Map<Integer, Action> actionMap : actionMapList) {
            children.add(new GameStateChild(actionMap, new GameState(ActionApplier.apply(actionMap, this.stateView))));
        }
        children = MinimaxAlphaBeta.orderChildrenWithHeuristics(children);
        return children;
    }

    /**
     *  you give me a player (and set AMIMAX), I give you the list of all actions that player can take
     * @param player source player
     * @return legal moves that the source player can make onto the destination enemies
     */
    private List<Action> getActions(Unit.UnitView player) {
        List<Action> actions = new ArrayList<Action>();
        
        // Add all possible moves to the action list for this player
        for (Direction direction : Direction.values()) {
            if (isLegalMove(player.getXPosition() + direction.xComponent(), player.getYPosition() + direction.yComponent())) {
                actions.add(Action.createPrimitiveMove(player.getID(), direction));
            }
        }

        // Add all possible attacks to the action list for this player
        for (Unit.UnitView enemy : enemiesInRange(player, AMIMAX ? archers : footmen)) {
            actions.add(Action.createPrimitiveAttack(player.getID(), enemy.getID()));
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
        return !stateView.isResourceAt(x,y) && !stateView.isUnitAt(x,y) && stateView.inBounds(x,y);
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
    private List<Unit.UnitView> enemiesInRange(Unit.UnitView player, List<Unit.UnitView> enemies) {
        int range, xDiff, yDiff;
        List<Unit.UnitView> enemiesInRange = new ArrayList<Unit.UnitView>();
        
        range = player.getTemplateView().getRange();

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
        this.AMIMAX = !this.AMIMAX;
        return this;
    }
}
