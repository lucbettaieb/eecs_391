package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.util.*;

/**
 * GameState.java
 * A class to represent the state of the game.
 *
 * Aidan Campbell and Luc Bettaieb
 */

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
    private int ownedWood = 0;                  //The amount of wood we have, updated by DepositAction
    private final int requiredGold;         //The amount of gold we need to win the game
    private int goldOnField;                //How much gold that is left on the map
    private int ownedGold = 0;                  //The amount of gold we have, this will be updated by the DepositAction
    
    private final int requiredPeasants;     //Whether or not we're going to be building peasants in this scenario
    private int ownedPeasants;              //how many peasants we currently have
    private boolean buildPeasants;

    private ArrayList<ExistentialPeasant> peasantTracker;
    private ArrayList<ExistentialGoldMine> goldMineTracker;
    private ArrayList<ExistentialForest> forestTracker;
    private ExistentialTownHall townhall;

    private int numPeasants;
    private int unusedFood;                 //Ya gotta eat.  But only 3 at a time

    private StripsAction parentAction = null;
    private GameState parentState = null;

    //Heuristic Stuff
    private double c = 0d;
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
        peasantTracker = new ArrayList<>();
        goldMineTracker = new ArrayList<>();
        forestTracker = new ArrayList<>();
        this.state = state;
        List<ResourceNode.ResourceView> resources = state.getAllResourceNodes();
        this.playerNum = playernum;

        this.requiredGold = requiredGold;
        this.goldOnField = this.woodOnField = 0;
        for(ResourceNode.ResourceView resource : resources){
            if (resource.getType() == ResourceNode.Type.GOLD_MINE) {
                goldOnField += resource.getAmountRemaining();
                goldMineTracker.add(new ExistentialGoldMine(true, resource.getAmountRemaining()));
            }
            if (resource.getType() == ResourceNode.Type.TREE) {
                woodOnField += resource.getAmountRemaining();
                forestTracker.add(new ExistentialForest(true, resource.getAmountRemaining()));
            }
        }
        this.requiredWood = requiredWood;
        this.ownedPeasants = state.getUnits(playernum).size();
        this.requiredPeasants = ownedPeasants + (buildPeasants ? 1 : 0);
        this.buildPeasants = buildPeasants;
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
        this.unusedFood = state.getSupplyCap(playernum) - ownedPeasants;
        numPeasants = peasantTracker.size();
        c = 0; //Cost thing for AStar is initially 0
    }

    /**
     * myConstructor, used as you traverse the state space during A
     * Updates the values of wood/gold on field (this requires the parent to change their goldMine/forestTracker)
     */
    public GameState(GameState parent, double costToMe, StripsAction parentAction){
        this(parent.state, parent.getPlayerNum(), parent.getRequiredGold(), parent.getRequiredWood(), 
                parent.getRequiredPeasants() != parent.ownedPeasants);
        
        this.parentAction = parentAction;
        this.parentState = parent;
        this.ownedGold = parent.ownedGold;
        this.ownedWood = parent.ownedWood;
        this.ownedPeasants = parent.ownedPeasants;
        this.c = parent.c + costToMe; //Updating cost here when a new GameState is created from a StripsAction being performed.


        //TODO: Do we still need to keep looking in to this?
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
        //TODO: because we're planning for each peasant, things get funny
        List<GameState> children = new ArrayList<>();
        
        //ExistentialPeasant peasant;
        //if(peasantTracker.size()>0)peasant = peasantTracker.get(0);
        //else return new ArrayList<>();
        
        for(ExistentialPeasant peasant : peasantTracker) {
            if (HarvestAction.canHarvest(peasant, this, ResourceType.WOOD)) {
                HarvestAction harvestAction = new HarvestAction(peasant, ResourceType.WOOD);
                children.add(harvestAction.apply(this));
            } else if (HarvestAction.canHarvest(peasant, this, ResourceType.GOLD)) {
                HarvestAction harvestAction = new HarvestAction(peasant, ResourceType.GOLD);
                children.add(harvestAction.apply(this));
            }
            //you can always move to wood, gold, or the townhall, so unconditionally add them.
            if (parentAction == null || !parentAction.getName().equals("MOVE")) {
                //if I didn't have a parent, or my parent was not MOVE, add these move commands.
                //don't double move, and don't move to the same place.
                if (MoveAction.canMove(peasant, ResourceType.WOOD))
                    children.add(new MoveAction(peasant, ResourceType.WOOD).apply(this));
                if (MoveAction.canMove(peasant, ResourceType.GOLD))
                    children.add(new MoveAction(peasant, ResourceType.GOLD).apply(this));
                if (MoveAction.canMove(peasant, null)) children.add(new MoveAction(peasant).apply(this));
            }

            DepositAction depositAction = new DepositAction(peasant);
            if (depositAction.preconditionsMet(this)) children.add(depositAction.apply(this));

            CreateAction createAction = new CreateAction();
            if (createAction.preconditionsMet(this)) children.add(createAction.apply(this));
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

    //TODO: Make this better!
    public double heuristic() {
        //return badHeuristic();
        return goodHeuristic();
    }
    
    public double goodHeuristic(){
        /*
        to get gold:
        MOVE -> HARVEST -> MOVE -> DEPOSIT
        e.g. 4 steps.
         */
        int peasantsLeft = requiredPeasants - ownedPeasants;
        switch (parentAction.getName().toLowerCase()){
            case "move":
                h += 2;
                h += ((requiredGold - ownedGold)/100)*4;//how many loads of gold are left
                h += ((requiredWood - ownedWood)/100)*4;
                if(peasantsLeft > 0) h += 16;//4 loads of gold to make a peasant.
                break;
            case "harvest":
                h += 3;
                h += ((requiredGold - ownedGold)/100)*4;
                h += ((requiredWood - ownedWood)/100)*4;
                if(peasantsLeft > 0) h += 16;//4 loads of gold to make a peasant.
                break;
            case "deposit":
                h += ((requiredGold - ownedGold)/100)*4;
                h += ((requiredWood - ownedWood)/100)*4;
                if(peasantsLeft > 0) h += 16;//4 loads of gold to make a peasant.
                break;
            case "create":
                h += ((requiredGold - ownedGold)/100)*4;
                h += ((requiredWood - ownedWood)/100)*4;
                if(peasantsLeft > 0) h += 16;//4 loads of gold to make a peasant.
                break;
            default:
                System.err.println("ERROR! UNKNOWN ACTION ENCOUNTERED IN GAMESTATE!");
                break;
        }
        return h;
    }

    /**
     *
     * Write the function that computes the current cost to get to this node. This is combined with your heuristic to
     * determine which actions/states are better to explore.
     *
     * @return The current cost to reach this goal
     */
    public double getCost() {
        return (int) (this.c + this.heuristic());
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

    /**
     * Removes one food for when a peasant is created.
     */
    public void removeOneFood(){
        unusedFood--;
    }

    /**
     * Adds a peasant to the peasant tracker.  TODO: This method may be underused.
     * No cargo initially, so null cargo type, false for having cargo.  Peasant ID is it's index location in the ArrayList+1.
     */
    public void addPeasant(){
        peasantTracker.add(new ExistentialPeasant(null, false, peasantTracker.size()));
        ownedPeasants++;
    }

    /**
     * Deposit action methods.  Will add 100 to ownedGold or ownedWood
     */
    public void addToOwnedGold(){
        ownedGold += 100;
    }
    public void addToOwnedWood(){
        ownedWood += 100;
    }

    /**
     * Getters and setters.
     */
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
    public int getOwnedWood() { return ownedWood; } //TODO: We may not need this.
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
    public int getF(){
        return (int) (this.c + this.heuristic());
    }

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

    /**
     * An existential peasant.  Used to track what the peasants on the map are doing for planning purposes.
     */
    public class ExistentialPeasant extends ExistentialBeing {
        private int peasantID;
        boolean besideGold;
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
            if(cargoType == null){
                if(hasGold && !hasWood) cargoType = ResourceType.GOLD;
                if(hasWood && !hasGold) cargoType = ResourceType.WOOD;
            }
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
        
        @Override
        public String toString(){
            StringBuilder sb = new StringBuilder();
            sb.append("\tID: ").append(peasantID).append("\n\tCargo Type: ").append(cargoType == null?"NULL": cargoType.toString());
            sb.append("\n\tHolding Wood: ").append(hasWood).append("\n\tHolding Gold: ").append(hasGold);
            sb.append("\n\tBeside Gold: ").append(besideGold).append("\n\tBeside Wood: ");
            sb.append(besideWood).append("\n\tBeside TH: ").append(besideTH).append('\n');
            return sb.toString();
        }
    }

    /**
     * An existential forest to keep track of forest locations and how much wood they have left.
     */
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

    /**
     * An existential gold mind (lol) to keep track of gold mine locations and how much gold they have left.
     */
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

    /**
     * An existential town hall to keep track of where the town hall is... or something.
     * NOTE: the 'amountCargo' inherited from 'ExistentialBeing' is the amount of food.
     */
    public class ExistentialTownHall extends ExistentialBeing{
        public ExistentialTownHall(boolean hasCargo){
            super(hasCargo);
        }
    }

    
    public ExistentialPeasant initializePeasant(){
        ExistentialPeasant newPeasent = new ExistentialPeasant(null, false, this.peasantTracker.size());
        this.peasantTracker.add(newPeasent);
        return newPeasent;
    }
}
