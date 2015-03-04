package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
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

    protected State.StateView stateView;//nice for function calls
    protected List<Unit.UnitView> units;
    protected List<Unit.UnitView> footmen = new ArrayList<Unit.UnitView>();
    protected List<Unit.UnitView> archers = new ArrayList<Unit.UnitView>();
    protected boolean AMIMAX = false;
    private ArrayList<GameStateChild> children = new ArrayList<GameStateChild>();
    
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
        this.units = state.getAllUnits();
        parseUnits();//puts the archers and footmen into their own lists
        this.stateView = state;//just reference the whole damn thing.
                                //but actually, it's super useful for function calls
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
    public void getUtility() {
        // see ActionApplier's applyHeuristic for actual use
        /**
         * Why did we do this?
         * We were never given, nor could we find or ask, a way to make a new state in a cheap manner.
         * So the utility is calculated from a previous state, and the actions left to apply.
         * Then, once we actually explore that state, we fully generate it.
         */
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
    public List<GameStateChild> getChildren() {
        //see getUnappliedChildren for details
        //unused.  See getUtility for details.
        return getUnappliedChildren();
    }

    /**
     * gets all State/desired actionMap tuples possible from this state.
     * NOTE: the returned GameStateChild's state is BEFORE the actionMap has been performed
     * @return all possible GameStateChildren, with unapplied actions (i.e. state returned is this)
     */
    public List<GameStateChild> getUnappliedChildren(){
        ArrayList<List<Action>> unitActions = new ArrayList<List<Action>>();
        //we keep a list of every action for every unit.  Making it a 2D array
        ArrayList<Integer> idList = new ArrayList<Integer>();//list of IDs, used for making actionCombos
        
        if(AMIMAX){
            for(Unit.UnitView footman: footmen){
                unitActions.add(getActions(footman));//all actions possible from this footman to the enemies
                idList.add(footman.getID());
            }
        } else {
            for(Unit.UnitView archer: archers){
                unitActions.add(getActions(archer));//populate the aligned index with the list of possible actions
                idList.add(archer.getID());
            }
        }
        
        //create all the action combinations
        List<Action[]> actionCombos = new ArrayList<Action[]>();
        generateActionCombos(idList, flattenActions(unitActions), actionCombos, 0, new Action[idList.size()]);
        //actionCombos is now filled and useful
        
        for (Action[] stateActions : actionCombos) {
            children.add(new GameStateChild(makeTupleFromActions(stateActions), this));
        }
        children = MinimaxAlphaBeta.orderChildrenWithHeuristics(children);
        return children;
    }
    
    private Map<Integer, Action> makeTupleFromActions(Action[] actions){
        Map<Integer, Action> returnVar = new HashMap<Integer, Action>();
        for(Action action: actions){
            returnVar.put(action.getUnitId(), action);
        }
        return returnVar;
    }

    /**
     * you give me a list of list of actions, I give you an ID/actionList key/pair map 
     * @param unitActions 2D list of actions, first being by ID, second an unordered set.
     * @return an ID/List of actions map
     */
    private Map<Integer, LinkedList<Action>> flattenActions(ArrayList<List<Action>> unitActions){
        
        Map<Integer, LinkedList<Action>> flattenedActions = new HashMap<Integer, LinkedList<Action>>();
        for(List<Action> unitsActions : unitActions){
            for(Action individualAction: unitsActions){
                if(flattenedActions.containsKey(individualAction.getUnitId())){//an action is already there
                    flattenedActions.get(individualAction.getUnitId()).add(individualAction);
                } else {//this is the first action, gotta add a LinkedList
                    LinkedList<Action> temp = new LinkedList<Action>();
                    temp.add(individualAction);
                    flattenedActions.put(individualAction.getUnitId(), temp);
                }
            }
        }
        return flattenedActions;
    }

    /* initial call:
        unitIDs is all IDs (req'd)
        unitsAndActions is unitID/actionList key/pair combo (req'd)
        actionCombos is all (thus far) unique action combinations (new empty list)
        depth is recursion level ( 0 )
        current is empty array
     */
    private void generateActionCombos(ArrayList<Integer> unitIDs, Map<Integer, LinkedList<Action>> unitsAndActions,
                                      List<Action[]> actionCombos,int depth, Action[] current){

        if(depth >= unitsAndActions.keySet().size()){
            actionCombos.add(current.clone());
            return;
        }
        LinkedList<Action> currentUnitsActions = unitsAndActions.get(unitIDs.get(depth));

        for (Action currentUnitsAction : currentUnitsActions) {
            current[depth] = currentUnitsAction;
            generateActionCombos(unitIDs, unitsAndActions, actionCombos, depth + 1, current);
        }
    }


    /**
     *  you give me a player (and set AMIMAX), I give you the list of all actions that player can take
     * @param unit source player
     * @return legal moves that the source player can make onto the destination enemies
     */
    private List<Action> getActions(Unit.UnitView unit) {
        List<Action> legalUnitActions = new ArrayList<Action>();
        
        // Add all possible moves to the action list for this player
        for (Direction direction : Direction.values()) {
            //we're only allowing cardinal directions, so ignore diagonals.
            if(direction.xComponent() != 0 && direction.yComponent() != 0) continue;//skip diagonals
            if (isLegalMove(unit.getXPosition() + direction.xComponent(), unit.getYPosition() + direction.yComponent())) {
                legalUnitActions.add(Action.createPrimitiveMove(unit.getID(), direction));
            }
        }

        // Add all possible attacks to the action list for this player
        for (Unit.UnitView enemy : enemiesInRange(unit, AMIMAX ? archers : footmen)) {
            legalUnitActions.add(Action.createPrimitiveAttack(unit.getID(), enemy.getID()));
        }
        return legalUnitActions;
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
}
