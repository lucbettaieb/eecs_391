 package edu.cwru.sepia.agent;
/**
 * average reward isn't happy
 *
 */

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionFeedback;
import edu.cwru.sepia.action.ActionResult;
import edu.cwru.sepia.environment.model.history.DamageLog;
import edu.cwru.sepia.environment.model.history.DeathLog;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.*;
import java.util.*;

public class RLAgent extends Agent {
    protected static final boolean debug = true;//debug flag, used for being verbose
    

    /**
     * Set in the constructor. Defines how many learning episodes your agent should run for.
     * When starting an episode. If the count is greater than this value print a message
     * and call sys.exit(0)
     */
    public final int numEpisodes;
    private List<Integer> myFootmen;//IDs of my footmen
    private List<Integer> enemyFootmen;//IDs of footmen owned by ENEMY_PLAYERNUM
    Map<Integer, Integer> unitHealth;
    Map<Integer, Position> unitLocations;

    /**
     * Convenience variable specifying enemy agent number. Use this whenever referring
     * to the enemy agent. We will make sure it is set to the proper number when testing your code.
     */
    public static final int ENEMY_PLAYERNUM = 1;
    public static final int NUM_FEATURES = FeatureVector.NUM_FEATURES;
    public final Random random = new Random(12345);
    @Deprecated
    public Double[] weights; //q function weights
                             //this is read on startup into FeatureVector, and written from FeatureVector before finish

    /**
     * These variables are set for you according to the assignment definition. You can change them,
     * but it is not recommended. If you do change them please let us know and explain your reasoning for
     * changing them.
     */
    public final double gamma = 0.9;            //loss rate                 [0.9]
    public final double alpha = .0001;          //learning rate             [0.0001]
    public final double epsilon = .02;          //disobedience probability  [0.02]
    public boolean exploitationMode = false;    //exploration/exploitation setting.
    public int currentEpisodeNumber = 0;        //what episode are we on?
    private List<Double> averageReward;         //reward across this epoch
    private double currentReward;               //reward across this game
    private FeatureVector featureVector;        //features and stuff
    private int epoch = 0;                      //one cycle of exploration/exploitation
    private int victories=0;

    /**
     * Constructor for RLAgent object
     * @param playernum my playerID
     * @param args number of episodes in 0th location, whether to load weights in 1st location
     */
    public RLAgent(int playernum, String[] args) {
        super(playernum);
        if(debug) setVerbose(true);//"print out the action list each time it is chosen by getAction()"

        if (args.length >= 1) {
            numEpisodes = Integer.parseInt(args[0]);
            out("Running " + numEpisodes + " episodes.");
        } else {
            out("Warning! Number of episodes not specified. Defaulting to 50 episodes.");
            numEpisodes = 50;
        }
        if(debug) out("deciding if I should load weights");
        boolean loadWeights = false;
        if (args.length >= 2) loadWeights = Boolean.parseBoolean(args[1]);
        else System.out.println("Warning! Load weights argument not specified. Defaulting to not loading.");
        if(debug) out("weights-loading decision made.");
        if (loadWeights) weights = loadWeights();
        else {
            // initialize weights to random values between -1 and 1
            if(debug) out("randomizing weights");
            weights = new Double[NUM_FEATURES];
            for (int i = 0; i < weights.length; i++) {
                weights[i] = random.nextDouble() * 2 - 1;
            }//end of for loop
        }//end of else statement
        if(debug) out("converting weight types");
        this.featureVector = new FeatureVector();//ugh. the next line caused me too much grief because it didn't report an NPE
        featureVector.featureWeights = convertDoubleTodouble(weights);//it's all loaded up, put it where I use it
        if(debug) out("RLAgent constructor finished");
        this.averageReward = new LinkedList<>();
    }//end of constructor

    /**
     * We've implemented some setup code for your convenience. Change what you need to.
     */
    @Override
    public Map<Integer, Action> initialStep(State.StateView stateView, History.HistoryView historyView) {
        if(debug) out("Initial step hit");
        if(currentEpisodeNumber > numEpisodes){
            out(String.format("final stats: played %d games, won %d games, %f win rate", 
                    this.currentEpisodeNumber, this.victories, (float)this.victories/(float)this.currentEpisodeNumber));
            
            if(debug)out("finished all epochs, exiting");
            System.exit(0);
        }
        this.unitHealth = new HashMap<>();
        this.unitLocations = new HashMap<>();
        // You will need to add code to check if you are in a testing or learning episode
        enumerateUnits(stateView);
        this.exploitationMode = this.currentEpisodeNumber % 10 > 2;//80% exploitation, 20% exploration
        if(!this.exploitationMode) {
            if(this.averageReward.size()<= epoch) this.averageReward.add(epoch, 0d);
        }
        this.featureVector = new FeatureVector();
        for(Unit.UnitView view : stateView.getAllUnits()){
            this.unitHealth.put(view.getID(), view.getHP());
            this.unitLocations.put(view.getID(), new Position(view));
        }
        return middleStep(stateView, historyView);
    }

    /**
     * You will need to calculate the reward at each step and update your totals. You will also need to
     * check if an event has occurred. If it has then you will need to update your weights and select a new action.
     *
     * If you are using the footmen vectors you will also need to remove killed units. To do so use the historyView
     * to get a DeathLog. Each DeathLog tells you which player's unit died and the unit ID of the dead unit. To get
     * the deaths from the last turn do something similar to the following snippet. Please be aware that on the first
     * turn you should not call this as you will get nothing back.
     *
     * for(DeathLog deathLog : historyView.getDeathLogs(stateView.getTurnNumber() -1)) {
     *     System.out.println("Player: " + deathLog.getController() + " unit: " + deathLog.getDeadUnitID());
     * }
     *
     * You should also check for completed actions using the history view. Obviously you never want a footman just
     * sitting around doing nothing (the enemy certainly isn't going to stop attacking). So at the minimum you will
     * have an even whenever one your footmen's targets is killed or an action fails. Actions may fail if the target
     * is surrounded or the unit cannot find a path to the unit. To get the action results from the previous turn
     * you can do something similar to the following. Please be aware that on the first turn you should not call this
     *
     * Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, stateView.getTurnNumber() - 1);
     * for(ActionResult result : actionResults.values()) {
     *     System.out.println(result.toString());
     * }
     *
     * @return New actions to execute or nothing if an event has not occurred.
     */
    @Override
    public Map<Integer, Action> middleStep(State.StateView stateView, History.HistoryView historyView) {
        if(debug) out("beginning execution of middle step");
        Map<Integer, Action> actionMap = new HashMap<>();
        boolean eventOccured = hasEventOccured(stateView, historyView);
        updateUnits(historyView, stateView, stateView.getTurnNumber());
        List<Integer> idleFootmen = getIdleFootmen(historyView, stateView.getTurnNumber());
        Map<Integer, Integer> idAttackTuple;
        
        if(idleFootmen.size()>0){
            //generate a myfootmanID/enemyFootmanID K/V pair for all idle footmen
            idAttackTuple = generateAttacks(idleFootmen);
            for(Integer myFootman : idAttackTuple.keySet()){
                //turn the above K/V pair into a myfootmanID/Action K/V pair
                actionMap.put(myFootman, Action.createCompoundAttack(myFootman, idAttackTuple.get(myFootman)));
            }
            //reallocate attacks from these guys
        }
        if(!eventOccured) return actionMap;//also happens on first turn
        idAttackTuple = generateAttacks(myFootmen);
        
        for(Integer footmanID:idAttackTuple.keySet()){//if an event occurred, reallocate all targets.  Map.put overwrites old value
            int targetID = idAttackTuple.get(footmanID);
            actionMap.put(footmanID, Action.createCompoundAttack(footmanID,targetID));
            currentReward = Math.max(currentReward, calculateReward(stateView, historyView, footmanID));
            if(!this.exploitationMode) {
                double[] features = calculateFeatureVector(stateView, historyView, footmanID, targetID);
                updateWeights(featureVector.featureWeights, features, currentReward, stateView, historyView, footmanID);
            }
        }
        return actionMap;
    }

    /**
     * Here you will calculate the cumulative average rewards for your testing episodes. If you have just
     * finished a set of test episodes you will call out testEpisode.
     *
     * It is also a good idea to save your weights with the saveWeights function.
     */
    @Override
    public void terminalStep(State.StateView stateView, History.HistoryView historyView) {
        updateUnits(historyView, stateView, stateView.getTurnNumber());//update for killed players
        if(debug) out("terminal step reached");
        if(enemyFootmen.size()==0) {
            out("victory");
            victories++;
        }
        else if(myFootmen.size()==0) out("failure");
        else err("cannot determine victory or failure");
        out("finished total episode: "+currentEpisodeNumber+" categorized under epoch: "+epoch);
        //take my weights that I kept in the FeatureVector, and write it back here for all Devin's code to use on finishing
        weights = convertdoubleToDouble(featureVector.featureWeights);

        averageReward.set(epoch, averageReward.get(epoch) + 
                (currentReward-averageReward.get(epoch))/ Math.max(currentEpisodeNumber % 10,1));

        printTestData(this.averageReward);
        saveWeights(convertdoubleToDouble(featureVector.featureWeights));
        currentEpisodeNumber++;
        if(currentEpisodeNumber % 10 == 0) epoch++;
    }

    /**
     * Calculate the updated weights for this agent. 
     * @param oldWeights Weights prior to update, unused: located within the FeatureVector
     * @param oldFeatures Features from (s,a)
     * @param totalReward Cumulative discounted reward for this footman.
     * @param stateView Current state of the game.
     * @param historyView History of the game up until this point
     * @param footmanId The footman we are updating the weights for
     * @return The updated weight vector.
     */
    public double[] updateWeights(double[] oldWeights, double[] oldFeatures, double totalReward, 
                                  State.StateView stateView, History.HistoryView historyView, int footmanId) {
        
        double oldQ = featureVector.qFunction(oldFeatures);
        int optimalEnemy = selectAction(footmanId);
        double[] newFeatures = FeatureVector.getFeatures(footmanId, optimalEnemy, myFootmen, enemyFootmen,unitHealth, unitLocations);
        double newQ = featureVector.qFunction(newFeatures);
        double loss = calculateLoss(totalReward,newQ,oldQ);
        return featureVector.updateWeights(oldFeatures, loss, alpha);
    }

    /**
     * Given a footman and the current state and history of the game select the enemy that this unit should
     * attack. This is where you would do the epsilon-greedy action selection.
     *
     * @param attackerId The footman that will be attacking
     * @return The enemy footman ID this unit should attack
     */
    public int selectAction(int attackerId) {
        /**
         * epsilon-greedy says we follow the "best" action 1-epsilon percent of the time
         * we take a random action epsilon percent of the time
         * as we move forward, epsilon decays to 0
         */
        double bestActionProbability = 1-epsilon;//this converges to 1, so rand() > bestActionProbability causes exploration
        double rand = random.nextDouble();
        boolean exploreAction;
        exploreAction = (rand >= bestActionProbability);
        if(exploreAction && !exploitationMode){//we're not following the policy, and we're in exploitation
            //TODO: above 'if' statement may not depend on exploitation mode
            if(debug) out("disobeying the policy");
            return randomEnemy();
        } else {//we're either following the policy, or not exploring
            double qOfBestEnemy = Double.NEGATIVE_INFINITY;
            int bestEnemy = enemyFootmen.get(0);//arbitrary first enemy
            for(Integer enemyID : enemyFootmen){//for each enemy footman
                double[] features = FeatureVector.getFeatures(attackerId, enemyID,myFootmen, enemyFootmen, unitHealth, unitLocations);
                double q = featureVector.qFunction(features);
                if(q > qOfBestEnemy){
                    qOfBestEnemy = q;
                    bestEnemy = enemyID;
                }
            }
            return bestEnemy;
        }
    }

    /**
     * Given the current state and the footman in question calculate the reward received on the last turn.
     * This is where you will check for things like Did this footman take or give damage? Did this footman die
     * or kill its enemy. Did this footman start an action on the last turn? See the assignment description
     * for the full list of rewards.
     *
     * Remember that you will need to discount this reward based on the timestep it is received on. See
     * the assignment description for more details.
     *
     * As part of the reward you will need to calculate if any of the units have taken damage. You can use
     * the history view to get a list of damages dealt in the previous turn. Use something like the following.
     *
     * for(DamageLog damageLogs : historyView.getDamageLogs(lastTurnNumber)) {
     *     System.out.println("Defending player: " + damageLog.getDefenderController() + " defending unit: " + \
     *     damageLog.getDefenderID() + " attacking player: " + damageLog.getAttackerController() + \
     *     "attacking unit: " + damageLog.getAttackerID());
     * }
     *
     * You will do something similar for the deaths. See the middle step documentation for a snippet
     * showing how to use the deathLogs.
     *
     * To see if a command was issued you can check the commands issued log.
     *
     * Map<Integer, Action> commandsIssued = historyView.getCommandsIssued(playernum, lastTurnNumber);
     * for (Map.Entry<Integer, Action> commandEntry : commandsIssued.entrySet()) {
     *     System.out.println("Unit " + commandEntry.getKey() + " was command to " + commandEntry.getValue().toString);
     * }
     *
     * @param stateView The current state of the game.
     * @param historyView History of the episode up until this turn.
     * @param footmanId The footman ID you are looking for the reward from.
     * @return The current reward
     */
    public double calculateReward(State.StateView stateView, History.HistoryView historyView, int footmanId) {
        double reward = -.2d;
        if(!myFootmen.contains(footmanId)){
            //this guy died.
            reward -= 10;
        }
        int turnNumber  = stateView.getTurnNumber();
        if(turnNumber<=0) return reward;
        for(DamageLog log :  historyView.getDamageLogs(turnNumber - 1)){
            if(log.getDefenderController() == playernum){
                reward -= log.getDamage();
            }
            if(log.getAttackerController() == playernum){
                reward += log.getDamage();
            }
        }
        for(DeathLog log : historyView.getDeathLogs(turnNumber-1)){
            if(log.getController() == playernum && myFootmen.contains(log.getDeadUnitID())){
                reward -= 100;
            }
            if(log.getController() == ENEMY_PLAYERNUM && enemyFootmen.contains(log.getDeadUnitID())){
                reward += 100;
            }
        }
        return reward;
    }

    /**
     * Calculate the Q-Value for a given state action pair. The state in this scenario is the current
     * state view and the history of this episode. The action is the attacker and the enemy pair for the
     * SEPIA attack action.
     *
     * This returns the Q-value according to your feature approximation. This is where you will calculate
     * your features and multiply them by your current weights to get the approximate Q-value.
     *
     * @param stateView Current SEPIA state
     * @param historyView Episode history up to this point in the game
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman that your footman would be attacking
     * @return The approximate Q-value
     */
    @Deprecated //absorbed by the FeatureVector class
    public double calcQValue(State.StateView stateView, History.HistoryView historyView, int attackerId, int defenderId) {
        double[] featureVectorValue = calculateFeatureVector(stateView, historyView, attackerId, defenderId);
        return featureVector.qFunction(featureVectorValue);
    }

    /**
     * Given a state and action calculate your features here. Please include a comment explaining what features
     * you chose and why you chose them.
     *
     * All of your feature functions should evaluate to a double. Collect all of these into an array. You will
     * take a dot product of this array with the weights array to get a Q-value for a given state action.
     *
     * It is a good idea to make the first value in your array a constant. This just helps remove any offset
     * from 0 in the Q-function. The other features are up to you. Many are suggested in the assignment
     * description.
     *
     * @param stateView Current state of the SEPIA game
     * @param historyView History of the game up until this turn
     * @param attackerId Your footman. The one doing the attacking.
     * @param defenderId An enemy footman. The one you are considering attacking.
     * @return The array of feature function outputs.
     */
    public double[] calculateFeatureVector(State.StateView stateView, History.HistoryView historyView, int attackerId, int defenderId) {
        return FeatureVector.getFeatures(attackerId, defenderId, myFootmen, enemyFootmen,unitHealth, unitLocations);
    }

    /**
     * DO NOT CHANGE THIS!
     *
     * Prints the learning rate data described in the assignment. Do not modify this method.
     *
     * @param averageRewards List of cumulative average rewards from test episodes.
     */
    public void printTestData (List<Double> averageRewards) {
        System.out.println("");
        System.out.println("Games Played      Average Cumulative Reward");
        System.out.println("-------------     -------------------------");
        for (int i = 0; i < averageRewards.size(); i++) {
            String gamesPlayed = Integer.toString(10*i);
            String averageReward = String.format("%.2f", averageRewards.get(i));

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
     * DO NOT CHANGE THIS!
     *
     * This function will take your set of weights and save them to a file. Overwriting whatever file is
     * currently there. You will use this when training your agents. You will include th output of this function
     * from your trained agent with your submission.
     *
     * Look in the agent_weights folder for the output.
     *
     * @param weights Array of weights
     */
    public void saveWeights(Double[] weights) {
        File path = new File("agent_weights/weights.txt");
        // create the directories if they do not already exist
        path.getAbsoluteFile().getParentFile().mkdirs();

        try {
            // open a new file writer. Set append to false
            BufferedWriter writer = new BufferedWriter(new FileWriter(path, false));

            for (double weight : weights) {
                writer.write(String.format("%f\n", weight));
            }
            writer.flush();
            writer.close();
        } catch(IOException ex) {
            System.err.println("Failed to write weights to file. Reason: " + ex.getMessage());
        }
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
    public Double[] loadWeights() {
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

    @Override
    public void savePlayerData(OutputStream outputStream) {

    }

    @Override
    public void loadPlayerData(InputStream inputStream) {

    }

    /**
     * removes killed units from the myfootmen and enemyfootmen fields
     * @param history history for the current game
     * @param currentTurnNumber the turn number about to be executed (i.e. not subtracted by 1)
     */
    private void updateUnits(History.HistoryView history, State.StateView stateView, int currentTurnNumber){
        for(Unit.UnitView view : stateView.getAllUnits()){
            this.unitHealth.put(view.getID(), view.getHP());
            this.unitLocations.put(view.getID(), new Position(view));
        }
        if(currentTurnNumber<=0) return;
        for(DeathLog deathLog : history.getDeathLogs(currentTurnNumber -1)) {
            int deadUnitID = deathLog.getDeadUnitID();
            int deadUnitsPlayer = deathLog.getController();
            if(deadUnitsPlayer == ENEMY_PLAYERNUM){//was the dead unit owned by the enemy?
                this.enemyFootmen.remove(enemyFootmen.indexOf(deadUnitID));
                out("Enemy player lost unit: "+deadUnitID+", they now have "+enemyFootmen.size()+" units left");
            } else if(deadUnitsPlayer == playernum){//the dead unit was owned by me
                this.myFootmen.remove(myFootmen.indexOf(deadUnitID));
                out("I lost unit: "+deadUnitID+", I now have "+myFootmen.size()+" units left");
            } else {
                err("a unit owned by an unknown player died");
                err("that player was " + deadUnitsPlayer);
                err("and the unit was " + deadUnitID);
            }
        }
    }

    /**
     * enumerates my footmen that just finished an action
     * i.e. footmen that just completed an action
     * @param historyView history for the current game
     * @param currentTurnNumber the turn number about to be executed (i.e. not subtracted by 1)
     * @return the list of footman's IDs that are now idle
     */
    private List<Integer> getIdleFootmen(History.HistoryView historyView, int currentTurnNumber) {
        List<Integer> returnVar = new ArrayList<>();

        Map<Integer, ActionResult> actionResults = historyView.getCommandFeedback(playernum, currentTurnNumber - 1);
        for (ActionResult result : actionResults.values()) {
            //no, IntelliJ, I don't want to do a "collect" call with lambdas and things I don't understand.
            if(result.getFeedback() == ActionFeedback.COMPLETED)returnVar.add(result.getAction().getUnitId());
        }
        return returnVar;
    }

    /**
     *  fills the "myFootmen" and "enemyFootmen" fields
     * @param stateView the stateview of the current board
     */
    private void enumerateUnits(State.StateView stateView){
        // Find all of your units
        this.myFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(playernum)) {//get all of my footmen, and my them in myFootmen
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                this.myFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }

        // Find all of the enemy units
        this.enemyFootmen = new LinkedList<>();
        for (Integer unitId : stateView.getUnitIds(ENEMY_PLAYERNUM)) {//get all their footmen and put them in enemyFootmen
            Unit.UnitView unit = stateView.getUnit(unitId);

            String unitName = unit.getTemplateView().getName().toLowerCase();
            if (unitName.equals("footman")) {
                this.enemyFootmen.add(unitId);
            } else {
                System.err.println("Unknown unit type: " + unitName);
            }
        }
    }
    
    /**
     * You give me the history and current state
     * I tell you if someone was killed or if I was attacked.
     * This is deemed an "event", which is important and means weights should be updated
     * @param historyView history of the current game
     * @param stateView current state of the game
     * @return whether I was attacked happened, or a player died.  Defaults to true on first turn
     */
    private boolean hasEventOccured(State.StateView stateView, History.HistoryView historyView){
        if(stateView.getTurnNumber()<=0) return true;
        int turnNumber = stateView.getTurnNumber();
        if(historyView.getDeathLogs(turnNumber-1).size()>0) {
            if(debug) out("a death was seen. an event occured between turns "+(turnNumber-1)+" and "+turnNumber);
            return true;//death occured
        }
        for(DamageLog damageLog : historyView.getDamageLogs(turnNumber-1)){
            if(damageLog.getDefenderController() == playernum) return true;
        }
        if(debug)out("No event was seen between turns "+(turnNumber-1)+" and "+turnNumber);
        return false;
    }
    
    //effectively a macro to make typing "system.out.println" more bearable.
    public void out(String s){
        System.out.println(s);
    }
    public void err(String s){
        System.err.println(s);
    }

    /**
     * you give me the footmen you want to assign attacks for
     * I give you a map from my footmen to enemy footmen, indicating who the key should attack 
     * @param idleFootmen footmen to assign attacks for
     * @return a K/V pair, where the key is my footman, and the value is the enemy it should attack
     */
    private Map<Integer, Integer> generateAttacks(List<Integer> idleFootmen){
        Map<Integer, Integer> returnVar = new HashMap<>();
        for(Integer footmanID : idleFootmen){
            returnVar.put(footmanID, selectAction(footmanID));
        }
        return returnVar;
    }

    /**
     * you give me a double[]
     * I give you the equivalent Double[] 
     * @param input a double[]
     * @return the Double[]
     */
    private Double[] convertdoubleToDouble(double[] input){
        //convoluded code below courtesy of: http://stackoverflow.com/questions/880581/how-to-convert-int-to-integer-in-java
        return Arrays.stream(input).boxed().toArray(Double[]::new);
    }

    /**
     * you give me a Double[]
     * I give you a double[]
     * @param input a Double[]
     * @return the double[]
     */
    private double[] convertDoubleTodouble(Double[] input){
        //convoluded code below courtesy of: http://stackoverflow.com/questions/960431/how-to-convert-listinteger-to-int-in-java
        if(debug) out("ready to convert Double to double");
        double[] returnVar = Arrays.stream(input).mapToDouble(i->i).toArray();
        if(debug) out("Converted Double to double");
        return returnVar;
    }
    
    private double calculateLoss(double reward, double newQ, double oldQ){
        return (reward + gamma * newQ - oldQ);
    }

    /**
     * gets a random element from the enemyFootmen list
     * useful for exploration in Q-learning
     * @return ID of a random enemy footman who is alive
     */
    private int randomEnemy(){
        //code adapted from: http://stackoverflow.com/questions/5034370/retrieving-a-random-item-from-arraylist
        int index = random.nextInt(enemyFootmen.size());
        return enemyFootmen.get(index);
    }
}
