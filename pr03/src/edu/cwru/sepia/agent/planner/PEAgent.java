package edu.cwru.sepia.agent.planner;

import com.sun.tools.javac.util.ArrayUtils;
import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;

import javax.annotation.Resource;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may add your own methods and members.
 */
public class PEAgent extends Agent {

    // The plan being executed
    private Stack<StripsAction> plan = null;

    // maps the real unit Ids to the plan's unit ids
    // when you're planning you won't know the true unit IDs that sepia assigns. So you'll use placeholders (1, 2, 3).
    // this maps those placeholders to the actual unit IDs.
    private Map<Integer, Integer> peasantIdMap;
    private int townhallId;
    private int peasantTemplateId;

    public PEAgent(int playernum, Stack<StripsAction> plan) {
        super(playernum);
        peasantIdMap = new HashMap<Integer, Integer>();
        this.plan = plan;

    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        // gets the townhall ID and the peasant ID
        for(int unitId : stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(unitId);
            String unitType = unit.getTemplateView().getName().toLowerCase();
            if(unitType.equals("townhall")) {
                townhallId = unitId;
            } else if(unitType.equals("peasant")) {
                peasantIdMap.put(unitId, unitId);//TODO: does this actually do what they specify?
            }
        }

        // Gets the peasant template ID. This is used when building a new peasant with the townhall
        for(Template.TemplateView templateView : stateView.getTemplates(playernum)) {
            if(templateView.getName().toLowerCase().equals("peasant")) {
                peasantTemplateId = templateView.getID();
                break;
            }
        }

        return middleStep(stateView, historyView);
    }

    /**
     * This is where you will read the provided plan and execute it. If your plan is correct then when the plan is empty
     * the scenario should end with a victory. If the scenario keeps running after you run out of actions to execute
     * then either your plan is incorrect or your execution of the plan has a bug.
     *
     * You can create a SEPIA deposit action with the following method
     * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
     *
     * You can create a SEPIA harvest action with the following method
     * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
     *
     * You can create a SEPIA build action with the following method
     * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
     *
     * You can create a SEPIA move action with the following method
     * Action.createCompoundMove(int peasantId, int x, int y)
     *
     * these actions are stored in a mapping between the peasant unit ID executing the action and the action you created.
     *
     * For the compound actions you will need to check their progress and wait until they are complete before issuing
     * another action for that unit. If you issue an action before the compound action is complete then the peasant
     * will stop what it was doing and begin executing the new action.
     *
     * To check an action's progress you can call getCurrentDurativeAction on each UnitView. If the Action is null nothing
     * is being executed. If the action is not null then you should also call getCurrentDurativeProgress. If the value is less than
     * 1 then the action is still in progress.
     *
     * Also remember to check your plan's preconditions before executing!
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        List<ResourceNode.ResourceView> resources = stateView.getAllResourceNodes();
        List<Unit.UnitView> units = stateView.getAllUnits();
        //compoundGather, and compoundDeposit
        while(!plan.empty()){
            Action nextAction = createSepiaAction(plan.pop(), resources, units);
            
        }
        // TODO: Implement me!
        return null;
    }

    /**
     * Returns a SEPIA version of the specified Strips Action.
     * @param action StripsAction
     * @return SEPIA representation of same action
     */
    private Action createSepiaAction(StripsAction action, List<ResourceNode.ResourceView> resourceList, List<Unit.UnitView> units) {
        Token token = new Token(action.getSentence());
        
        switch (token.verb){//dear luc: if this is complaining, switch "project language level" to 8 (or 7)
            case "get":
                Action.createCompoundGather(peasantIdMap.get(1), 
                        getNearestNonemptyResource(resourceList,token.nounEnums, myPosition));
                break;
            case "move":
                break;
            case "put":
                break;
            default:
                break;
        }
        return null;
    }

    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {

    }

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }
    //Takes a single Plan action, and parses it into a more usable form
    private class Token{
        /*
            Language:
                Verb->ID->(->Args->)
                
                Args := Noun
                     := Noun , Args
                     
                Verb := Move
                     := Get
                     := Put
                
                ID   := 1
                     := 2
                     := 3
                     
            This is code is not resilient at all.
            Do not pass it malformed requests.
         */
        private String value;
        private String verb;
        private int id;
        private ArrayList<ResourceNode.Type> nounEnums;
        
        public Token(String value){
            this.value = value;
            this.nounEnums = new ArrayList<>();
        }

        public void parse(){
            this.value = this.value.replace(" ","");
            this.value = this.value.toLowerCase();
            parseVerb();
            parseID();
            parseNouns();
        }
        
        private void parseVerb(){
            if(value.contains("move")) verb = "move";//Moves are ignored.  Compound actions are used instead.
            if(value.contains("get")) verb = "get";
            if(value.contains("put")) verb = "put";
        }
        
        private void parseID(){
            if(value.contains("1")) id = 1;
            if(value.contains("2")) id = 2;
            if(value.contains("3")) id = 3;
        }
        
        private void parseNouns(){
            String inQuestion = value.substring(value.indexOf('('), value.indexOf(')'));
            for(String noun: inQuestion.split(",")){
                ResourceNode.Type type = getTypeFromString(noun);
                if(type != null) nounEnums.add(type);
            }
        }
        
        @Override
        public String toString(){
            StringBuilder returnVar = new StringBuilder();
            returnVar.append(verb);
            returnVar.append(id).append('(');
            for(ResourceNode.Type type: nounEnums) {
                returnVar.append(type.toString());
                returnVar.append(',');
            }
            if(returnVar.lastIndexOf(",") >=0) returnVar.deleteCharAt(returnVar.lastIndexOf(","));
            returnVar.append(')');
            return returnVar.toString();
        }
    }

    /**
     * Find the nearest resource to my position
     *      it must be of the specified type
     *      it must not be empty      
     * @param resources arrayList of ResourceViews on the map
     * @param resources name of requested resource, as a String
     * @param myPosition my current position
     * @return the best destination, or -1 if impossible
     */
    private int getNearestNonemptyResource(ArrayList<ResourceNode.ResourceView> resources, ResourceNode.Type requiredType, Position myPosition){
        int shortestDistance = Integer.MAX_VALUE;
        int shortestDistanceID = -1;
        for(ResourceNode.ResourceView resource: resources){
            if(resource.getType() == requiredType && resource.getAmountRemaining()>0){
                //if it's what we want, and there's some left.
                int distance = myPosition.chebyshevDistance(new Position(resource.getXPosition(), resource.getYPosition()));
                if (distance < shortestDistance){
                    shortestDistanceID = resource.getID();
                }
            }
        }
        return shortestDistanceID;
    }

    /**
     *
     * @param resourceName requested resource, as a case insensitive string
     * @return the enum associated with the queried string, null if not found
     */
    private ResourceNode.Type getTypeFromString(String resourceName){
        ResourceNode.Type requiredType = null;
        if(resourceName.toLowerCase().contains("gold")) requiredType = ResourceNode.Type.GOLD_MINE;
        if(resourceName.toLowerCase().contains("wood")) requiredType = ResourceNode.Type.TREE;
        if(resourceName.toLowerCase().contains("tree")) requiredType = ResourceNode.Type.TREE;
        if(requiredType == null){
            System.err.println("Error! Invalid resourceName given in getNearestNonemptyResource!");
            System.err.println("Expected: gold, wood");
            System.err.println("Received: "+resourceName);
        }
        return requiredType;
    }
}
