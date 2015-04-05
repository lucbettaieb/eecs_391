package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.util.List;

/**
 * This class is used to represent the state of the game after applying one of the avaiable actions. It will also
 * track the A* specific information such as the parent pointer and the cost and heuristic function. Remember that
 * unlike the path planning A* from the first assignment the cost of an action may be more than 1. Specifically the cost
 * of executing a compound action such as move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2). Implement the methods provided and
 * add any other methods and member variables you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * I recommend storing the actions that generated the instance of the GameState in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
    //Fields, these should be private

    private final State.StateView state; //The StateView of the world which allows us to query the actual "state" of SEPIA
    private final int playerNum;         //The player number of the agent that is planning TODO: What is this?  Do we need it?

    private final int requiredWood;      //The goal amount of wood we need to win the game (which you just lost)
    private int remainingWood;           //The amount of wood that is left on the map
    private int ownedWood;               //The amount of wood we have

    private final boolean buildPeasants; //Whether or not we're going to be building peasants in this scenario
    private boolean builtPeasant;        //Did we build a peasant yet? TODO: Is this necessary?


    private final int requiredGold;      //The amount of gold we need to win the game
    private int remainingGold;           //How much gold that is left on the map
    private int ownedGold;               //The amount of gold we have, this will be updated by the DepositAction

    private int numPeasants;             //How many peasants?  Never too many.

    private int amountFood;              //Ya gotta eat.  But only 3 at a time.

    private final double costToThisNode; //TODO: What does this even do?
    

    /**
     * Construct a GameState from a stateview object. This is used to construct the initial search node. All other
     * nodes should be constructed from the another constructor you create or by factory functions that you create.
     *
     * @param state The current stateview at the time the plan is being created
     * @param playernum The player number of agent that is planning
     * @param requiredGold The goal amount of gold (e.g. 200 for the small scenario)
     * @param requiredWood The goal amount of wood (e.g. 200 for the small scenario)
     * @param buildPeasants True if the BuildPeasant action should be considered
     */
    public GameState(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants){
        this.state = state;
        this.playerNum = playernum;

        this.requiredGold = requiredGold;
        this.remainingGold = state.getResourceAmount(playernum, ResourceType.GOLD); //I think this makes a little more sense..

        this.requiredWood = requiredWood;
        this.remainingWood = state.getResourceAmount(playernum, ResourceType.WOOD); //..since we want to know the remaining resources available to the peasant(s)

        this.buildPeasants = buildPeasants;
        this.builtPeasant = false;          //I guess I have not yet built a peasant
        this.costToThisNode = 0d;           //TODO: What does this mean?

        ownedGold = 0; //You initially own nothing.


        //Added code here to determine how many peasants are on the field.
        numPeasants = 0;
        for(int unitId : state.getUnitIds(playernum)) {
            Unit.UnitView unit = state.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("peasant")) {
                numPeasants++;
            } else if(unitType.equals("townhall")) {
                int townhallID = unitId; //I think this will set the townhallID to the townhall's ID. TODO: Right, Aidan?
            }
        }
        System.out.println("There are "+numPeasants+" peasants present.");

        //Add food into the mix here... vvv
        this.amountFood = state.getUnit(townhallID).

    }

    /**
     * myConstructor
     */
    public GameState (GameState parent, double costToMe){
        this.state = parent.state;//I don't know if this is correct.  We may need to generate the new state
        this.playerNum = parent.playerNum;
        this.requiredGold = parent.requiredGold;
        this.requiredWood = parent.requiredWood;
        this.buildPeasants = parent.buildPeasants;
        this.costToThisNode = parent.costToThisNode + costToMe;
        //TODO: keep looking into this.
    }

    /**
     * Unlike in the first A* assignment there are many possible goal states. As long as the wood and gold requirements
     * are met the peasants can be at any location and the capacities of the resource locations can be anything. Use
     * this function to check if the goal conditions are met and return true if they are.
     *
     * @return true if the goal conditions are met in this instance of game state.
     */
    public boolean isGoal() {
        return remainingGold<= 0 && remainingWood <= 0 && builtPeasant==buildPeasants;
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
        // TODO: Implement me!
        return null;
    }

    /**
     * Write your heuristic function here. Remember this must be admissible for the properties of A* to hold. If you
     * can come up with an easy way of computing a consistent heuristic that is even better, but not strictly necessary.
     *
     * Add a description here in your submission explaining your heuristic.
     *
     * @return The value estimated remaining cost to reach a goal state from this state.
     */
    public double heuristic() {
        double heursitic = 0d;
        heursitic += remainingGold / requiredGold;
        heursitic += remainingWood / requiredWood;
        heursitic += buildPeasants==builtPeasant ? 0 : 1;
        return heursitic;
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
        return costToThisNode;
    }

    /**
     * This is necessary to use your state in the Java priority queue. See the official priority queue and Comparable
     * interface documentation to learn how this function should work.
     *
     * @param o The other game state to compare
     * @return 1 if this state costs more than the other, 0 if equal, -1 otherwise
     */
    @Override
    public int compareTo(GameState o) {
        if (this.getCost() < o.getCost()) return -1;
        if (this.getCost() > o.getCost()) return 1;
        return 0;
    }

    /**
     * This will be necessary to use the GameState as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return this.getClass() == o.getClass() && o.hashCode() == this.hashCode();
    }

    /**
     * This is necessary to use the GameState as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        if(builtPeasant){
            return state.hashCode() + remainingGold * 10 + remainingWood;
        } else {
            return state.hashCode() + remainingGold * 10 + remainingWood + 1000;
        }
    }

    //Getters

    public int getPlayerNum(){
        return playerNum;
    }
    public int getRequiredGold(){
        return requiredGold;
    }
    public int getRequiredWood(){
        return requiredWood;
    }
    public int getRemainingGold(){
        return remainingGold;
    }
    public int getRemainingWood(){
        return remainingWood;
    }
    public boolean getBuildPeasants(){
        return buildPeasants;
    }
    public int getNumPeasants(){
        return numPeasants;
    }

}
