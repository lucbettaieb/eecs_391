/*
        TODO LIST
    -break down compound actions into their atomic (called primitive) forms
    -remove player if its HP is at or below zero
 */
package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.DirectedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by aidan on 3/1/15.
 */
public class ActionApplier {

    
    public static State.StateView apply(Map<Integer, Action> givenActionMap, State.StateView givenPreActionState){
        State postActionState = null;
        try {
            
            postActionState = givenPreActionState.getStateCreator().createState();
            //TODO: this isn't even duct-tape-quality code...
            //HOW THE HELL DO YOU MAKE A STATE OR STATEVIEW IN A DECENT MANNER?
            //I JUST WANT A NEW STATE OUT OF A STATE + ACTION
            
            for(Integer key: givenActionMap.keySet()){//apply all the actions
                applyNextAtomicAction(key, givenActionMap, postActionState);
            }
            
        } catch (IOException e){
            System.err.println("ERROR WHILE CLONING STATE");
            //System.exit(1);
        }
        
        
        return (postActionState==null)?null: postActionState.getView(givenPreActionState.getPlayerNumbers()[0]);
    }
    
    
    private static void applyNextAtomicAction(int playerID, Map<Integer, Action> actionMap , State postActionState){
        if(!actionMap.containsKey(playerID)) return;//bad call.
        Action mappedAction = actionMap.get(playerID);
        if(mappedAction instanceof DirectedAction){//PrimitiveMove
            DirectedAction directedAction = (DirectedAction) mappedAction;
            applyMove(playerID, directedAction.getDirection(), postActionState);
        } else if (mappedAction instanceof TargetedAction){//Compound/Primitive Attack
            TargetedAction targetedAction = (TargetedAction) mappedAction;
            
            if(targetedAction.getType() == ActionType.PRIMITIVEATTACK) {//I doubt anyone uses primitive attacks.
                applyAttack(playerID, targetedAction.getTargetId(), postActionState);
            } else{
                System.err.println("ERROR: COULD NOT EXECUTE COMPOUND ACTION: "+targetedAction.toString()+'\n');
            }
            //TODO:
            //I need the next primitive in the (possibly) compound action.
            //if it's a move I have to get the direction, else I can make up the attack.
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
    private static double applyHeuristic(Map<Integer, Action> givenActionMap, State.StateView givenPreActionState){
        List<Unit.UnitView> footmen = new ArrayList<Unit.UnitView>();
        List<Unit.UnitView> archers = new ArrayList<Unit.UnitView>();
        for (Unit.UnitView unit : givenPreActionState.getAllUnits()) {//categorize dem units
            if (unit.getTemplateView().getName().equals("Footman")) {
                footmen.add(unit);
            } else if (unit.getTemplateView().getName().equals("Archer")) {
                archers.add(unit);
            }
        }
        
        double goodHealth = 0d;//health of footmen
        double badHealth = 0d;//health of archers
        int distance1 = 0;//distance between each footman, and what's probably its target
        
        //apply attacks to HP
        //apply moves to distance
        
        for(Unit.UnitView footman: footmen){
            Action action = givenActionMap.get(footman.getID());
            if (action instanceof DirectedAction){
                DirectedAction directedAction = (DirectedAction) action;
                int x = directedAction.getDirection().xComponent() + footman.getXPosition();
                int y = directedAction.getDirection().yComponent() + footman.getYPosition();
                int minDistance = Integer.MAX_VALUE;
                for (Unit.UnitView archer : archers) {
                    int currentDistance = manhattanImmediate(archer, x,y);
                    if (currentDistance < minDistance) minDistance = currentDistance;
                }
                distance1 += minDistance;
            } else if (action instanceof TargetedAction){
                TargetedAction targetedAction = (TargetedAction) action;
                badHealth += givenPreActionState.getUnit(targetedAction.getTargetId()).getHP()
                        - footman.getTemplateView().getBasicAttack();
            }
        }
        int distance2 = 0;
        for(Unit.UnitView archer: archers){
            Action action = givenActionMap.get(archer.getID());
            if (action instanceof DirectedAction){
                DirectedAction directedAction = (DirectedAction) action;
                int x = directedAction.getDirection().xComponent() + archer.getXPosition();
                int y = directedAction.getDirection().yComponent() + archer.getYPosition();
                int minDistance = Integer.MAX_VALUE;
                for (Unit.UnitView footman : footmen) {
                    int currentDistance = manhattanImmediate(footman, x,y);
                    if (currentDistance < minDistance) minDistance = currentDistance;
                }
                distance2 += minDistance;
            } else if (action instanceof TargetedAction){
                TargetedAction targetedAction = (TargetedAction) action;
                goodHealth += givenPreActionState.getUnit(targetedAction.getTargetId()).getHP()
                        - archer.getTemplateView().getBasicAttack();
            }
        }
        
        return goodHealth - badHealth - (distance1+distance2);
    }
    
    private static int manhattanImmediate(Unit.UnitView loc1, int x, int y){
        return Math.abs(loc1.getXPosition() - x) +  Math.abs(loc1.getYPosition() - y);
    }
}
