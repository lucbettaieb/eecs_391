package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by aidan on 4/24/15.
 */
public class Utils {

    /**
     * enumerates my footmen that just finished an action
     * i.e. footmen that just completed an action
     * @param historyView history for the current game
     * @param currentTurnNumber the turn number about to be executed (i.e. not subtracted by 1)
     * @param playerNumber
     * @return the list of footman's IDs that are now idle
     */
    public static List<Integer> getIdleFootmen(History.HistoryView historyView, int currentTurnNumber, int playerNumber) {
        List<Integer> returnVar = new ArrayList<>();

        Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playerNumber, currentTurnNumber - 1);
        for (ActionResult result : actionResults.values()) {
            //no, IntelliJ, I don't want to do a "collect" call with lambdas and things I don't understand.
            if(result.getFeedback() == ActionFeedback.COMPLETED)returnVar.add(result.getAction().getUnitId());
        }
        return returnVar;
    }

    /**
     * copy of printTestData, to print the weights
     * * called after every epoch, this is designed to show that weights converge eventually
     * @param weights weight list for the features
     */
    public static void printWeights(List<Double> weights){
        System.out.println("");
        System.out.println("Feature Number      Weight value");
        System.out.println("--------------      ---------------");
        for (int i = 0; i < weights.size(); i++) {
            String gamesPlayed = Integer.toString(i);
            String averageReward = String.format("%.2f", weights.get(i));

            int numSpaces = "-------------     ".length() - gamesPlayed.length();
            StringBuffer spaceBuffer = new StringBuffer(numSpaces);
            for (int j = 0; j < numSpaces; j++) {
                spaceBuffer.append(" ");
            }
            System.out.println(gamesPlayed + spaceBuffer.toString() + averageReward);
        }
        System.out.println("");
    }

    /**
     * abstraction of the temporal difference function.  
     * * Makes things nice and readable
     * * Gamma is taken from "this"
     * @param reward reward for a predetermined state/action
     * @param gamma discount factor               
     * @param newQ Q(s',a')
     * @param oldQ Q(s ,a )
     * @return the temporal difference: reward + gamma * newQ - oldQ_
     */
    public static double temporalDifference(double reward, double gamma, double newQ, double oldQ){
        return (reward + gamma * newQ - oldQ);
    }

    /**
     * You give me the history and current state
     * I tell you if someone was killed or if I was attacked.
     * This is deemed an "event", which is important and means weights should be updated
     * @param historyView history of the current game
     * @param stateView current state of the game
     * @param playernum player number to check event for                  
     * @return whether I was attacked happened, or a player died.  Defaults to true on first turn
     */
    public static boolean hasEventOccured(State.StateView stateView, History.HistoryView historyView, int playernum){
        if(stateView.getTurnNumber()<=0) return true;
        int turnNumber = stateView.getTurnNumber();
        if(historyView.getDeathLogs(turnNumber-1).size()>0) {
            return true;//death occured
        }
        for(DamageLog damageLog : historyView.getDamageLogs(turnNumber-1)){
            if(damageLog.getDefenderController() == playernum) return true;
        }
        return false;
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * This function will load the weights stored at agent_weights/weights.txt. The contents of this file
     * can be created using the saveWeights function. You will use this function if the load weights argument
     * of the agent is set to 1.
     *
     * @return The array of weights
     */
    public static Double[] loadWeights() {
        File path = new File("agent_weights/weights.txt");
        if (!path.exists()) {
            System.err.println("Failed to load weights. File does not exist");
            return null;
        }

        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            String line;
            List<Double> weights = new LinkedList<>();
            while((line = reader.readLine()) != null) {
                weights.add(Double.parseDouble(line));
            }
            reader.close();

            return weights.toArray(new Double[weights.size()]);
        } catch(IOException ex) {
            System.err.println("Failed to load weights from file. Reason: " + ex.getMessage());
        }
        return null;
    }

    /**
     * you give me a double[]
     * I give you the equivalent Double[]
     * @param input a double[]
     * @return the Double[]
     */
    protected static Double[] convertdoubleToDouble(double[] input){
        //convoluded code below courtesy of: http://stackoverflow.com/questions/880581/how-to-convert-int-to-integer-in-java
        return Arrays.stream(input).boxed().toArray(Double[]::new);
    }

    /**
     * you give me a Double[]
     * I give you a double[]
     * @param input a Double[]
     * @return the double[]
     */
    protected static double[] convertDoubleTodouble(Double[] input){
        //convoluded code below courtesy of: http://stackoverflow.com/questions/960431/how-to-convert-listinteger-to-int-in-java
        if(RLAgent.debug) RLAgent.out("ready to convert Double to double");
        double[] returnVar = Arrays.stream(input).mapToDouble(i->i).toArray();
        if(RLAgent.debug) RLAgent.out("Converted Double to double");
        return returnVar;
    }
}
