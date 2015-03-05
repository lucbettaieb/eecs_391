package edu.cwru.sepia.agent.minimax;

/**
 * Needed an object to pass around an int value.
 * Integer is immutable, so it didn't work.  This does.
 */
public class Freedom {
    int freedom;

    public Freedom() {
        this.freedom = 0;
    }
    public void add(){this.freedom++;}
    public int getFreedom(){return this.freedom;}
}
