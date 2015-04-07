package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.*;
import java.util.*;

public class PlannerAgent extends Agent {
    public final static boolean debug = true;

    final int requiredWood;
    final int requiredGold;
    final boolean buildPeasants;

    // Your PEAgent implementation. This prevents you from having to parse the text file representation of your plan.
    PEAgent peAgent;

    public PlannerAgent(int playernum, String[] params) {
        super(playernum);

        if(params.length < 3) {
            System.err.println("You must specify the required wood and gold amounts and whether peasants should be built");
        }

        requiredWood = Integer.parseInt(params[0]);
        requiredGold = Integer.parseInt(params[1]);
        buildPeasants = Boolean.parseBoolean(params[2]);


        System.out.println("required wood: " + requiredWood + " required gold: " + requiredGold + " build Peasants: " + buildPeasants);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {

        Stack<StripsAction> plan = AstarSearch(new GameState(stateView, playernum, requiredGold, requiredWood, buildPeasants));

        if(plan == null) {
            System.err.println("No plan was found");
            System.exit(1);
            return null;
        }

        // write the plan to a text file
        savePlan(plan);


        // Instantiates the PEAgent with the specified plan.
        peAgent = new PEAgent(playernum, plan);

        return peAgent.initialStep(stateView, historyView);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        if(peAgent == null) {
            System.err.println("Planning failed. No PEAgent initialized.");
            return null;
        }

        return peAgent.middleStep(stateView, historyView);
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

    /**
     * Perform an A* search of the game graph. This should return your plan as a stack of actions. This is essentially
     * the same as your first assignment. The implementations should be very similar. The difference being that your
     * nodes are now GameState objects not MapLocation objects.
     *
     * @param startState The state which is being planned from
     * @return The plan or null if no plan is found.
     */
    private Stack<StripsAction> AstarSearch(GameState startState) {
        
        PriorityQueue<GameState> openSet = new PriorityQueue<>(); //initialize open list
        openSet.add(startState); //add initial state to the open list
        while(openSet.size() > 0){
            GameState Q = openSet.poll();
            for(GameState successor : Q.generateChildren()){
                //for all possible moves from here:
                if(successor.isGoal()) {
                    if(debug) System.out.println("found path! recursing to generate stack.");
                    return generatePath(successor);
                }
                if(shouldAddToOpenSet(successor, openSet)) openSet.add(successor);
                if(debug) {
                    System.out.println("finished considering child "+successor.hashCode()+", with cost "+successor.getCost());
                    System.out.println("Number of items in the openlist: "+openSet.size());
                }
            }//end of for each child
            if(debug) {
                System.out.println("finished considering all children");
                System.out.println("the current path is: \n"+stringifyPath(generatePath(Q)));
            }
        }//end of open set.  We're done now.
        System.err.println("No path to goal. Cannot plan. Exiting...");
        System.exit(1);
        return null;
    }
    
    private boolean shouldAddToOpenSet(GameState successor, PriorityQueue<GameState> openSet){
        boolean shouldAdd = true;
        for(GameState node: openSet){
            //skip: already going to visit it, with a better path
            if(node.getF() < successor.getF()) shouldAdd = false;
        }
        return shouldAdd;
    }
    
    public String stringifyPath(Stack<StripsAction> path){
        String returnVar="";
        StripsAction action;
        while(!path.empty()){
            action = path.pop();
            returnVar += action.toString() + '\n';
        }
        return returnVar.trim();
    }
    
    public Stack<StripsAction> generatePath(GameState destination){
        Stack<StripsAction> path = new Stack<>();
        boolean isGoal = true;
        while(destination.getParentState() != null){
            if(!isGoal) path.add(destination.getParentAction());
            isGoal = false;
            destination = destination.getParentState();
        }
        return path;
    }

    //We need some way of generating possible actions for each state of the game...
    //But wouldn't this just be meaningless?  All actions are "possible" but some might have a heuristic of zero
    //so that's how I'll handle it!  This method will return a list of actual
    private List<StripsAction> getPossibleActions(GameState currentState){
        ArrayList<StripsAction> actionList = new ArrayList<>();
        return actionList;
    }


    /**
     * This has been provided for you. Each strips action is converted to a string with the toString method. This means
     * each class implementing the StripsAction interface should override toString. Your strips actions should have a
     * form matching your included Strips definition writeup. That is <action name>(<param1>, ...). So for instance the
     * move action might have the form of Move(peasantID, X, Y) and when grounded and written to the file
     * Move(1, 10, 15).
     *
     * @param plan Stack of Strips Actions that are written to the text file.
     */
    private void savePlan(Stack<StripsAction> plan) {
        if (plan == null) {
            System.err.println("Cannot save null plan");
            return;
        }

        File outputDir = new File("saves");
        outputDir.mkdirs();

        File outputFile = new File(outputDir, "plan.txt");

        PrintWriter outputWriter = null;
        try {
            outputFile.createNewFile();

            outputWriter = new PrintWriter(outputFile.getAbsolutePath());

            Stack<StripsAction> tempPlan = (Stack<StripsAction>) plan.clone();
            while(!tempPlan.isEmpty()) {
                outputWriter.println(tempPlan.pop().toString());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (outputWriter != null)
                outputWriter.close();
        }
    }
}
