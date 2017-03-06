package nl.tue.s2id90.group27;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import nl.tue.s2id90.draughts.DraughtsState;
import org10x10.dam.game.Move;

/**
 * A class representing a node in the search tree for the draughts game. 
 * An object of this class contains a draughts state. By adapting the draughts state
 * it becomes a representation of a different node in the search tree.
 * The get/setBestMove methods are intended for storing/retrieving the best move
 * as it has been computed for the draughts state in this node.
 * @author huub
 */
public class DraughtsNode {
    private final DraughtsState state;
    private Move move, bestMoveCurrentDepth;
    private int bestValue;
    private Map<Integer, Move> moves = new HashMap<Integer, Move>();
    public DraughtsNode(DraughtsState s) {
        this.state = s;
    }
    
    public DraughtsState getState() {
        return state;
    }
    
    public void setBestMove(Move m) {
        this.move = m;
    }
    
    public Move getBestMove() {
        return move;
    }
    
    public void setBestMoveCurrentDepth(Move m) {
        this.bestMoveCurrentDepth = m;
    }
    
    public Move getBestMoveCurrentDepth() {
        return this.bestMoveCurrentDepth;
    }
    
    public void setBestValue(int value) {
        this.bestValue = value;
    }
    
    public int getBestValue() {
        return this.bestValue;
    }
    
    public Map<Integer, Move> getBestMoves() {
        return moves;
    }
    
    public void resetBestMoves() {
        this.moves =  new HashMap<Integer, Move>();
    }
    
    //To enhance readability we reverse the map
    public String getBestMovesString() {
        Map<Integer, Move> newMap = new HashMap<Integer, Move>();
        int maxDepth = Collections.max(this.moves.keySet());
        for (Integer key: this.moves.keySet()) {
            newMap.put(maxDepth - key, this.moves.get(key));
        }
        return newMap.toString();
    }
    
    public void setBestMoveDepth(Move m, int i) {
        this.moves.put(i, m);
    }
}
