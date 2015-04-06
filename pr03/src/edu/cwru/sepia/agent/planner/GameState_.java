package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public class GameState_ implements Comparable<GameState_> {
    //Fields, these should be private

    private final State.StateView state; //The StateView of the world which allows us to query the actual "state" of SEPIA
    private final int playerNum;         //The player number of the agent that is planning TODO: What is this?  Do we need it?

    private final int requiredWood;      //The goal amount of wood we need to win the game (which you just lost)
    private int woodOnField;           //The amount of wood that is left on the map
    private int ownedWood;               //The amount of wood we have, updated by DepositAction
    private final int requiredGold;      //The amount of gold we need to win the game
    private int remainingGold;           //How much gold that is left on the map
    private int goldOnField;               //The amount of gold we have, this will be updated by the DepositAction
    
    private final int requiredPeasants; //Whether or not we're going to be building peasants in this scenario
    private int ownedPeasants;        //Did we build a peasant yet? TODO: Is this necessary? 
                                                //TODO: TODONE: See constructor.  We receive it as a boolean,
                                                //so we can track the number we have and the number required if you'd rather.

    private ArrayList<ExistentialPeasant> peasantTracker;
    private ArrayList<ExistentialGoldMine> goldMineTracker;
    private ArrayList<ExistentialForest> forestTracker;

    private ExistentialTownHall townhall;

    private int numPeasants = peasantTracker.size();    //How many peasants?  Never too many.  >3 peasants spoil the broth.
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
    public GameState_(State.StateView state, int playernum, int requiredGold, int requiredWood, boolean buildPeasants){
        this.state = state;
        this.playerNum = playernum;

        this.requiredGold = requiredGold;
        this.remainingGold = state.getResourceAmount(playernum, ResourceType.GOLD); //I think this makes a little more sense..

        this.requiredWood = requiredWood;
        this.woodOnField = state.getResourceAmount(playernum, ResourceType.WOOD); //..since we want to know the remaining resources available to the peasant(s)

        this.ownedPeasants = state.getUnits(playernum).size();
        this.requiredPeasants = ownedPeasants + (buildPeasants ? 1 : 0);
        
        this.costToThisNode = 0d;           //TODO: What does this mean? TODONE: A*'s g(x) value.

        this.goldOnField = 0; //You initially own nothing...
        this.ownedWood = 0;

        //Added code here to determine how many peasants are on the field.
        this.numPeasants = 0;
        for(int unitId : state.getUnitIds(playernum)) {
            Unit.UnitView unit = state.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            
            switch (unitType) {
                case "peasant":
                    peasantTracker.add(new ExistentialPeasant(unit.getXPosition(), unit.getYPosition(), unit.getCargoType(), unit.getCargoAmount(), peasantTracker.size() + 1));
                    break;
                case "townhall":
                    townhall = new ExistentialTownHall(unit.getXPosition(), unit.getYPosition(), unit.getCargoAmount()); //TODO: Does it make sense to
                    break;
                case "forest":
                    forestTracker.add(new ExistentialForest(unit.getXPosition(), unit.getYPosition(), unit.getCargoAmount()));
                    break;
                case "goldmine":
                    goldMineTracker.add(new ExistentialGoldMine(unit.getXPosition(), unit.getYPosition(), unit.getCargoAmount()));
                    break;
                default:
                    System.out.println(unitType + "LOOK AT ME IM MR MEESEEKS: YOU SCREWED UP YOUR STRING EQUALS CHECKING IN GAMESTATE CONSTRUCTOR.");
                    break;
            }
        }
        this.amountFood = state.getSupplyCap(playernum); //TODO: Does playernum make sense here?  TODONE: yes.
    }

    /**
     * myConstructor, used as you traverse the state space during A*
     */
    public GameState_(GameState_ parent, double costToMe){
        this.state = parent.state;//I don't know if this is correct.  We may need to generate the new state
        this.playerNum = parent.playerNum;
        this.requiredGold = parent.requiredGold;
        this.requiredWood = parent.requiredWood;
        this.requiredPeasants = parent.requiredPeasants;
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
        return remainingGold<= 0 && woodOnField <= 0 && ownedPeasants == requiredPeasants;
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState_> generateChildren() {
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
        //TODO: this is a bad heuristic.  Use distance to next destination, and other things.
        double heursitic = 0d;
        heursitic += remainingGold / requiredGold;
        heursitic += woodOnField / requiredWood;
        heursitic += requiredPeasants == ownedPeasants ? 0 : 1;
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
    public int compareTo(GameState_ o) {
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
        return remainingGold * 31 + woodOnField + peasantTracker.hashCode();
    }

    //Methods for use by CreateAction
    public void removeOneFood(){
        amountFood--;
    }

    public void addPeasant(){
        int x = townhall.position.x + 1;
        int y = townhall.position.y + 1;
        peasantTracker.add(new ExistentialPeasant(x, y, null, 0, peasantTracker.size()));
    }

    public void iBuiltAPeasant(){ ownedPeasants++; }

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
    public int getWoodOnField(){
        return woodOnField;
    }
    public int getRequiredPeasants(){
        return requiredPeasants;
    }
    public int getNumPeasants(){
        return numPeasants;
    }
    public int getAmountFood() { return amountFood; }
    public ArrayList<ExistentialPeasant> getPeasantTracker() { return peasantTracker; }
    public ArrayList<ExistentialGoldMine> getGoldMineTracker() { return goldMineTracker; }
    public ArrayList<ExistentialForest> getForestTracker() { return forestTracker; }
    public ExistentialTownHall getTownhall() { return townhall; }

    public abstract class ExistentialBeing{
        Position position;
        int amountCargo;
        public ExistentialBeing(int xPos, int yPos, int amountCargo){
            this.amountCargo = amountCargo;
            this.position = new Position(xPos, yPos);
        }
        public int getAmountCargo() { return amountCargo; }

        public boolean isNextTo(ExistentialBeing other){
            return this.position.isAdjacent(other.position);
        }

    }


    public class ExistentialPeasant extends ExistentialBeing{
        private int peasantID;
        private ResourceType cargoType;

        public ExistentialPeasant(int xPos, int yPos, ResourceType cargoType, int amountCargo, int pID) {
            super(xPos, yPos, amountCargo);
            this.cargoType = cargoType;
            this.peasantID = pID; //ExistentialPeasant ID's are their location in the arrayList-1.  So the 0th peasant has a peasant ID of 1, and so on.
        }
        public int getPeasantID() { return this.peasantID; }
        public int getAmountWood(){
            return cargoType.equals(ResourceType.WOOD) ? amountCargo : 0;
        }
        public int getAmountGold(){
            return cargoType.equals(ResourceType.GOLD) ? amountCargo : 0;
        }
        public int removeAmount(int amountToRemove){
            return (amountCargo -= amountToRemove);
        }
        public void harvestForest(ExistentialForest harvestingLocation){
            if(this.amountCargo > 0){
                System.err.println("ERROR! PEASANT ATTEMPTED TO HARVEST WHILE CARRYING CARGO!");
            } else {
                this.amountCargo = harvestingLocation.harvest();
                this.cargoType = ResourceType.WOOD;
            }
        }
        public void harvestGold(ExistentialGoldMine harvestingLocation){
            if(this.amountCargo > 0){
                System.err.println("ERROR! PEASANT ATTEMPTED TO HARVEST WHILE CARRYING CARGO!");
            } else {
                this.amountCargo = harvestingLocation.harvest();
                this.cargoType = ResourceType.GOLD;
            }
        }
        public void depositCargo(ExistentialTownHall depositLocation){
            depositLocation.depositCargo(amountCargo, cargoType);
        }
    }

    public class ExistentialForest extends ExistentialBeing{
        private ResourceType cargoType;
        public ExistentialForest(int xPos, int yPos, int amountCargo){
            super(xPos, yPos, amountCargo);
            this.cargoType = ResourceType.WOOD;
        }

        /**
         * does a harvest on the forest, assuming 100 to be harvested each time.
         * @return amount of resource harvested from this location.
         */
        public int harvest(){
            if(this.amountCargo <= 0){
                System.err.println("ERROR! ATTEMPT TO HARVEST FOREST WHEN NO RESOURCES REMAINED.");
                return 0;
            } else if(this.amountCargo < 100){
                int returnVar = amountCargo;
                amountCargo = 0;
                return returnVar;
            } else {
                amountCargo -= 100;
                return 100;
            }
        }
    }

    public class ExistentialGoldMine extends ExistentialBeing{
        private ResourceType cargoType;
        public ExistentialGoldMine(int xPos, int yPos, int amountCargo){
            super(xPos, yPos, amountCargo);
            this.cargoType = ResourceType.GOLD;
        }

        /**
         * does a harvest on the gold mine, assuming 100 to be harvested each time.
         * @return amount of resource harvested from this location.
         */
        public int harvest(){
            if(this.amountCargo <= 0){
                System.err.println("ERROR! ATTEMPT TO HARVEST GOLD MINE WHEN NO RESOURCES REMAINED.");
                return 0;
            } else if(this.amountCargo < 100){
                int returnVar = amountCargo;
                amountCargo = 0;
                return returnVar;
            } else {
                amountCargo -= 100;
                return 100;
            }
        }
    }

    public class ExistentialTownHall extends ExistentialBeing{
        Map<ResourceType, Integer> resourceMap;
        public ExistentialTownHall(int xPos, int yPos, int amountCargo){
            super(xPos, yPos, amountCargo);
            this.resourceMap = new HashMap<>();
        }

        /**
         * puts 'amountToDeposit' cargo of type 'cargoType' into the townhall
         * @param amountToDeposit amount of cargo to deposit
         * @param cargoType type of cargo to deposit (WOOD, GOLD)
         */
        public void depositCargo(int amountToDeposit, ResourceType cargoType){
            if(!resourceMap.containsKey(cargoType)) resourceMap.put(cargoType, amountToDeposit);
            else resourceMap.replace(cargoType, resourceMap.get(cargoType)+amountToDeposit);
        }
        

        /**
         * @return true if a new Peasant can be created
         */
        public boolean canICreatePeasant(){
            //I have to have gold, and I have to have at least 400 gold, and I have to have at least 3 food.
            return resourceMap.containsKey(ResourceType.GOLD) && resourceMap.get(ResourceType.GOLD)>=400 && amountCargo>=3;
        }
    }
}
