package edu.cwru.sepia.agent.planner;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by aidan on 3/27/15.
 */

/**
 * a "pool" of STRIPS "objects" used to describe the world
 * query the pool with preconditions, and change it with postconditions
 * 
 * 
 * Use: a StripsPool represents the world state.
 * * Beside(Peasant, Townhall) 
 * * Beside(Peasant, Gold)
 * * Nonempty(Peasant)
 * * 
 * A StripsSentence represents a single state
 * * Beside(Peasant, Gold)
 * * * These are currently unordered sets, so keep that in mind, or change it to an ordered list
 * * 
 * A StripsAtom represents a single enumerated word allowed in the language
 * * DEPOSIT, HARVEST, GO, BESIDE, etc...
 * 
 * So you build your Sentences from Atoms, then you build your Pool from Sentences.
 * 
 * i.e. to make the aforementioned Pool, we do the following.
 * StripsSentence one = new StripsSentence(BESIDE, PEASANT, TOWNHALL); 
 * StripsSentence two = new StripsSentence(GOLD, PEASANT, BESIDE);//ordering doesn't matter.
 * StripsSentence three=new StripsSentence(NONEMPTY, PEASANT); 
 * StripsPool pool = new StripsPool(one, two);
 * if(pool.meetsPreconditions(three)){
 *      pool.add(three);    //just demonstrating how to add things after initializing.
 * } 
 */
public class StripsPool {
    
    public Set<StripsSentence> pool;
    
    enum StripsAtom{
        DEPOSIT("DEPOSIT"), HARVEST("HARVEST"), GO("GO"), BESIDE("BESIDE"), CREATE("CREATE"), TOWNHALL("TOWNHALL"),
        PEASANT("PEASANT"), WOOD("WOOD"), GOLD("GOLD"), FOOD("FOOD"), ONE("1"), TWO("2"), THREE("3"), EMPTY("EMPTY"),
        NONEMPTY("NONEMPTY");

        private String representation;
        StripsAtom(String s){
            this.representation = s;
        }
        @Override
        public String toString(){
            return representation;
        }
    }

    public StripsPool(){
        this.pool = new HashSet<>();
    }
    public StripsPool(StripsSentence... sentences){
        this.pool = new HashSet<>();
        this.pool.addAll(Arrays.asList(sentences));
    }
    
    public StripsPool add(StripsSentence object){
        if(!meetsPreconditions(object)) return null;
        this.pool.removeAll(object.postConditions);
        this.pool.add(object);
        return this;//all about dat chaining
    }
    
    public boolean meetsPreconditions(StripsSentence object){
        return pool.containsAll(object.preconditions);
    }
    
    class StripsSentence {
        Set<StripsSentence> preconditions;
        Set<StripsSentence> postConditions;
        Set<StripsAtom> sentence;
        public StripsSentence(StripsAtom... stripsAtoms){
            this.preconditions = new HashSet<>();
            this.postConditions = new HashSet<>();
            this.sentence = new HashSet<>();
            this.sentence.addAll(Arrays.asList(stripsAtoms));
        }
        
        @Override
        public String toString(){
            StringBuilder returnVar = new StringBuilder();
            for(StripsAtom atom: sentence) returnVar.append(atom.toString()).append(' ');
            return returnVar.toString().trim();
        }
    }
}
