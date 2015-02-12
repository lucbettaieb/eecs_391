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
     * object used to render a node on the map.
     * Added A* values to it (f,c,h)
     * Added a parent, to allow for backtracking
     */
    class MapLocation {
        public int x, y;
        public int f = 0, c = 0, h = 0;
        private MapLocation parent;

        public MapLocation(int x, int y, MapLocation cameFrom, float cost) {
            this.x = x;
            this.y = y;
            this.parent = cameFrom;
            //if(this.parent != null) this.c = this.parent.getc() + (int)cost;
            this.c = (this.parent != null) ? this.parent.getc() + (int)cost : (int) cost;
        }

        public MapLocation setc(int givenC) {
            this.c = givenC;
            this.f = this.c + this.h;
            return this;
        }

        public int getc() {
            return this.c;
        }

        public MapLocation seth(int givenH) {
            this.h = givenH;
            this.f = this.c + this.h;
            return this;
        }

        public int getf() {
            return this.f;
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
    }

    Stack<MapLocation> path;
    int footmanID, townhallID, enemyFootmanID;
    MapLocation nextLoc;

    private long totalPlanTime = 0; // nsecs
    private long totalExecutionTime = 0; //nsecs

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
        return false;
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

    /**
     * This is the method you will implement for the assignment. Your implementation
     * will use the A* algorithm to compute the optimum path from the start position to
     * a position adjacent to the goal position.
     * <p/>
     * You will return a Stack of positions with the top of the stack being the first space to move to
     * and the bottom of the stack being the last space to move to. If there is no path to the townhall
     * then return null from the method and the agent will print a message and do nothing.
     * The code to execute the plan is provided for you in the middleStep method.
     * <p/>
     * As an example consider the following simple map
     * <p/>
     * F - - - -
     * x x x - x
     * H - - - -
     * <p/>
     * F is the footman
     * H is the townhall
     * x's are occupied spaces
     * <p/>
     * xExtent would be 5 for this map with valid X coordinates in the range of [0, 4]
     * x=0 is the left most column and x=4 is the right most column
     * <p/>
     * yExtent would be 3 for this map with valid Y coordinates in the range of [0, 2]
     * y=0 is the top most row and y=2 is the bottom most row
     * <p/>
     * resourceLocations would be {(0,1), (1,1), (2,1), (4,1)}
     * <p/>
     * The path would be
     * <p/>
     * (1,0)
     * (2,0)
     * (3,1)
     * (2,2)
     * (1,2)
     * <p/>
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
        HashSet<MapLocation> openSet = new HashSet<MapLocation>();//nodes to consider
        openSet.add(start.setc(0));//gotta add the starting position to the open set
        
        while(openSet.size()>0){
            MapLocation q = getMinF(openSet);//grab the minimum node, and pop it off the open set
            Set<MapLocation> blocks = new HashSet<MapLocation>();
            blocks.addAll(resourceLocations);
            blocks.add(enemyFootmanLoc);//combine footmen and resources as 'things that can block me'
            for(MapLocation successor: possibleMoves(q, blocks, xExtent, yExtent)){
                //for all possible moves from here:
                if(successor.x == goal.x && successor.y == goal.y) return generatePath(successor);//we're done!
                successor.setc(q.getc()+1).seth(chebyshevDistance(successor, goal));//f value is automagically calculated.
                if(shouldAddToOpenSet(successor, closedSet, openSet)) openSet.add(successor);
            }
            closedSet.add(q);
        }
        //No path was found.  Someone else will know this and exit.
        return null;
    }

    /**
     * Computes whether the given successor should be added to the open set of nodes to consider
     * @param successor node in consideration
     * @param closedSet nodes already considered
     * @param openSet nodes to consider
     * @return whether successor should be added to the open set
     */
    private boolean shouldAddToOpenSet(MapLocation successor, Queue<MapLocation> closedSet, HashSet<MapLocation> openSet){
        boolean shouldAdd = true;

        for(MapLocation node: openSet){
            //skip: already going to visit it, with a better path
            if(successor.x == node.x && successor.y == node.y && node.getf() < successor.getf()) shouldAdd = false;
        }
        for(MapLocation node: closedSet){
            //skip: already visited it, with a better path
            if(successor.x == node.x && successor.y == node.y && node.getf() < successor.getf()) shouldAdd = false;
        }
        return shouldAdd;
    }

    /**
     * makes a path by recursively following the parent link from destination to source 
     * @param destination final position in the path
     * @return the path from source (on top) to destination, as a Stack of MapLocations
     */
    private Stack<MapLocation> generatePath(MapLocation destination){
        Stack<MapLocation> path = new Stack<MapLocation>();
        while(destination.parent != null){
            path.add(destination);
            destination = destination.getParent();
        }
        return path;
    }

    /**
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
        returnVar.add(new MapLocation(parent.x, parent.y, parent, 1f));
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
                    if(child.x == block.x && child.y == block.y)invalidLocations.add(child);
                    //no backtracking
                    if(child.x == parent.x && child.y == parent.y) invalidLocations.add(child);
                }
            } 
        }
        for(MapLocation invalid : invalidLocations){
            returnVar.remove(invalid);
        }
        return returnVar;
    }

    /**
     * grabs the minimum 'f' value node in the given set.
     * @param openSet
     * @return
     */
    private MapLocation getMinF(HashSet<MapLocation> openSet){
        MapLocation currentMin = null;
        for(MapLocation position: openSet){
            if(currentMin == null) currentMin = position;
            if(position.getf() < currentMin.getf()) currentMin = position;
        }
        openSet.remove(currentMin);
        return currentMin;
    }

    /**
     * returns the Chebyshev heuristic value of the distance between two given nodes
     *
     * @param source      the beginning or current node on the map
     * @param destination the desired destination on the map
     * @return the Chebyshev heuristic distance between the nodes
     */
    private int chebyshevDistance(MapLocation source, MapLocation destination) {
        int xDiff = Math.abs(source.x - destination.x);
        int yDiff = Math.abs(source.y - destination.y);
        return Math.max(xDiff, yDiff);
        //TODO: subtract 1 because we go beside the goal?
    }

    /**
     * the Chebyshev heuristic underestimates too much in my opinion.
     * By using a hypotenuse of the points, we can more closely estimate the true cost.
     * This is a much more accurate method for 8-direction traversal, where diagonals are possible
     * The possible downside is that each heuristic calculation is more processor intensive, due to
     *      squaring and rooting
     * @param source        the beginning or current node on the map
     * @param destination   the desired destination on the map
     * @return the crow flies distance, rounded down to an integer.
     */
    private int crowsDistance(MapLocation source, MapLocation destination){
        int xDiff = Math.abs(source.x - destination.x);
        int yDiff = Math.abs(source.y - destination.y);
        return (int) Math.sqrt(Math.pow(xDiff,2)+ Math.pow(yDiff,2));
        //TODO: subtract 1 because we go beside the goal?
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
