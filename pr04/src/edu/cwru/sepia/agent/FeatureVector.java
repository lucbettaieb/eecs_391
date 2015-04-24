package edu.cwru.sepia.agent;
import java.util.List;
import java.util.Map;

/** Soumya-blessed Feature examples
 – #of other friendly units that are currently attacking E
 – Health of friendly unit F
 – Health of enemy unit E
 – Is E the enemy unit that F is currently attacking?
 */

/**
 * A class for the "f(s,a)" feature vector
 *
 */
public class FeatureVector {

    public static final int NUM_FEATURES = 7; //The number of features to store
    protected double[] featureWeights; //The weights of each feature

    /**
     * randomizes initial feature values
     */
    public FeatureVector() {
        featureWeights = new double[NUM_FEATURES];
        // to a random value between -1 and 1
        for (int i = 0; i < featureWeights.length; i++) {
            featureWeights[i] = Math.random() * 2 - 1;
        }
    }

    /**
     * Determines the Q weight value by using the given feature vector.
     *
     * @param featureVector - the vector of feature values to use in calculation
     * @return the Q weight value of the given feature vector
     */
    public double qFunction(double[] featureVector) {
        double qWeight = 0;
        for (int i = 0; i < featureWeights.length; i++) {
            qWeight += featureWeights[i] * featureVector[i];
        }
        return qWeight;
    }

    /**
     * Gets the feature vector of a given state using the footman id, the enemy id, a list of footmen ids, a list of enemy ids,
     * the health of all units, a map of the unit locations, and a map of attack actions in reference to unit ids.
     *
     * @param myFootman - the footman id in reference to
     * @param enemyFootman - the enemy targeted by the given footman
     * @param myFootmen - a list of the footmen currently in the game state
     * @param enemyFootmen - a list of the enemy footmen currently in the game state
     * @param unitHealth - a list of the health of all units
     * @return the feature vector in reference to the given values
     */
    public static double[] getFeatures(Integer myFootman, Integer enemyFootman, List<Integer> myFootmen, List<Integer> enemyFootmen,
                                       Map<Integer, Integer> unitHealth, Map<Integer, Position> unitLocations) {
        
        double[] featureVector = new double[NUM_FEATURES];
        featureVector[0] = 1;//first feature value is always 1, because Devin said so
        featureVector[1] = unitHealth.get(enemyFootman);//enemy health
        featureVector[2] = unitHealth.get(myFootman);//footman health
        //determine the ratio of hit-points from enemy to those of footman
        featureVector[3] = unitHealth.get(myFootman) / Math.max(unitHealth.get(enemyFootman), .5d);//don't divide by 0, yo!
        
        //determine whether the enemy is the closest possible enemy
        if (isNearestEnemy(myFootman, enemyFootman, enemyFootmen, unitLocations)) featureVector[4] += .3;
        else featureVector[4] -= .4;
        
        Position myPosition = unitLocations.get(myFootman);
        Position enemyPosition = unitLocations.get(enemyFootman);
        //determine if the enemy can be attacked based on range from current footman
        if(myPosition.isAdjacent(enemyPosition)) featureVector[5] += .03;
        
        int adjEnemyCount = getAdjacentEnemyCount(myFootman, enemyFootmen, unitLocations);
        //determine how many enemies can currently attack the given footman
        if (adjEnemyCount <= 2) featureVector[6] += ((0.02 * adjEnemyCount) / Math.random());
        else featureVector[6] -= ((0.1 * adjEnemyCount) / Math.random());
        
        return featureVector;
    }

    /**
     * Updates the feature weights vector given a feature vector, the temporal difference, and alpha.
     *
     * @param updateVector - the feature vector to update the feature weights set with
     * @param temporalDifference - the calculated loss function
     * @param alpha - a variable alpha value
     */
    public double[] updateWeights(double[] updateVector, double temporalDifference, double alpha) {
        for (int i = 0; i < featureWeights.length; i++) {
            featureWeights[i] += (alpha * temporalDifference * updateVector[i]);
        }
        return featureWeights;
    }

    /**
     * Determines if the given enemy is the closest enemy
     *
     * @param myFootmanID - the footman to use the reference point of
     * @param enemyFootmanID - the enemy to find the distance from the footman
     * @param enemyFootmen - the list of enemy footmen
     * @param unitLocations - the list of unit locations
     * @return if the given enemy is the closest enemy in terms of the Chebyshev distance
     */
    private static boolean isNearestEnemy(Integer myFootmanID, Integer enemyFootmanID, 
                                          List<Integer> enemyFootmen, Map<Integer, Position> unitLocations) {
        
        Position footmanLoc = unitLocations.get(myFootmanID);
        int closestDistance = footmanLoc.chebyshevDistance(unitLocations.get(enemyFootmanID));
        for (Integer curEnemy : enemyFootmen) {
            Position enemyLoc = unitLocations.get(curEnemy);
            if (footmanLoc.chebyshevDistance(enemyLoc) < closestDistance) return false;
        }
        return true;
    }

    /**
     * You give me the footman's ID, a list of enemy IDs
     *      I give you the number of adjacent enemy footmen
     * @param footman - the footman to use the reference point of
     * @param enemyFootmen - the enemy footmen in the game state
     * @param unitLocations - the unit locations in the game state
     * @return the number of enemies adjacent to the given footman
     */
    private static int getAdjacentEnemyCount(Integer footman, List<Integer> enemyFootmen, Map<Integer, Position> unitLocations) {
        int adjacent = 0;
        Position footmanPos = unitLocations.get(footman);
        for (Integer enemy : enemyFootmen) {
            Position enemyPos = unitLocations.get(enemy);
            if (footmanPos.isAdjacent(enemyPos)) adjacent++;
        }
        return adjacent;
    }

}
