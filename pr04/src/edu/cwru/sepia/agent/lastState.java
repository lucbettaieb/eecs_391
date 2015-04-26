package edu.cwru.sepia.agent;

import java.util.List;
import java.util.Map;

/**
 * Created by aidan on 4/25/15.
 */
public class lastState {

    protected List<Integer> myFootmen;//IDs of my footmen
    protected List<Integer> enemyFootmen;//IDs of footmen owned by ENEMY_PLAYERNUM
    protected Map<Integer, Integer> unitHealth;
    protected Map<Integer, Position> unitLocations;
    
    
    
    public lastState(){

    }
}
