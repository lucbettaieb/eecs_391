package edu.cwru.sepia.agent.minimax;

/**
 * Created by aidan on 3/4/15.
 */
public class Freedom {
    int freedom;

    public Freedom() {
        this.freedom = 0;
    }
    public void add(){this.freedom++;}
    public int getFreedom(){return this.freedom;}
}
