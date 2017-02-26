package nl.tue.s2id90.group27;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.MIN_VALUE;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
        Move bestMove = null;
        bestValue = 0;
        DraughtsNode node = new DraughtsNode(s);    // the root of the search tree
        try {
            // compute bestMove and bestValue in a call to alphabeta
            bestValue = iterativeDeepening(node, MIN_VALUE, MAX_VALUE, maxSearchDepth);
            // I think above call will need to be replaced by call to iterative deepening, which then calls alphabeta 
            //, of course it was not 100% necessary to make a new function for iterative deepening, but I just didn't want merge conflicts

            // store the bestMove found uptill now
            // NB this is not done in case of an AIStoppedException in alphaBeat()
            bestMove = node.getBestMove();

            // print the results for debugging reasons
            System.err.format(
                    "%s: depth= %2d, best move = %5s, value=%d\n",
                    this.getClass().getSimpleName(), maxSearchDepth, bestMove, bestValue
            );
        } catch (AIStoppedException ex) {
            /* nothing to do */        }

        if (bestMove == null) {
            System.err.println("no valid move found!");
            return getRandomValidMove(s);
        } else {
            return bestMove;
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
                int mValue = alphaBeta(mNode, alpha, beta, depth - 1);
                state.undoMove(m);
                if (mValue > bestValue) {
                    bestValue = mValue;
                    node.setBestMove(m);
                }
                alpha = Math.max(alpha, bestValue);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        } else {
            int bestValue = Integer.MAX_VALUE / 2;
            for (Move m : state.getMoves()) {
                state.doMove(m);
                DraughtsNode mNode = new DraughtsNode(state);
                int mValue = alphaBeta(mNode, alpha, beta, depth - 1);
                state.undoMove(m);
                if (mValue < bestValue) {
                    bestValue = mValue;
                    node.setBestMove(m);
                }
                beta = Math.min(beta, bestValue);
                if (beta <= alpha) {
                    break;
                }
            }
            return bestValue;
        }
    }

    /**
     * A method that evaluates the given state.
     */
    // ToDo: write an appropriate evaluation function
    int evaluate(DraughtsState state) {
        int[] pieces = state.getPieces(); //obtain pieces array
        int color; //the color of checkers that the bot controls
        int[] tileCounts = new int[5]; //for each i in this array, it contains the number
        //of tiles that have the enum value i (e.g. int[0] indicates number of empty fields)
        for (int i = 1; i <= 50; i++) {
            int piece = pieces[i];
            tileCounts[piece]++;
        }
        //System.err.println(Arrays.toString(tileCounts));
        if (state.isWhiteToMove()) { //TODO: can't find better way to check what color the bot is, it must be somewhere
            color = 1;
            System.out.println("The bot has the white checkers");
        } else {
            color = 2;
            System.out.println("The bot has the black checkers");
        }
        //for now not really taking the king into account, but a king could be heuristically worth 3 or so normal pieces
        return tileCounts[color] + tileCounts[color + 2]; //for white, color = 1, so 1+2=3 represents the whitekings
        //and for black color=2, so 2+2=4 which is the value for black kings also
    }

    /**
     * A method implementing iterative deepening which then calls AlphaBeta
     */
    int iterativeDeepening(DraughtsNode node, int alpha, int beta, int maxDepth) throws AIStoppedException {
        //TODO: not sure if there is another stopping condition (except of course for the exception), like in the slides it says until result found
        //but is there ever really a 'result' found?
        int value = 0;
        for (int depth = 1; depth <= maxDepth; depth++) { //iterative deepening starts at the lowest depth possible and then keeps increasing depth
            value = alphaBeta(node, alpha, beta, depth);
        }
        return value;
    }
}
