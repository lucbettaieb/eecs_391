package edu.cwru.sepia.agent.minimax;

import com.sun.tools.javac.util.ArrayUtils;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * a Vector implementation for pathfinding with vectors
 * 
 * The crowflies path is considered optimal, but movement restrictions and blocks make 
 * execution of this path impossible.  So this object is designed to help implementation.
 * You give it a source, destination, and information on the map.
 * 
 * It will rank what it thinks the best next moves are, based on the crowflies vector
 * and the congestion around you on the map 
 */
public class Vector {
    int i, j, x, y;
    double magnitude;
    List<ResourceNode.ResourceView> blocks;
    int xExtent;
    int yExtent;
    public Vector(Unit.UnitView source, Unit.UnitView destination, 
                  List<ResourceNode.ResourceView> blocks, int xExtent, int yExtent){
        this.x = source.getXPosition();
        this.y = source.getYPosition();
        this.i = destination.getXPosition() - source.getXPosition();
        this.j = -1*(destination.getYPosition() - source.getYPosition());
        this.magnitude = Math.sqrt(Math.pow(i, 2) + Math.pow(j, 2));
        this.blocks = blocks;
        this.xExtent = xExtent;
        this.yExtent = yExtent;
    }

    /**
     * you give me a position, I tell you if something's occupying that position 
     * @param x x coordinate in question
     * @param y y coordinate in question
     * @return whether the coordinate pair is occupied
     */
    private boolean isBlockAt(int x, int y){
        for(ResourceNode.ResourceView block: blocks){
            if(block.getXPosition() == x && block.getYPosition() == y) return true;
        }
        return false;
    }

    /**
     * you give me a coordinate pair, I tell you if that position is in bounds 
     * @param x x coordinate in question
     * @param y y coordinate in question
     * @return whether the coordinate pair is in bounds
     */
    private boolean isInBounds(int x, int y){
        return !(x>xExtent || y>yExtent|| x<0 || y<0);
    }

    /**
     * checks the northward direction for trees 
     * @return distance until a tree was seen, capped at the magnitude of the vector
     * between source and destination 
     */
    private double checkNorth(){
        for(int i = 0; i<=magnitude; i++){
            if(isBlockAt(x,y-i) || !isInBounds(x, y - i)){
                return i;
            }
        }
        return magnitude;
    }

    /**
     * checks the southward direction for trees 
     * @return distance until a tree was seen, capped at the magnitude of the vector
     * between source and destination 
     */
    private double checkSouth(){
        for(int i = 0; i<=magnitude; i++){
            if(isBlockAt(x, y + i) || !isInBounds(x,y+i)){
                return i;
            }
        }
        return magnitude;
    }

    /**
     * checks the eastward direction for trees 
     * @return distance until a tree was seen, capped at the magnitude of the vector
     * between source and destination 
     */
    private double checkEast(){
        for(int i = 0; i<=magnitude; i++){
            if(isBlockAt(x + i, y) || !isInBounds(x+i,y)){
                return i;
            }
        }
        return magnitude;
    }

    /**
     * checks the westward direction for trees 
     * @return distance until a tree was seen, capped at the magnitude of the vector
     * between source and destination 
     */
    private double checkWest(){
        for(int i = 0; i<=magnitude; i++){
            if(isBlockAt(x - i, y) || !isInBounds(x-i,y)){
                return i;
            }
        }
        return magnitude;
    }

    /**
     * gets the best crowflies next actions, ranked by their utility
     * i.e. the first direction is guaranteed to get you closer, while the
     * last is guaranteed to take you further from your destination 
     * @return a list of directions, ordered by their desirability
     */
    private List<Direction> intendedNextDirections(){
        Map<Integer, Direction> directionOrder = new HashMap<Integer, Direction>();//1 is most wanted, 4 is least wanted
        if(Math.abs(i)>= Math.abs(j)){//delta x > delta y
            if(i < 0) {
                directionOrder.put(1, Direction.WEST);
                directionOrder.put(4, Direction.EAST);
                if (j < 0) {
                    directionOrder.put(2,Direction.SOUTH);
                    directionOrder.put(3, Direction.NORTH);
                } else {
                    directionOrder.put(2, Direction.NORTH);
                    directionOrder.put(3, Direction.SOUTH);
                }
            } else {
                directionOrder.put(1,Direction.EAST);
                directionOrder.put(4, Direction.WEST);
                if (j < 0) {
                    directionOrder.put(2,Direction.NORTH);
                    directionOrder.put(3, Direction.SOUTH);
                } else {
                    directionOrder.put(2, Direction.SOUTH);
                    directionOrder.put(3, Direction.NORTH);
                }
            }
        } else {
            if (j < 0) {
                directionOrder.put(1,Direction.SOUTH);
                directionOrder.put(4, Direction.NORTH);
                if (i < 0) {
                    directionOrder.put(2,Direction.WEST);
                    directionOrder.put(3, Direction.EAST);
                } else {
                    directionOrder.put(2, Direction.EAST);
                    directionOrder.put(3, Direction.WEST);
                }
            } else {
                directionOrder.put(1, Direction.NORTH);
                directionOrder.put(4, Direction.SOUTH);
                if (i < 0) {
                    directionOrder.put(2,Direction.WEST);
                    directionOrder.put(3, Direction.EAST);
                } else {
                    directionOrder.put(2, Direction.EAST);
                    directionOrder.put(3, Direction.WEST);
                }
            }
        }
        //yes, I know that's an atrocious way of doing things.  I'm just trying to make this work first.
        List<Direction> returnVar = new ArrayList<Direction>();
        for(int i =1; i<=4; i++) returnVar.add(directionOrder.get(i));
        return returnVar;
    }

    /**
     * heuristically ranks the directions.  Takes the crowflies direction ranking
     * (intendedNextDirections), and weights them based on the congestion of the map in that direction
     * 
     * @return a list of directions, ordered by their desirability
     */
    public List<Direction> getBestNextDirection(){
        
        List<Direction> bestCrowsDirection = intendedNextDirections();
        //ordered list, with best interruption-free direction first
        
        double northBlockness = checkNorth() / magnitude;
        double northRanking = 0d;
        double southBlockness = checkSouth() / magnitude;
        double southRanking = 0d;
        double eastBlockness = checkEast() / magnitude;
        double eastRanking = 0d;
        double westBlockness = checkWest() / magnitude;
        double westRanking = 0d;
        //0 to 1, depending on how clear the path is.
        
        int weightArray[] = new int[]{4,2,-2,-4};//index aligned with bestCrowsDirection
        
        for(int i = 0; i<bestCrowsDirection.size(); i++){
            switch (bestCrowsDirection.get(i)){
                case NORTH:
                    northRanking = northBlockness * weightArray[i];
                    break;
                case SOUTH:
                    southRanking = southBlockness * weightArray[i];
                    break;
                case EAST:
                    eastRanking = eastBlockness * weightArray[i];
                    break;
                case WEST:
                    westRanking = westBlockness * weightArray[i];
                    break;
            }
        }
        double rankings[] = new double[]{northRanking, southRanking, eastRanking, westRanking};
        Arrays.sort(rankings);
        reverse(rankings);
        Map<Double, Direction> rankingMap = new HashMap<Double, Direction>();
        rankingMap.put(northRanking, Direction.NORTH);
        rankingMap.put(southRanking, Direction.SOUTH);
        rankingMap.put(eastRanking, Direction.EAST);
        rankingMap.put(westRanking, Direction.WEST);
        bestCrowsDirection.clear();
        
        for(double key: rankings){
            bestCrowsDirection.add(rankingMap.get(key));
        }
        return bestCrowsDirection;
    }
    
    private static void reverse(double[] data) {
        int left = 0;
        int right = data.length - 1;

        while( left < right ) {
            // swap the values at the left and right indices
            double temp = data[left];
            data[left] = data[right];
            data[right] = temp;

            // move the left and right index pointers in toward the center
            left++;
            right--;
        }
    }
    
}
