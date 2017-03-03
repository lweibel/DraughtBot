package nl.tue.s2id90.group27;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import nl.tue.s2id90.draughts.DraughtsState;
import nl.tue.s2id90.draughts.player.DraughtsPlayer;
import org10x10.dam.game.Move;

/**
 * Implementation of the DraughtsPlayer interface.
 *
 * @author Luca Weibel and Michiel Verburg
 */
public class DraughtBot extends DraughtsPlayer {

    private int bestValue = 0;
    int maxSearchDepth;

    /**
     * boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    public DraughtBot(int maxSearchDepth) {
        super("best.png");
        this.maxSearchDepth = maxSearchDepth;
    }

    @Override
    public Move getMove(DraughtsState s) {
        //Move bestMove = null;
        bestValue = 0;
        DraughtsNode node = new DraughtsNode(s);    // the root of the search tree
        try {
            // compute bestMove and bestValue in a call to alphabeta
            bestValue = iterativeDeepening(node, MIN_VALUE, MAX_VALUE, maxSearchDepth);

            // store the bestMove found uptill now
            // NB this is not done in case of an AIStoppedException in alphaBeat()
            //bestMove = node.getBestMove();

            // print the results for debugging reasons
            
        } catch (AIStoppedException ex) {
            /* nothing to do */        }
        
        System.err.format(
                    "%s: depth= %2d, best move = %5s, value=%d\n",
                    this.getClass().getSimpleName(), maxSearchDepth, node.getBestMove(), bestValue
            );
        if (node.getBestMove() == null) {
            System.err.println("no valid move found!");
            return getRandomValidMove(s);
        } else {
            return node.getBestMove();
        }
    }

    /**
     * This method's return value is displayed in the AICompetition GUI.
     *
     * @return the value for the draughts state s as it is computed in a call to
     * getMove(s).
     */
    @Override
    public Integer getValue() {
        return bestValue;
    }

    /**
     * Tries to make alphabeta search stop. Search should be implemented such
     * that it throws an AIStoppedException when boolean stopped is set to true;
     *
     */
    @Override
    public void stop() {
        stopped = true;
    }

    /**
     * returns random valid move in state s, or null if no moves exist.
     */
    Move getRandomValidMove(DraughtsState s) {
        List<Move> moves = s.getMoves();
        Collections.shuffle(moves);
        return moves.isEmpty() ? null : moves.get(0);
    }

    /**
     * Implementation of alphabeta that automatically chooses the white player
     * as maximizing player and the black player as minimizing player.
     *
     * @param node contains DraughtsState and has field to which the best move
     * can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the computed value of this node
     * @throws AIStoppedException
     *
     */
    // TODO Do we need to check which color the bot is, or is it assumed that it is white?
    int alphaBeta(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        if (node.getState().isWhiteToMove()) {
            return alphaBetaMax(node, alpha, beta, depth);
        } else {
            return alphaBetaMin(node, alpha, beta, depth);
        }
    }

    /**
     * Does an alphabeta computation with the given alpha and beta where the
     * player that is to move in node is the minimizing player.
     *
     * <p>
     * Typical pieces of code used in this method are:
     * <ul> <li><code>DraughtsState state = node.getState()</code>.</li>
     * <li><code> state.doMove(move); .... ; state.undoMove(move);</code></li>
     * <li><code>node.setBestMove(bestMove);</code></li>
     * <li><code>if(stopped) { stopped=false; throw new AIStoppedException(); }</code></li>
     * </ul>
     * </p>
     *
     * @param node contains DraughtsState and has field to which the best move
     * can be assigned.
     * @param alpha
     * @param beta
     * @param depth maximum recursion Depth
     * @return the compute value of this node
     * @throws AIStoppedException thrown whenever the boolean stopped has been
     * set to true.
     */
    int alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        return combinedAlphaBetaMax(node, alpha, beta, depth, false);
    }

    int alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth)
            throws AIStoppedException {
        return combinedAlphaBetaMax(node, alpha, beta, depth, true);
    }

    int combinedAlphaBetaMax(DraughtsNode node, int alpha, int beta, int depth, boolean isMaximizing)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        if (depth <= 0 || state.isEndState()) {
            return evaluate(state);
        }
        if (isMaximizing) {
            int bestValue = -Integer.MAX_VALUE / 2;
            for (Move m : state.getMoves()) {
                state.doMove(m);
                // root: not sure if just can use node
                DraughtsNode mNode = new DraughtsNode(state);
                int mValue = alphaBetaMin(mNode, alpha, beta, depth - 1);
                state.undoMove(m);
                if (mValue > bestValue) {
                    /** debugging info start **/
                    if (depth > 1) {
                        Map<Integer, Move> bests = mNode.getBestMoves();
                        for (int i = (maxSearchDepth + 1) - depth; i < maxSearchDepth; i++) {
                            node.setBestMoveDepth(bests.get(i), i);
                        }
                    }
                    node.setBestMoveDepth(m, maxSearchDepth-depth);
                    /** debugging info end **/
                    bestValue = mValue;
                    node.setBestMoveCurrentDepth(m);
                }
                alpha = Math.max(alpha, bestValue);
                if (alpha >= beta) {
                    break;
                }
            }
            return bestValue;
        } else {
            int bestValue = Integer.MAX_VALUE / 2;
            for (Move m : state.getMoves()) {
                state.doMove(m);
                DraughtsNode mNode = new DraughtsNode(state);
                int mValue = alphaBetaMax(mNode, alpha, beta, depth - 1);
                state.undoMove(m);
                if (mValue < bestValue) {
                   /** debugging info start **/
                    if (depth > 1) {
                        Map<Integer, Move> bests = mNode.getBestMoves();
                        for (int i = (maxSearchDepth + 1) - depth; i < maxSearchDepth; i++) {
                            node.setBestMoveDepth(bests.get(i), i);
                        }
                    }
                    node.setBestMoveDepth(m, maxSearchDepth-depth);
                    /** debugging info end **/
                    bestValue = mValue;
                    node.setBestMoveCurrentDepth(m);
                }
                beta = Math.min(beta, bestValue);
                if (alpha >= beta) {
                    break;
                }
            }
            return bestValue;
        }
    }

    /**
     * A method that evaluates the given state.
     */
    int evaluate(DraughtsState state) {
        int[] pieces = state.getPieces(); //obtain pieces array
        //int color; //the color of checkers that the bot controls
        int[] tileCounts = new int[5]; //for each i in this array, it contains the number
        //of tiles that have the enum value i (e.g. int[0] indicates number of empty fields)
        for (int i = 1; i <= 50; i++) {
            int piece = pieces[i];
            tileCounts[piece]++;
        }

        //3*no. of kings cuz king worth 3 pieces
        return (tileCounts[1] + 3 * tileCounts[3]) - (tileCounts[2] + 3 * tileCounts[4]);
    }

    /**
     * A method implementing iterative deepening which then calls AlphaBeta
     */
    int iterativeDeepening(DraughtsNode node, int alpha, int beta, int maxDepth) throws AIStoppedException {
        int value = 0;

        //#TODO: right now iterative deepening seems to do double work, making it slow
        for (int depth = 1; depth <= maxDepth; depth++) { //iterative deepening starts at the lowest depth possible and then keeps increasing depth
            value = alphaBeta(node, alpha, beta, depth);
            System.err.println("at depth: " + depth + " the best value is: " + value);
            System.err.println("bestMoves: " + node.getBestMoves());
            node.setBestMove(node.getBestMoveCurrentDepth());
        }
        return value;
    }
}
