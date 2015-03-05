package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.util.*;

/**
 * Created by aidan on 3/4/15.
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
        this.i = GameState.deltaX(source, destination);
        this.j = GameState.deltaY(source, destination);
        this.magnitude = Math.sqrt(Math.pow(i, 2) + Math.pow(j, 2));
        this.blocks = blocks;
        this.xExtent = xExtent;
        this.yExtent = yExtent;
    }
    
    private boolean isBlockAt(int x, int y){
        for(ResourceNode.ResourceView block: blocks){
            if(block.getXPosition() == x && block.getYPosition() == y) return true;
        }
        return false;
    }
    
    private boolean isInBounds(int x, int y){
        return !(x>xExtent || y>yExtent|| x<0 || y<0);
    }

    private double checkNorth(){
        for(int i = 0; i<magnitude; i++){
            if(isBlockAt(x,y+i) || !isInBounds(x, y + i)){
                return i;
            }
        }
        return magnitude;
    }

    private double checkSouth(){
        for(int i = 0; i<magnitude; i++){
            if(isBlockAt(x, y - i) || !isInBounds(x,y-i)){
                return i;
            }
        }
        return magnitude;
    }

    private double checkEast(){
        for(int i = 0; i<magnitude; i++){
            if(isBlockAt(x + i, y) || !isInBounds(x+i,y)){
                return i;
            }
        }
        return magnitude;
    }

    private double checkWest(){
        for(int i = 0; i<magnitude; i++){
            if(isBlockAt(x - i, y) || !isInBounds(x-i,y)){
                return i;
            }
        }
        return magnitude;
    }

    private List<Direction> intendedNextDirections(){
        Map<Integer, Direction> directionOrder = new HashMap<Integer, Direction>();//1 is most wanted, 4 is least wanted
        boolean izero = false;
        boolean jzero = false;
        if(i == 0) {
            i = 1;
            izero = true;
        }
        if(j == 0) {
            j = 1;
            jzero = true;
        }
        if(Math.abs(i)>= Math.abs(j)){//i(x) value matters
            if(i / Math.abs(i) == -1) {
                directionOrder.put(1, Direction.SOUTH);
                directionOrder.put(4, Direction.NORTH);
                if (j / Math.abs(j) == -1) {
                    directionOrder.put(2,Direction.WEST);
                    directionOrder.put(3, Direction.EAST);
                } else {
                    directionOrder.put(3, Direction.EAST);
                    directionOrder.put(2, Direction.WEST);
                }
            }
            else {
                directionOrder.put(1,Direction.NORTH);
                directionOrder.put(4, Direction.SOUTH);
                if (j / Math.abs(j) == -1) {
                    directionOrder.put(2,Direction.WEST);
                    directionOrder.put(3, Direction.EAST);
                } else {
                    directionOrder.put(3, Direction.EAST);
                    directionOrder.put(2, Direction.WEST);
                }
            }
        } else {
            if (j / Math.abs(j) == -1) {
                directionOrder.put(1,Direction.WEST);
                directionOrder.put(4, Direction.EAST);
                if (i / Math.abs(i) == -1) {
                    directionOrder.put(2,Direction.SOUTH);
                    directionOrder.put(3, Direction.NORTH);
                } else {
                    directionOrder.put(3, Direction.NORTH);
                    directionOrder.put(2, Direction.SOUTH);
                }
            } else {
                directionOrder.put(1, Direction.EAST);
                directionOrder.put(4, Direction.WEST);
                if (i / Math.abs(i) == -1) {
                    directionOrder.put(2,Direction.SOUTH);
                    directionOrder.put(3, Direction.NORTH);
                } else {
                    directionOrder.put(3, Direction.NORTH);
                    directionOrder.put(2, Direction.SOUTH);
                }
            }
        }
        i = izero? 0 : i;
        j = jzero ? 0 : j;
        //yes, I know that's an atrocious way of doing things.  I'm just trying to make this work first.
        List<Direction> returnVar = new ArrayList<Direction>();
        for(int i =1; i<=4; i++) returnVar.add(directionOrder.get(i));
        return returnVar;
    }
    
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
        
        int weightArray[] = new int[]{5,2,-2,-5};//index aligned with bestCrowsDirection
        
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
    
}
