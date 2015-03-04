package edu.cwru.sepia.agent.minimax;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class MinimaxAlphaBeta extends Agent {

    private final int numPlys;
    private boolean AMIMAX = false;

    public MinimaxAlphaBeta(int playernum, String[] args)
    {
        super(playernum);

        if(args.length < 1)
        {
            System.err.println("You must specify the number of plys");
            System.exit(1);
        }

        numPlys = Integer.parseInt(args[0]);
    }

    @Override
    public Map<Integer, Action> initialStep(State.StateView newstate, History.HistoryView statehistory) {
        return middleStep(newstate, statehistory);
    }

    @Override
    public Map<Integer, Action> middleStep(State.StateView newstate, History.HistoryView statehistory) {
        GameStateChild bestChild = alphaBetaSearch(new GameStateChild(newstate),
                numPlys,
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY);

        return bestChild.action;
    }

    @Override
    public void terminalStep(State.StateView newstate, History.HistoryView statehistory) {

    }

    @Override
    public void savePlayerData(OutputStream os) {

    }

    @Override
    public void loadPlayerData(InputStream is) {

    }

    /**
     * You will implement this.
     *
     * This is the main entry point to the alpha beta search. Refer to the slides, assignment description
     * and book for more information.
     *
     * Try to keep the logic in this function as abstract as possible (i.e. move as much SEPIA specific
     * code into other functions and methods)
     *
     * @param node The action and state to search from
     * @param depth The remaining number of plys under this node
     * @param alpha The current best value for the maximizing node from this node to the root
     * @param beta The current best value for the minimizing node from this node to the root
     * @return The best child of this node with updated values
     * 
     * There's some dirty tricks in here that lead to incredible improvements.
     *      Minimax doesn't care about the state, just its heuristic.  I only care about
     *      the best state.  So only generate the state if we're redefining the best
     */
    public GameStateChild alphaBetaSearch(GameStateChild node, int depth, double alpha, double beta)
    {/*         minimax:
         http://en.wikipedia.org/wiki/Minimax#Pseudocode
        
                pruning:
         http://www.cs.trincoll.edu/~ram/cpsc352/notes/minimax.html
         http://en.wikipedia.org/wiki/Alpha%E2%80%93beta_pruning#Pseudocode
        */
        if(node.action != null){//this node has an outstanding action.  Perform it to get the node.
            node = new GameStateChild(node.action, new GameState(ActionApplier.apply(
                    node.action, node.state.stateView).getView(node.state.stateView.getPlayerNumbers()[0])));
        }
        
        this.flipPlayer();//initialized to false, so first flip sets it true
        node.state.flipPlayer();
        
        if(depth == 0 || node.state.getChildren()==null || node.state.getChildren().isEmpty()) return node;
        GameStateChild returnVar = null;
        if(AMIMAX){//MAX is playing
            double value = Double.NEGATIVE_INFINITY;
            for(GameStateChild child: node.state.getUnappliedChildren()){//action,state tuple.  Action unapplied to state
                double childValue = ActionApplier.applyHeuristic(child.action, child.state.stateView);
                if(childValue > value){
                    value = childValue;
                    returnVar = alphaBetaSearch(child, depth-1, alpha, beta);//actually make the state only if we have to
                }
                alpha = Math.max(value, alpha);
                if(beta <= alpha) break; //beta-pruned
            }
            return returnVar;
        } else {//MIN is playing
            double value = Double.POSITIVE_INFINITY;
            for(GameStateChild child: node.state.getUnappliedChildren()){//action,state tuple.  Action unapplied to state
                double childValue = ActionApplier.applyHeuristic(child.action, child.state.stateView);
                if(childValue < value){
                    value = childValue;
                    returnVar = alphaBetaSearch(child, depth-1, alpha, beta);//actually make the state only if we have to
                }
                beta = Math.min(value, beta);
                if(beta <= alpha) break; //alpha-pruned
            }
            return returnVar;
        }
    }//end of minimax

    /**
     * You will implement this.
     *
     * Given a list of children you will order them according to heuristics you make up.
     * See the assignment description for suggestions on heuristics to use when sorting.
     *
     * Use this function inside of your alphaBetaSearch method.
     *
     * Include a good comment about what your heuristics are and why you chose them.
     *
     * @param children the children nodes to sort
     * @return The list of children sorted by your heuristic.
     */
    public static ArrayList<GameStateChild> orderChildrenWithHeuristics(ArrayList<GameStateChild> children)
    {//this is static, and actually gets called on child generation, instead of at search time
        //orders nodes with higher utility first.
        /*
         * Insertion sort.  Because it has low overhead and complexity doesn't matter much on small search sizes
          *  for i = 1 to length(A) - 1
                x = A[i]
                j = i
                while j > 0 and A[j-1] > x
                    A[j] = A[j-1]
                    j = j - 1
                A[j] = x*
         */
        
        int i = 0;
        //gotta work on a copy of the list because you can't play with
        //the variable you're iterating through in a for-each loop
        
        for(GameStateChild x: children){//stand back kids, we're rolling our own for loop
            if(i==0){
                i++;
                continue;//cool, I've never used a continue before.  It skips the current loop iteration.
            }
            int j = i;
            while(j>0 && children.get(j-1).state.getUtility()>x.state.getUtility()){
                children.set(j, children.get(j-1));
                j--;
            }
            children.set(j, x);
            i++;
        }
        Collections.reverse(children);//the sorting algorithm put lowest utility first.  Flip it.
        return children;
    }
    
    private void flipPlayer(){this.AMIMAX = !AMIMAX;}
}
