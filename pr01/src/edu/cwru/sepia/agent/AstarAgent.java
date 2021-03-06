package edu.cwru.sepia.agent;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

@SuppressWarnings("serial")
public class AstarAgent extends Agent {

    /**
     * object used to render a node on the map
     * Added A* values to it (f,c,h)
     * Added a parent, to allow for backtracking
     */
    class MapLocation implements Comparable {
        public int x, y;
        public int f = 0, c = 0, h = 0;
        private MapLocation parent;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost) {
            this.x = x;
            this.y = y;
            this.parent = cameFrom;
            this.c = (this.parent != null) ? this.parent.c + (int)cost : (int) cost;
        }

        public MapLocation setc(int givenC) {
            this.c = givenC;
            this.f = this.c + this.h;
            return this;
        }

        public MapLocation seth(int givenH) {
            this.h = givenH;
            this.f = this.c + this.h;
            return this;
        }

        public MapLocation getParent() {
            return this.parent;
        }
        
        @Override
        public String toString(){
            String position = String.format("Position: (%d,%d)\n", x, y);
            String function = String.format("Heuristic: %d\n", this.h);
            String cost = String.format("Cost: %d\n", this.c);
            return String.format("%s%s%s", position, function, cost);
        }
        
        @Override
        public int compareTo(Object o){
        	if (!( o instanceof MapLocation)) return -1;
        	return this.f - ((MapLocation)o).f;
        }
        
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs
    
    private float replanProbability = 0f;

    public AstarAgent(int playernum) {
        super(playernum);

        System.out.println("Constructed AstarAgent");
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        // get the footman location
        List<Integer> unitIDs = newstate.getUnitIds(playernum);

        if (unitIDs.size() == 0) {
            System.err.println("No units found!");
            return null;
        }

        footmanID = unitIDs.get(0);

        // double check that this is a footman
        if (!newstate.getUnit(footmanID).getTemplateView().getName().equals("Footman")) {
            System.err.println("Footman unit not found");
            return null;
        }

        // find the enemy playernum
        Integer[] playerNums = newstate.getPlayerNumbers();
        int enemyPlayerNum = -1;
        for (Integer playerNum : playerNums) {
            if (playerNum != playernum) {
                enemyPlayerNum = playerNum;
                break;
            }
        }

        if (enemyPlayerNum == -1) {
            System.err.println("Failed to get enemy playernumber");
            return null;
        }

        // find the townhall ID
        List<Integer> enemyUnitIDs = newstate.getUnitIds(enemyPlayerNum);

        if (enemyUnitIDs.size() == 0) {
            System.err.println("Failed to find enemy units");
            return null;
        }

        townhallID = -1;
        enemyFootmanID = -1;
        for (Integer unitID : enemyUnitIDs) {
            Unit.UnitView tempUnit = newstate.getUnit(unitID);
            String unitType = tempUnit.getTemplateView().getName().toLowerCase();
            if (unitType.equals("townhall")) {
                townhallID = unitID;
            } else if (unitType.equals("footman")) {
                enemyFootmanID = unitID;
            } else {
                System.err.println("Unknown unit type");
            }
        }

        if (townhallID == -1) {
            System.err.println("Error: Couldn't find townhall");
            return null;
        }

        long startTime = System.nanoTime();
        path = findPath(newstate);
        totalPlanTime += System.nanoTime() - startTime;

        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        long startTime = System.nanoTime();
        long planTime = 0;

        Map<Integer, Action> actions = new HashMap<Integer, Action>();

        if (shouldReplanPath(newstate, statehistory, path)) {
            long planStartTime = System.nanoTime();
            path = findPath(newstate);
            planTime = System.nanoTime() - planStartTime;
            totalPlanTime += planTime;
        }

        Unit.UnitView footmanUnit = newstate.getUnit(footmanID);

        int footmanX = footmanUnit.getXPosition();
        int footmanY = footmanUnit.getYPosition();

        if (!path.empty() && (nextLoc == null || (footmanX == nextLoc.x && footmanY == nextLoc.y))) {

            // stat moving to the next step in the path
            nextLoc = path.pop();

            System.out.println("Moving to (" + nextLoc.x + ", " + nextLoc.y + ")");
        }

        if (nextLoc != null && (footmanX != nextLoc.x || footmanY != nextLoc.y)) {
            int xDiff = nextLoc.x - footmanX;
            int yDiff = nextLoc.y - footmanY;

            // figure out the direction the footman needs to move in
            Direction nextDirection = getNextDirection(xDiff, yDiff);

            actions.put(footmanID, Action.createPrimitiveMove(footmanID, nextDirection));
        } else {
            Unit.UnitView townhallUnit = newstate.getUnit(townhallID);

            // if townhall was destroyed on the last turn
            if (townhallUnit == null) {
                terminalStep(newstate, statehistory);
                return actions;
            }

            if (Math.abs(footmanX - townhallUnit.getXPosition()) > 1 ||
                    Math.abs(footmanY - townhallUnit.getYPosition()) > 1) {
                System.err.println("Invalid plan. Cannot attack townhall");
                totalExecutionTime += System.nanoTime() - startTime - planTime;
                return actions;
            } else {
                System.out.println("Attacking TownHall");
                // if no more movements in the planned path then attack
                actions.put(footmanID, Action.createPrimitiveAttack(footmanID, townhallID));
            }
        }

        totalExecutionTime += System.nanoTime() - startTime - planTime;
        return actions;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {
        System.out.println("Total turns: " + newstate.getTurnNumber());
        System.out.println("Total planning time: " + totalPlanTime / 1e9);
        System.out.println("Total execution time: " + totalExecutionTime / 1e9);
        System.out.println("Total time: " + (totalExecutionTime + totalPlanTime) / 1e9);
    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }


    /**
     * little object to represent vectors.
     * it's used to compare vector from me to obstacle, and from me to goal
     */
    private class Tuple { 
    	private static final float EPSILON = 0.00001f;
    	public float x,y; 
    	
    	public Tuple(float x, float y) { 
    		this.x = x; 
    	    this.y = y; 
    	  } 
    	  
    	public boolean equals(Object c){
    		if(! (c instanceof Tuple)) return false;
    		Tuple comparedTo = (Tuple) c;
    		return (Math.abs(this.x - comparedTo.x) < EPSILON) && (Math.abs(this.y - comparedTo.y) < EPSILON);
    	}
    	
    	public Tuple unit(){
    		Tuple unit = new Tuple(x, y);
    		float mag = this.magnitude();
        	unit.x /= mag;
        	unit.y /= mag;
        	return unit;
    	}
    	
    	public float magnitude(){
    		return (float) Math.sqrt(x*x+y*y);
    	}
    }
    
    /**
     * You will implement this method.
     * <p/>
     * This method should return true when the path needs to be replanned
     * and false otherwise. This will be necessary on the dynamic map where the
     * footman will move to block your unit.
     *
     * @param state
     * @param history
     * @param currentPath remaining steps in the current path
     * @return whether the path should be recalculated
     */
    private boolean shouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath) {
    	boolean test = true;
    	if(test) return newShouldReplanPath(state,history,currentPath);
    	
    	//if we have no path, then we have no need to replan.  We're done.
    	if(currentPath.size() <= 0) return false;
    	
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);
        Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
        
        boolean useVectorMethod = true;
        if(useVectorMethod){
        	//compare the vectors and replan if they're equal, or if enemy's location is within a predefined radius.
        	Tuple goal = new Tuple(townhallUnit.getXPosition() - footmanUnit.getXPosition(), townhallUnit.getYPosition() - footmanUnit.getYPosition());
        	Tuple replan = new Tuple(enemyFootmanUnit.getXPosition() - footmanUnit.getXPosition(), enemyFootmanUnit.getYPosition() - footmanUnit.getYPosition());
        	
        	return goal.unit().equals(replan.unit()) || replan.magnitude() < 2;
        } else {
        	//check if enemy's location is within our planned path
        	@SuppressWarnings("unchecked")//it works, ya dingus compiler.
    		Stack<MapLocation> path = (Stack<MapLocation>) currentPath.clone();
        	for(int i = 0; i<2 && !path.isEmpty(); i++){
        		MapLocation location = path.pop();
        		if(location.x == enemyFootmanUnit.getXPosition() && location.y == enemyFootmanUnit.getYPosition()) return true;
        	}
        	return false;
        }
    }//end of shouldReplanPath method
    
    private boolean newShouldReplanPath(State.StateView state, History.HistoryView history, Stack<MapLocation> currentPath){
    	@SuppressWarnings("unchecked")//it works, ya dingus compiler.
		Stack<MapLocation> path = (Stack<MapLocation>) currentPath.clone();
    	Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
    	if(enemyFootmanUnit == null) return false;
    	float pastReplanProbability = replanProbability;
    	
    	for(int i = 1; i<= 5 && ! path.isEmpty(); i++){
    		MapLocation inQuestion  = path.pop();
    		if(inQuestion.x == enemyFootmanUnit.getXPosition() && inQuestion.y == enemyFootmanUnit.getYPosition()){
    			replanProbability += 1/i;
    		}
    	}
    	if(replanProbability == pastReplanProbability) replanProbability -= 0.25f;
    	if(replanProbability < 0 || replanProbability > 2) replanProbability = 0;
    	return (replanProbability > 0.4f);
    }

    /**
     * This method is implemented for you. You should look at it to see examples of
     * how to find units and resources in Sepia.
     *
     * @param state
     * @return
     */
    private Stack<MapLocation> findPath(State.StateView state) {
        Unit.UnitView townhallUnit = state.getUnit(townhallID);
        Unit.UnitView footmanUnit = state.getUnit(footmanID);

        MapLocation startLoc = new MapLocation(footmanUnit.getXPosition(), footmanUnit.getYPosition(), null, 0);

        MapLocation goalLoc = new MapLocation(townhallUnit.getXPosition(), townhallUnit.getYPosition(), null, 0);

        MapLocation footmanLoc = null;
        if (enemyFootmanID != -1) {
            Unit.UnitView enemyFootmanUnit = state.getUnit(enemyFootmanID);
            footmanLoc = new MapLocation(enemyFootmanUnit.getXPosition(), enemyFootmanUnit.getYPosition(), null, 0);
        }

        // get resource locations
        List<Integer> resourceIDs = state.getAllResourceIds();
        Set<MapLocation> resourceLocations = new HashSet<MapLocation>();
        for (Integer resourceID : resourceIDs) {
            ResourceNode.ResourceView resource = state.getResourceNode(resourceID);

            resourceLocations.add(new MapLocation(resource.getXPosition(), resource.getYPosition(), null, 0));
        }

        return AstarSearch(startLoc, goalLoc, state.getXExtent(), state.getYExtent(), footmanLoc, resourceLocations);
    }

    /** O(7*n*n)
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     * As an example consider the following simple map
     * F - - - -
     * x x x - x
     * H - - - -
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     * The path would be
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     * Notice how the initial footman position and the townhall position are not included in the path stack
     *
     * @param start             Starting position of the footman
     * @param goal              MapLocation of the townhall
     * @param xExtent           Width of the map
     * @param yExtent           Height of the map
     * @param resourceLocations Set of positions occupied by resources
     * @return Stack of positions with top of stack being first move in plan
     */
    private Stack<MapLocation> AstarSearch(MapLocation start, MapLocation goal, int xExtent, int yExtent,
                                           MapLocation enemyFootmanLoc, Set<MapLocation> resourceLocations) {

        Queue<MapLocation> closedSet = new LinkedList<MapLocation>();//nodes already considered
        PriorityQueue<MapLocation> fastOpenSet = new PriorityQueue<MapLocation>();
        fastOpenSet.add(start);//gotta add the starting position to the open set
        
        while(fastOpenSet.size()>0){
            MapLocation fastQ = fastOpenSet.poll();
            
            Set<MapLocation> blocks = new HashSet<MapLocation>();
            blocks.addAll(resourceLocations);
            blocks.add(enemyFootmanLoc);//combine footmen and resources as 'things that can block me'
            for(MapLocation successor: possibleMoves(fastQ, blocks, xExtent, yExtent)){
                //for all possible moves from here:
                if(successor.x == goal.x && successor.y == goal.y) return generatePath(successor);//we're done!
                successor.setc(fastQ.c+1).seth(chebyshev(successor, goal));//f value is automagically calculated.
                if(shouldAddToOpenSet(successor, closedSet, fastOpenSet)) {
                	fastOpenSet.add(successor);
                }
            }
            closedSet.add(fastQ);
        }
        //No path was found.  Someone else will know this and exit.
        System.err.println("ERROR: NO PATH");
        System.exit(1);
        return null;//silly compiler, you know this won't execute...
    }

    /** O(openSet + closedSet)
     * Computes whether the given successor should be added to the open set of nodes to consider
     * @param successor node in consideration
     * @param closedSet nodes already considered
     * @param openSet nodes to consider
     * @return whether successor should be added to the open set
     */
    private boolean shouldAddToOpenSet(MapLocation successor, Queue<MapLocation> closedSet, PriorityQueue<MapLocation> openSet){
        boolean shouldAdd = true;

        for(MapLocation node: openSet){
            //skip: already going to visit it, with a better path
            if(node.f < successor.f && successor.x == node.x && successor.y == node.y) shouldAdd = false;
        }
        for(MapLocation node: closedSet){
            //skip: already visited it, with a better path
            if(node.f < successor.f && successor.x == node.x && successor.y == node.y) shouldAdd = false;
        }
        return shouldAdd;
    }

    /** O(n)
     * makes a path by recursively following the parent link from destination to source 
     * @param destination final position in the path
     * @return the path from source (on top) to destination, as a Stack of MapLocations
     */
    private Stack<MapLocation> generatePath(MapLocation destination){
        Stack<MapLocation> path = new Stack<MapLocation>();
        boolean isGoal = true;
        while(destination.parent != null){
            if(!isGoal) path.add(destination);
            isGoal = false;
            destination = destination.getParent();
        }
        return path;
    }

    /** O(7*blocks)
     * returns the list of possible moves from the given location 
     * @param parent node to create moves from
     * @param blocks list of invalid locations
     * @param xExtent x length of map
     * @param yExtent y length of map
     * @return set of valid moves
     */
    private Set<MapLocation> possibleMoves(MapLocation parent, Set<MapLocation> blocks, int xExtent, int yExtent){
        
    	Set<MapLocation> returnVar = new HashSet<MapLocation>();
        returnVar.add(new MapLocation(parent.x+1, parent.y+1, parent, 1f));
        returnVar.add(new MapLocation(parent.x+1, parent.y, parent, 1f));
        returnVar.add(new MapLocation(parent.x+1, parent.y-1, parent, 1f));
        returnVar.add(new MapLocation(parent.x, parent.y+1, parent, 1f));
        returnVar.add(new MapLocation(parent.x, parent.y-1, parent, 1f));
        returnVar.add(new MapLocation(parent.x-1, parent.y+1, parent, 1f));
        returnVar.add(new MapLocation(parent.x-1, parent.y, parent, 1f));
        returnVar.add(new MapLocation(parent.x-1, parent.y-1, parent, 1f));
        
        Set<MapLocation> invalidLocations = new HashSet<MapLocation>();//bad coding...
        for(MapLocation child: returnVar){
            if(child.x < 0 || child.x >= xExtent || child.y < 0 || child.y >= yExtent){
                invalidLocations.add(child);//out of bounds
            } else {
                for(MapLocation block: blocks){
                    //collision with resource or block
                    if(block != null && child.x == block.x && child.y == block.y)invalidLocations.add(child);
                }
            } 
        }
        for(MapLocation invalid : invalidLocations){
            returnVar.remove(invalid);
        }
        return returnVar;
    }

    /** O(n)
     * grabs the minimum 'f' value node in the given set.
     * TODO: change to priority queue?  
     * @param openSet the set to remove and return the minimum value from
     * @return the minimum value of the given set
     * 
     */
    private MapLocation getMinF(HashSet<MapLocation> openSet){
        MapLocation currentMin = null;
        for(MapLocation position: openSet){
            if(currentMin == null) currentMin = position;
            if(position.f < currentMin.f) currentMin = position;
        }
        openSet.remove(currentMin);
        return currentMin;
    }

    /**
     * The Chebyshev heuristic 
     * Returns the chebyshev distance
     * 
     * @param source        the beginning or current node on the map
     * @param destination   the desired destination on the map
     * @return the Chebyshev distance, rounded down to an integer.
     */
    private int chebyshev(MapLocation source, MapLocation destination) {
        int xDiff = Math.abs(source.x - destination.x);
        int yDiff = Math.abs(source.y - destination.y);
        
        return Math.max(xDiff, yDiff);
    }

    /**
     * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc)
     * This converts the difference between the current position and the
     * desired position to a direction.
     *
     * @param xDiff Integer equal to 1, 0 or -1
     * @param yDiff Integer equal to 1, 0 or -1
     * @return A Direction instance (e.g. SOUTHWEST) or null in the case of error
     */
    private Direction getNextDirection(int xDiff, int yDiff) {
    	//System.out.println("x dif: "+xDiff + " y dif: " + yDiff);
        // figure out the direction the footman needs to move in
        if (xDiff == 1 && yDiff == 1) {
            return Direction.SOUTHEAST;
        } else if (xDiff == 1 && yDiff == 0) {
            return Direction.EAST;
        } else if (xDiff == 1 && yDiff == -1) {
            return Direction.NORTHEAST;
        } else if (xDiff == 0 && yDiff == 1) {
            return Direction.SOUTH;
        } else if (xDiff == 0 && yDiff == -1) {
            return Direction.NORTH;
        } else if (xDiff == -1 && yDiff == 1) {
            return Direction.SOUTHWEST;
        } else if (xDiff == -1 && yDiff == 0) {
            return Direction.WEST;
        } else if (xDiff == -1 && yDiff == -1) {
            return Direction.NORTHWEST;
        }
        System.err.println("Invalid path. Could not determine direction");
        return null;
    }
}
