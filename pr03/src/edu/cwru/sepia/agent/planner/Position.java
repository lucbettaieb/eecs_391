package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.util.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Devin on 3/15/15.
 *
 * I've provided you with a simple Position class with some helper methods. Use this for any place you need to track
 * a location. If you need modify the methods and add new ones. If you make changes add a note here about what was
 * changed and why.
 *
 * This class is immutable, meaning any changes creates an entirely separate copy.
 */
public class Position {

    public final int x;
    public final int y;

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Deep copy of specified position.
     *
     * @param pos Position to copy
     */
    public Position(Position pos) {
        x = pos.x;
        y = pos.y;
    }

    /**
     * Gives the position one step in the specified direciton.
     *
     * @param direction North, south, east, etc.
     * @return Position one step away
     */
    public Position move(Direction direction) {
        return new Position(direction.xComponent() + x, direction.yComponent() + y);
    }

    /**
     * Returns a list of adjacent positions. This method does not check
     * if the positions are valid. So it may return locations outside of the
     * map bounds or positions that are occupied by other objects.
     *
     * @return List of adjacent positions
     */
    public List<Position> getAdjacentPositions() {
        List<Position> positions = new ArrayList<Position>();

        for (Direction direction : Direction.values()) {
            positions.add(move(direction));
        }

        return positions;
    }

    /**
     * Check if the position is within the map. Does not check if the position is occupied
     *
     * @param xExtent X dimension size of the map (get this from the StateView object)
     * @param yExtent Y dimension size of the map (get this from the StateView object)
     * @return True if in bounds, false otherwise.
     */
    public boolean inBounds(int xExtent, int yExtent) {
        return (x >= 0 && y >= 0 && x < xExtent && y < yExtent);
    }

    /**
     * Calculates the Euclidean distance between this position and another.
     * May be useful for your heuristic.
     *
     * @param position Other position to get distance to
     * @return Euclidean distance between two positions
     */
    public double euclideanDistance(Position position) {
        return Math.sqrt(Math.pow(x - position.x, 2) + Math.pow(y - position.y, 2));
    }

    /**
     * Calculates the Chebyshev distance between this position and another.
     * May be useful for your heuristic.
     *
     * @param position Other position to get distance to
     * @return Chebyshev distance between two positions
     */
    public int chebyshevDistance(Position position) {
        return Math.max(Math.abs(x - position.x), Math.abs(y - position.y));
    }

    /**
     * True if the specified position can be reached in one step. Does not check if the position
     * is in bounds.
     *
     * @param position Position to check for adjacency
     * @return true if adjacent, false otherwise
     */
    public boolean isAdjacent(Position position) {
        return Math.abs(x - position.x) <= 1 && Math.abs(y - position.y) <= 1;
    }

    /**
     * Get the direction for an adjacent position.
     *
     * @param position Adjacent position
     * @return Direction to specified adjacent position
     */
    public Direction getDirection(Position position) {
        int xDiff = position.x - x;
        int yDiff = position.y - y;

        // figure out the direction the footman needs to move in
        if(xDiff == 1 && yDiff == 1)
        {
            return Direction.SOUTHEAST;
        }
        else if(xDiff == 1 && yDiff == 0)
        {
            return Direction.EAST;
        }
        else if(xDiff == 1 && yDiff == -1)
        {
            return Direction.NORTHEAST;
        }
        else if(xDiff == 0 && yDiff == 1)
        {
            return Direction.SOUTH;
        }
        else if(xDiff == 0 && yDiff == -1)
        {
            return Direction.NORTH;
        }
        else if(xDiff == -1 && yDiff == 1)
        {
            return Direction.SOUTHWEST;
        }
        else if(xDiff == -1 && yDiff == 0)
        {
            return Direction.WEST;
        }
        else if(xDiff == -1 && yDiff == -1)
        {
            return Direction.NORTHWEST;
        }

        System.err.println("Position not adjacent. Could not determine direction");
        return null;
    }

    /**
     * Utility function. Allows you to check equality with pos1.equals(pos2) instead of manually checking if x and y
     * are the same.
     *
     * @param o Position to check equality with
     * @return true if x and y components are equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (x != position.x) return false;
        if (y != position.y) return false;

        return true;
    }

    /**
     * Utility function. Necessary for use in a HashSet or HashMap.
     *
     * @return hashcode for x and y value
     */
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    /**
     * @return human readable string representation.
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
