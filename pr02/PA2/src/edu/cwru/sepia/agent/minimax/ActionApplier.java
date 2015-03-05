package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;
import edu.cwru.sepia.util.DistanceMetrics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ActionApplier {
    private static final double TREE_CONSTANT = -1;
    private static final double FOOTMEN_HP = 0.2;
    private static final double ARCHER_HP = -0.6;
    private static final double DISTANCE = -0.5;
    private static final double FREEDOM = 0.05;
    
    

    /**
     * entry point for full state generation 
     * @param givenActionMap action map to apply to the state
     * @param givenPreActionState state to apply actions to
     * @return resultant state
     */
    public static State apply(Map<Integer, Action> givenActionMap, State.StateView givenPreActionState){
        State postActionState = null;
        try {
            
            postActionState = givenPreActionState.getStateCreator().createState();
            //TODO: this isn't even duct-tape-quality code...
            //HOW THE HELL DO YOU MAKE A STATE OR STATEVIEW IN A DECENT MANNER?
            //I JUST WANT A NEW STATE OUT OF A STATE + ACTION
            //THIS IS A STUPID AMOUNT OF CODE FOR WHAT SHOULD BE AN API CALL
            
            for(Integer key: givenActionMap.keySet()){//apply all the actions
                applyNextAtomicAction(key, givenActionMap, postActionState);
            }
            
        } catch (IOException e){
            System.err.println("ERROR WHILE CLONING STATE");
        }
        return (postActionState==null)?null: postActionState;
    }

    /**
     *  you give me a unit's ID, the ID/Action map, and the state
     *      I categorize the action and make the proper call to change the state  
     * @param unitID ID of unit performing the action
     * @param actionMap action map containing unit's desired action
     * @param postActionState state to perform the action on
     */
    private static void applyNextAtomicAction(int unitID, Map<Integer, Action> actionMap , State postActionState){
        if(!actionMap.containsKey(unitID)) return;//bad call.
        Action mappedAction = actionMap.get(unitID);
        if(mappedAction instanceof DirectedAction){//PrimitiveMove
            DirectedAction directedAction = (DirectedAction) mappedAction;
            applyMove(unitID, directedAction.getDirection(), postActionState);
        } else if (mappedAction instanceof TargetedAction){//Compound/Primitive Attack
            TargetedAction targetedAction = (TargetedAction) mappedAction;
            if(targetedAction.getType() == ActionType.PRIMITIVEATTACK) {//I'm only using primitive attacks.
                applyAttack(unitID, targetedAction.getTargetId(), postActionState);
            } else{
                System.err.println("ERROR: COULD NOT EXECUTE COMPOUND ACTION: "+targetedAction.toString()+'\n');
            }
        } else{
            System.err.println("ERROR: ACTION COULD NOT BE PARSED\n\t"+mappedAction.toString()+"\n");
        }
    }
    
    /**
     * Applies an attack from the source unit to the destination unit, subtracting health from ths destination unit
     * There is no check for death, I assume that some other portion of the code will handle that. 
     * @param sourceID source unit's ID
     * @param destinationID destination unit's ID
     * @param postActionState state in which to perform the action
     */
    private static void applyAttack(int sourceID, int destinationID, State postActionState){
        Unit source = postActionState.getUnit(sourceID);
        Unit destination = postActionState.getUnit(destinationID);
        int attackAmount = source.getTemplate().getBasicAttack();
        destination.setHP(destination.getCurrentHealth()-attackAmount);
        if(destination.getCurrentHealth() <= 0) postActionState.removeUnit(destinationID);
    }

    /**
     * Applies a move on the source unit in the given direction
     * There is no collision checking, so this may be abused, but error handling exists further up. 
     * @param sourceID source unit's ID
     * @param direction direction to move source
     * @param postActionState state in which to perform the action
     */
    private static void applyMove(int sourceID, Direction direction, State postActionState){
        Unit source = postActionState.getUnit(sourceID);
        postActionState.moveUnit(source, direction);
    }

    /**
     * don't go through the heavy process of cloning a state, just to find its heuristic. 
     * @param givenActionMap actions to take
     * @param givenPreActionState state actions are taken on
     * @return heuristic value
     */
    public static double applyHeuristic(Map<Integer, Action> givenActionMap, State.StateView givenPreActionState){
        List<Unit.UnitView> footmen = new ArrayList<Unit.UnitView>(); //list of all footmen
        Map<Integer, Integer> footmenHP = new HashMap<Integer, Integer>();//ID/HP map for footmen (for temporary calculation)
        List<Unit.UnitView> archers = new ArrayList<Unit.UnitView>(); //list of all archers
        Map<Integer, Integer> archerHP = new HashMap<Integer, Integer>();//ID/HP map for archers
        
        //populate footmen and archers
        parseUnits(givenPreActionState.getAllUnits(), footmen, archers);
        
        //populate their HP maps
        for(Unit.UnitView footman : footmen) footmenHP.put(footman.getID(), footman.getHP());
        for(Unit.UnitView archer : archers) archerHP.put(archer.getID(), archer.getHP());
        
        //apply attacks to HP, and moves to distance
        int distance1 = estimateActionHeuristics(footmen, givenActionMap, archers, archerHP);
        int distance2 = estimateActionHeuristics(archers, givenActionMap, footmen, footmenHP);

        //we don't like trees, so gotta play around with those, too
        List<ResourceNode.ResourceView> trees = givenPreActionState.getAllResourceNodes();
        int treeFactor = generateTreeFactor(footmen, trees);
        
        Freedom freedom = new Freedom();
        for(Unit.UnitView footman: footmen){
            raycastQuadrant(Direction.EAST, Direction.NORTH, 8,0, footman.getXPosition(),
                    footman.getYPosition(), trees, freedom);
            raycastQuadrant(Direction.NORTH, Direction.WEST, 8,0, footman.getXPosition(),
                    footman.getYPosition(), trees, freedom);
            raycastQuadrant(Direction.WEST, Direction.SOUTH, 8,0, footman.getXPosition(),
                    footman.getYPosition(), trees, freedom);
            raycastQuadrant(Direction.SOUTH, Direction.EAST, 8,0, footman.getXPosition(),
                    footman.getYPosition(), trees, freedom);
        }
        
        double correctMovement = estimateMovementHeuristic(givenActionMap, givenPreActionState);

        //sum them all up, and turn it in
        double heuristic = FOOTMEN_HP * sum(footmenHP);
        heuristic += ARCHER_HP * sum(archerHP);
        heuristic += DISTANCE * (distance1+distance2);
        heuristic += TREE_CONSTANT * treeFactor;
        heuristic += FREEDOM * freedom.getFreedom();
        heuristic += correctMovement;
        return heuristic;
    }

    /**
     * estimates the heuristic value of a move along a straight line, taking blocks into consideration 
     * @param givenActionMap unitID/Action tuples to be applied to the state
     * @param givenPreActionState the state being traversed
     * @return heuristic value of the move set
     */
    public static double estimateMovementHeuristic(Map<Integer, Action> givenActionMap, State.StateView givenPreActionState){
        List<ResourceNode.ResourceView> trees = givenPreActionState.getAllResourceNodes();
        int xExtent = givenPreActionState.getXExtent();
        int yExtent = givenPreActionState.getYExtent();
        List<Unit.UnitView> allUnits = givenPreActionState.getAllUnits();
        double returnVar = 0;

        for(Integer i: givenActionMap.keySet()){
            if(givenPreActionState.getUnit(i).getTemplateView().getName().equals("Archer")) continue;
            if(givenActionMap.get(i) instanceof DirectedAction){
                DirectedAction directedAction = (DirectedAction) givenActionMap.get(i);
                Vector bestAttack = new Vector(givenPreActionState.getUnit(i), 
                        closestEnemy(givenPreActionState.getUnit(i), allUnits), trees, xExtent, yExtent);
                List<Direction> directionList = bestAttack.getBestNextDirection();
                for(int j = 0; j < directionList.size(); j++){
                    if (directedAction.getDirection() == directionList.get(j)){
                        switch (j){
                            case 0:
                                returnVar += 5;
                                break;
                            case 1:
                                returnVar += 2;
                                break;
                            case 2:
                                returnVar -=2;
                                break;
                            case 3:
                                returnVar -= 5;
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        }
        return returnVar;
    }

    /**
     * you give me all units, I give you the footmen and archers separated into their lists
     * @param allUnits all units in the state
     * @param footmen unitViews that are footmen (set on return)
     * @param archers unitViews that are archers (set on return)
     */
    private static void parseUnits(List<Unit.UnitView> allUnits, List<Unit.UnitView> footmen, List<Unit.UnitView> archers){
        for (Unit.UnitView unit : allUnits) {//categorize dem units
            if (unit.getTemplateView().getName().equals("Footman")) {
                footmen.add(unit);
            } else if (unit.getTemplateView().getName().equals("Archer")) {
                archers.add(unit);
            }
        }
    }

    /**
     * calculates the tree factor of the footmen
     * the tree factor is the number of trees generally around the unit.  This is a bad thing.
     * @param footmen units in question
     * @param trees resources to enumerate near units in question
     * @return number of trees within TREE_CONSTANT radius of the footmen
     */
    private static int generateTreeFactor(List<Unit.UnitView> footmen, List<ResourceNode.ResourceView> trees){
        int treeFactor = 0;
        for (Unit.UnitView aFootmen : footmen) { //this may not be accounting for dead footmen
            for (ResourceNode.ResourceView tree : trees) {
                if (manhattanImmediate(aFootmen, tree.getXPosition(), tree.getYPosition()) < TREE_CONSTANT) {
                    treeFactor++;
                }
            }
        }
        return treeFactor;
    }

    /**
     * estimates the footmen heuristics (health, damage, distance)
     * returns distance.
     * @param attackers units that are performing actions
     * @param givenActionMap total actions that are being taken in this state
     * @param attackees units that are receiving actions
     * @param attackeeHP HP of units receiving actions (set on return)
     */
    private static int estimateActionHeuristics(List<Unit.UnitView> attackers, Map<Integer, Action> givenActionMap,
                                                 List<Unit.UnitView> attackees, Map<Integer, Integer> attackeeHP){
        int attackerAttackeeDistance = 0;
        for(Unit.UnitView attacker: attackers){
            Action action = givenActionMap.get(attacker.getID());
            if (action instanceof DirectedAction){
                DirectedAction directedAction = (DirectedAction) action;
                int x = directedAction.getDirection().xComponent() + attacker.getXPosition();
                int y = directedAction.getDirection().yComponent() + attacker.getYPosition();
                int minDistance = Integer.MAX_VALUE;
                Unit.UnitView closestAttackee = null;
                for (Unit.UnitView attackee : attackees) {
                    int currentDistance = manhattanImmediate(attackee, x,y);
                    if (currentDistance < minDistance) {
                        minDistance = currentDistance;
                        closestAttackee = attackee;
                    }
                }
                attackerAttackeeDistance += minDistance;
                
            } else if (action instanceof TargetedAction){
                TargetedAction targetedAction = (TargetedAction) action;
                attackeeHP.put(targetedAction.getTargetId(), attackeeHP.get(targetedAction.getTargetId())
                        - attacker.getTemplateView().getBasicAttack());
            }
        }
        return attackerAttackeeDistance;
    }
    /**
     * manhattan Distance with an immediate location.  Useful for calculating distance without moving unit 
     * @param loc1 location of unit 1
     * @param x x location of unit 2
     * @param y y location of unit 2
     * @return manhattan distance between points
     */
    private static int manhattanImmediate(Unit.UnitView loc1, int x, int y){
        return Math.abs(loc1.getXPosition() - x) +  Math.abs(loc1.getYPosition() - y);
    }

    /**
     * sums the values in the given set 
     * @param toSum set to sum
     * @return the sum of the set's values
     */
    private static int sum(Map<Integer, Integer> toSum){
        if(toSum == null || toSum.keySet().isEmpty()) return 0;
        int returnVar = 0;
        for(Integer key : toSum.keySet()) {
            Integer value = toSum.get(key);
            returnVar += value == null ? 0 : value;//There should be no reason for this to happen.
        }
        return returnVar;
    }

    /**
     * performs a recursive 2D raycast(ish) search of a quadrant.  I'm proud of this one
     * @param primary initial search direction
     * @param secondary direction 90 degrees to the left of primary
     * @param radius search depth
     * @param depth current depth in the search
     * @param startX initial searching source X position
     * @param startY initial searching source Y position
     * @param trees list of blocks on the map
     * @param freedom an Object (byRef) to represent the freedom (free spots I can see before a wall)
     */
    private static void raycastQuadrant(Direction primary, Direction secondary, int radius, int depth,
                                 int startX, int startY, List<ResourceNode.ResourceView> trees, Freedom freedom){
        if(isBlocked(startX, startY, trees) || depth>radius)return;//don't wanna start off on the wrong foot now
        int i = 0;
        while(!isBlocked(startX + primary.xComponent() * i, startY + primary.yComponent() * i, trees) && i <=radius){
            freedom.add();
            i++;
        }
        i = 0;
        while(isBlocked(startX + primary.xComponent() * i + secondary.xComponent(),
                startY + primary.yComponent() * i + secondary.yComponent(), trees)){
            i++;
        }
        //recurse
        raycastQuadrant(secondary, primary, radius, depth + 1,startX+primary.xComponent()*i+secondary.xComponent(),
                startY+primary.yComponent()*i+secondary.yComponent(), trees, freedom);
    }

    /**
     *  you give me a position, and a list of things that could be blocking you,
     *  I tell you whether one of the blocks is in that position  
     * @param x x coordinate in question
     * @param y y coordinate in question
     * @param blocks list of things that could block that position
     * @return whether the position is blocked by anything on the blocks list
     */
    private static boolean isBlocked(int x, int y, List<ResourceNode.ResourceView> blocks){
        for(ResourceNode.ResourceView block:blocks){
            if(block.getXPosition() == x && block.getYPosition() == y) return true;
        }
        return false;
    }

    /**
     * wrapper for DistanceMetric's chebyshev calculator 
     * @param loc1 unitView 1
     * @param loc2 unitView 2
     * @return chebyshev distance between the units
     */
    private static int chebyshevDistance(Unit.UnitView loc1, Unit.UnitView loc2){
        return DistanceMetrics.chebyshevDistance(loc1.getXPosition(), loc1.getYPosition(), loc2.getXPosition(), loc2.getYPosition());
    }

    /**
     * You give me a source unit, I give you the closest unit that's not my type
     * @param source unit to search from
     * @param units all units on map
     * @return closest unit
     */
    private static Unit.UnitView closestEnemy(Unit.UnitView source, List<Unit.UnitView> units){
        int minDistance = Integer.MAX_VALUE;
        Unit.UnitView minEnemy = null;
        for(Unit.UnitView possibleEnemy: units){
            if(possibleEnemy.getTemplateView().getName().equals(source.getTemplateView().getName())) continue;
            if(chebyshevDistance(source, possibleEnemy) < minDistance){
                minDistance = chebyshevDistance(source, possibleEnemy);
                minEnemy = possibleEnemy;
            }
        }
        return minEnemy;
    }
}
