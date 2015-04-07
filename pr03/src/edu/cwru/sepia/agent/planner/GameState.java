package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.util.*;

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
 * I recommend storing the actions that generated the instance of the GameState_old in this class using whatever
 * class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {
    //Fields, these should be private

    private final State.StateView state;    //The StateView of the world which allows us to query the actual "state" of SEPIA
    private final int playerNum;            //The player number of the agent that is planning TODO: What is this?  Do we need it?

    private final int requiredWood;         //The goal amount of wood we need to win the game (which you just lost)
    private int woodOnField;                //The amount of wood that is left on the map
    private int ownedWood;                  //The amount of wood we have, updated by DepositAction
    private final int requiredGold;         //The amount of gold we need to win the game
    private int goldOnField;                //How much gold that is left on the map
    private int ownedGold;                  //The amount of gold we have, this will be updated by the DepositAction
    
    private final int requiredPeasants;     //Whether or not we're going to be building peasants in this scenario
    private int ownedPeasants;              //Did we build a peasant yet?   Is this necessary?
                                                                            //DONE: See constructor.  We receive it as a boolean,
                                                //so we can track the number we have and the number required if you'd rather.
    private boolean buildPeasants;

    private ArrayList<ExistentialPeasant> peasantTracker;
    private ArrayList<ExistentialGoldMine> goldMineTracker;
    private ArrayList<ExistentialForest> forestTracker;
    private ExistentialTownHall townhall;

    private int numPeasants = peasantTracker.size();
    private int unusedFood;                 //Ya gotta eat.  But only 3 at a time

    private double costToThisNode; //TODO: What does this even do? TODONE: this is the g(x) value in A*
    private StripsAction parentAction = null;
    private GameState parentState = null;

    //Heuristic Stuff
    private double c;
    private double h = -1;
    
    /**
     * Construct a GameState_old from a stateview object. This is used to construct the initial search node. All other
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
        this.goldOnField = state.getResourceAmount(playernum, ResourceType.GOLD); //I think this makes a little more sense..
        this.requiredWood = requiredWood;
        this.woodOnField = state.getResourceAmount(playernum, ResourceType.WOOD); //..since we want to know the remaining resources available to the peasant(s)
        this.ownedPeasants = state.getUnits(playernum).size();
        this.requiredPeasants = ownedPeasants + (buildPeasants ? 1 : 0);
        this.buildPeasants = buildPeasants;
        this.ownedGold = 0; //You initially own nothing...
        this.ownedWood = 0;
        this.costToThisNode = 0d;
        this.peasantTracker = new ArrayList<>();
        this.goldMineTracker = new ArrayList<>();
        this.forestTracker = new ArrayList<>();
        

        //Added code here to determine how many peasants are on the field.
        this.numPeasants = 0;
        for(int unitId : state.getUnitIds(playernum)) {
            Unit.UnitView unit = state.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            
            switch (unitType) {
                case "peasant":
                    peasantTracker.add(new ExistentialPeasant(unit.getCargoType(), unit.getCargoAmount() == 0, peasantTracker.size()));
                    break;
                case "townhall":
                    townhall = new ExistentialTownHall(unit.getCargoAmount() == 0); //TODO: Does it make sense to
                    break;
                case "forest":
                    forestTracker.add(new ExistentialForest(unit.getCargoAmount() == 0, unit.getCargoAmount()));
                    break;
                case "goldmine":
                    goldMineTracker.add(new ExistentialGoldMine(unit.getCargoAmount() == 0, unit.getCargoAmount()));
                    break;
                default:
                    System.out.println(unitType + "LOOK AT ME IM MR MEESEEKS: YOU SCREWED UP YOUR STRING EQUALS CHECKING IN GAMESTATE CONSTRUCTOR.");
                    break;
            }
        }
        this.unusedFood = state.getSupplyCap(playernum) - ownedPeasants; //TODO: Does playernum make sense here?  TODONE: yes.


        c = 0; //Cost thing for AStar is initially 0
    }

    /**
     * myConstructor, used as you traverse the state space during A
     * Updates the values of wood/gold on field (this requires the parent to change their goldMine/forestTracker)
     */
    public GameState(GameState parent, double costToMe, StripsAction parentAction){
        this(parent.state, parent.getPlayerNum(), parent.getRequiredGold(), parent.getRequiredWood(), 
                parent.getRequiredPeasants() == parent.ownedPeasants);
        
        this.costToThisNode = parent.costToThisNode + costToMe;
        woodOnField = goldOnField = 0;
        for(ExistentialForest forest : parent.forestTracker) woodOnField += forest.getAmountCargo();
        for(ExistentialGoldMine goldMine : parent.goldMineTracker) goldOnField += goldMine.getAmountCargo();
        this.parentAction = parentAction;
        this.parentState = parent;
        c = parent.c + costToMe; //Updating cost here when a new GameState is created from a StripsAction being performed.
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
        return this.requiredGold <= this.ownedGold && 
                this.requiredWood <= this.ownedWood && 
                this.requiredPeasants <= this.ownedPeasants;
    }

    /**
     * The branching factor of this search graph are much higher than the planning. Generate all of the possible
     * successor states and their associated actions in this method.
     *
     * @return A list of the possible successor states and their associated actions
     */
    public List<GameState> generateChildren() {
        List<GameState> children = new ArrayList<>();
        for(ExistentialPeasant peasant: peasantTracker){
            
            if(HarvestAction.canHarvest(peasant, this, ResourceType.WOOD)){
                HarvestAction harvestAction = new HarvestAction(peasant.getPeasantID(), ResourceType.WOOD);
                children.add(harvestAction.apply(this));
            } else if (HarvestAction.canHarvest(peasant, this, ResourceType.GOLD)){
                HarvestAction harvestAction = new HarvestAction(peasant.getPeasantID(), ResourceType.GOLD);
                children.add(harvestAction.apply(this));
            }
            //you can always move to wood, gold, or the townhall, so unconditionally add them.
            children.add(new MoveAction(peasant.getPeasantID(), ResourceType.WOOD).apply(this));
            children.add(new MoveAction(peasant.getPeasantID(), ResourceType.GOLD).apply(this));
            children.add(new MoveAction(peasant.getPeasantID()).apply(this));

            //TODO: fix this for the static preconditionsMet implementation
            DepositAction depositAction = new DepositAction(peasant);
            if(depositAction.preconditionsMet(this)) children.add(depositAction.apply(this));
            
            //TODO: fix this for the static preconditionsMet implementation
            CreateAction createAction = new CreateAction();
            if(createAction.preconditionsMet(this)) children.add(createAction.apply(this));
        }
        return children;
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
        double h = 0;
        if(parentAction.name.equals("MOVE")){
            //Move Action Handling

            return h;
        } else if(parentAction.name.equals("HARVEST")){
            //Harvest Action Handling

            return h;
        } else if(parentAction.name.equals("DEPOSIT")){
            //Deposit Action Handling

            return h;
        } else if(parentAction.name.equals("CREATE")){
            //Create Action Handling

            return h;
        } else return h;

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
     * This will be necessary to use the GameState_old as a key in a Set or Map.
     *
     * @param o The game state to compare
     * @return True if this state equals the other state, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        return this.getClass() == o.getClass() && o.hashCode() == this.hashCode();
    }

    /**
     * This is necessary to use the GameState_old as a key in a HashSet or HashMap. Remember that if two objects are
     * equal they should hash to the same value.
     *
     * @return An integer hashcode that is equal for equal states.
     */
    @Override
    public int hashCode() {
        return goldMineTracker.hashCode()*3 + forestTracker.hashCode()*5 + peasantTracker.hashCode()*7;
    }

    //Methods for use by CreateAction
    public void removeOneFood(){
        unusedFood--;
    }

    public void addPeasant(){
        peasantTracker.add(new ExistentialPeasant(null, false, peasantTracker.size()));
        ownedPeasants++;
    }

    //Methods for use by DepositAction, and I swear I'm cool.
    public void addToOwnedGold(){
        ownedGold = ownedGold + 100;
    }
    public void addToOwnedWood(){
        ownedWood = ownedWood + 100;
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
    public int getOwnedGold(){
        return ownedGold;
    }
    public int getOwnedWood() { return ownedWood; }
    public int getGoldOnField() { return goldOnField; }
    public int getWoodOnField(){
        return woodOnField;
    }
    public int getRequiredPeasants(){
        return requiredPeasants;
    }
    public int getNumPeasants(){
        return numPeasants;
    }
    public void setWoodOnField(int woodOnField) {
        this.woodOnField = woodOnField;
    }
    public void setGoldOnField(int goldOnField) {
        this.goldOnField = goldOnField;
    }
    public void setOwnedWood(int ownedWood) {
        this.ownedWood = ownedWood;
    }
    public void setOwnedGold(int ownedGold) {
        this.ownedGold = ownedGold;
    }
    public StripsAction getParentAction() { return this.parentAction;}
    public boolean getBuildPeasants(){
        return buildPeasants;
    }
    public int getUnusedFood() { return unusedFood; }
    public ArrayList<ExistentialPeasant> getPeasantTracker() { return peasantTracker; }
    public ArrayList<ExistentialGoldMine> getGoldMineTracker() { return goldMineTracker; }
    public ArrayList<ExistentialForest> getForestTracker() { return forestTracker; }
    public ExistentialTownHall getTownhall() { return townhall; }
    public GameState getParentState(){ return this.parentState; }

    /**
     * I am the abstract representation of everything in the map.
     * Goldmines, Forests, Peasants, and TownHalls all extend me. 
     * I have a Position and an amount of cargo.  You can check if I'm beside another ExistentialBeing.
     */
    public abstract class ExistentialBeing{
        boolean  hasCargo;
        public ExistentialBeing(boolean hasCargo){
            this.hasCargo = hasCargo;
        }
        public boolean getHasCargo() { return hasCargo; }

    }
    
    public class ExistentialPeasant extends ExistentialBeing {
        private int peasantID;
        boolean besideGold; //i'm here, I promise
        boolean besideWood;
        boolean besideTH;
        boolean hasGold;
        boolean hasWood;
        
        private ResourceType cargoType;

        public ExistentialPeasant(ResourceType cargoType, boolean hasCargo, int pID) {
            super(hasCargo);
            this.cargoType = cargoType;
            this.peasantID = pID; //ExistentialPeasant ID's are their location in the arrayList-1.  So the 0th peasant has a peasant ID of 1, and so on.
        }
        public int getPeasantID() { return this.peasantID; }
        public ResourceType getCargoType() {
            return cargoType;
        }
        
        public boolean isHasWood() {
            return hasWood;
        }

        public void setHasWood(boolean hasWood) {
            this.hasWood = hasWood;
        }

        public boolean isHasGold() {
            return hasGold;
        }

        public void setHasGold(boolean hasGold) {
            this.hasGold = hasGold;
        }

        public boolean isBesideTH() {
            return besideTH;
        }

        public boolean isBesideWood() {
            return besideWood;
        }

        public boolean isBesideGold() {
            return besideGold;
        }

        public void setBesideGold(boolean besideGold) {
            this.besideGold = besideGold;
        }

        public void setBesideWood(boolean besideWood) {
            this.besideWood = besideWood;
        }

        public void setBesideTH(boolean besideTH) {
            this.besideTH = besideTH;
        }

        //Method to reset booleans to all false after applying an action to easily update booleans
        public void resetBools(){
            besideGold = false;
            besideWood = false;
            besideTH = false;
            hasGold = false;
            hasWood = false;
        }

        public void setCargoType(ResourceType cargoType) {
            this.cargoType = cargoType;
        }
    }

    public class ExistentialForest extends ExistentialBeing{
        private final ResourceType cargoType;
        private int amountCargo;
        public ExistentialForest(boolean hasCargo, int amountCargo){
            super(hasCargo);
            this.cargoType = ResourceType.WOOD;
        }

        public int getAmountCargo(){
            return amountCargo;
        }
    }

    public class ExistentialGoldMine extends ExistentialBeing{
        private final ResourceType cargoType;
        private int amountCargo;
        public ExistentialGoldMine(boolean hasCargo, int amountCargo){
            super(hasCargo);
            this.cargoType = ResourceType.GOLD;
        }
        
        public int getAmountCargo(){
            return amountCargo;
        }
    }

    public class ExistentialTownHall extends ExistentialBeing{
        //NOTE: the 'amountCargo' inherited from 'ExistentialBeing' is the amount of food.
        public ExistentialTownHall(boolean hasCargo){
            super(hasCargo);
        }
    }

}
