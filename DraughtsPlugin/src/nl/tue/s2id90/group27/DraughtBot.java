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

    final static int NRCOLUMNS = 5;
    final static int NRROWS = 10;
    final static int KING = 3000; //value of king
    final static int PIECE = 1000; //value of normal piece
    final static int ROWMULTIPLIER = 30; //for taking tempi into account
    final static int CORNERBACKRANK = 150; //value for corner in the back, less valuable as others
    final static int MIDDLEBACKRANK = 250; //value for middle in the backrank
    final static int DOUBLECORNER = 75; //play from the double corner (e.g. field 45 and 50)
    final static int SIDEPIECE = -100; //minus points for pieces on the left or right side because they can be blocked

    final static int LATEMIDGAME = 20; //after less than 20 pieces are left, center pieces become more valuable

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

        if (bestValue == 0) {
            bestValue = node.getBestValue();
        }
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
            return alphaBetaMax(node, alpha, beta, depth, true);
        } else {
            return alphaBetaMin(node, alpha, beta, depth, true);
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
    int alphaBetaMin(DraughtsNode node, int alpha, int beta, int depth, boolean firstTime)
            throws AIStoppedException {
        return combinedAlphaBetaMax(node, alpha, beta, depth, false, firstTime);
    }

    int alphaBetaMax(DraughtsNode node, int alpha, int beta, int depth, boolean firstTime)
            throws AIStoppedException {
        return combinedAlphaBetaMax(node, alpha, beta, depth, true, firstTime);
    }

    int combinedAlphaBetaMax(DraughtsNode node, int alpha, int beta, int depth, boolean isMaximizing, boolean isFirstRun)
            throws AIStoppedException {
        if (stopped) {
            stopped = false;
            throw new AIStoppedException();
        }
        DraughtsState state = node.getState();
        if (state.isEndState()) {
            return evaluate(state);
        }
        //quiescence by continuing if last ply's move is a capture
        if (depth <= 0 && !state.getMoves().get(0).isCapture()) {
            return evaluate(state);
        }

        // move ordering
        List<Move> movesToCheck;
        if (isFirstRun && node.getBestMove() != null) {

            //System.out.println("Before sort: " + state.getMoves());
            movesToCheck = new ArrayList<>();
            movesToCheck.add(node.getBestMove());
            List<Move> moves = state.getMoves();
            Collections.shuffle(moves);
            for (Move m : moves) {
                if (!m.equals(node.getBestMove())) {
                    movesToCheck.add(m);
                }
            }
            //System.out.println("After sort: " + movesToCheck);
        } else {
            movesToCheck = state.getMoves();
            Collections.shuffle(movesToCheck);
        }
        // end of move ordering
        if (isMaximizing) {
            int bestValue = -Integer.MAX_VALUE / 2;
            for (Move m : movesToCheck) {
                state.doMove(m);
                // root: not sure if just can use node
                DraughtsNode mNode = new DraughtsNode(state);
                int mValue = alphaBetaMin(mNode, alpha, beta, depth - 1, false);
                state.undoMove(m);
                if (mValue > bestValue) {
                    /**
                     * debugging info start *
                     */
                    Map<Integer, Move> bests = mNode.getBestMoves();
                    node.resetBestMoves();
                    for (Integer key : bests.keySet()) {
                        node.setBestMoveDepth(bests.get(key), key);
                    }
                    node.setBestMoveDepth(m, depth);
                    /**
                     * debugging info end *
                     */
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
            for (Move m : movesToCheck) {
                state.doMove(m);
                DraughtsNode mNode = new DraughtsNode(state);
                int mValue = alphaBetaMax(mNode, alpha, beta, depth - 1, false);
                state.undoMove(m);
                if (mValue < bestValue) {
                    /**
                     * debugging info start *
                     */
                    Map<Integer, Move> bests = mNode.getBestMoves();
                    node.resetBestMoves();
                    for (Integer key : bests.keySet()) {
                        node.setBestMoveDepth(bests.get(key), key);
                    }
                    node.setBestMoveDepth(m, depth);
                    /**
                     * debugging info end *
                     */
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
    int evaluate(DraughtsState state
    ) {
        int[] pieces = state.getPieces(); //obtain pieces array
        int[] tileCounts = new int[5]; //for each i in this array, it contains the number
        //of tiles that have the enum value i (e.g. int[0] indicates number of empty fields)

        for (int i = 1; i <= 50; i++) {
            int piece = pieces[i];
            tileCounts[piece]++;
        }
        int totalPieces = tileCounts[1] + tileCounts[2] + tileCounts[3] + tileCounts[4]; //total number of pieces remaining
        int whiteScore = 0;
        int blackScore = 0;
        for (int i = 1; i <= 50; i++) {
            int piece = pieces[i];
            if (piece == 1) {
                whiteScore += positionalEvaluation(state, piece, i, totalPieces);
            } else if (piece == 2) {
                blackScore += positionalEvaluation(state, piece, i, totalPieces);
            }
        }

        whiteScore += PIECE * tileCounts[1] + KING * tileCounts[3];
        blackScore += PIECE * tileCounts[2] + KING * tileCounts[4];
        int difference = whiteScore - blackScore; //we get the difference between the two,
        //so that we maximize whiteCount and minimize blackCount in order to get a higher value.

        //TODO: add "whiteCount +" to return statement, which currently does not 
        //seem to be an improvement probably due to implicit assumptions on 1 for 1 exchanges.
        return difference;
    }

    /**
     * A method that evaluates the position of a piece and gives it a value
     *
     * @pre 1 <= pieceType <= 4
     */
    int positionalEvaluation(DraughtsState state, int pieceType, int pieceNumber, int totalPieces
    ) {
        if (pieceType == 2 || pieceType == 4) {
            return 0; //for now no extra yet for kings, they already have a higher value
        }
        int posEval = 0;

        int centerMinus = 0; //minus points for having non-center pieces
        if (totalPieces < LATEMIDGAME) {
            if (pieceNumber % 10 == 6 || pieceNumber % 10 == 5) {
                centerMinus += SIDEPIECE;
            }

        }
//        posEval += centerMinus;

        int rowNr = (int) Math.ceil((double) pieceNumber / NRCOLUMNS); //correct if piece is black, 
        //but if piece is white this needs to be reversed
        if (pieceType == 1) {
            rowNr = NRROWS + 1 - rowNr;
        }
        int tempoScore = rowNr * ROWMULTIPLIER;
        posEval += tempoScore;

        int defenseBonus = 0;
        // give defense bonus if leaving pieces to defend backrow, except for corner pieces
        if (rowNr == 1) {
            if (pieceNumber == 46 || pieceNumber == 5 || pieceNumber == 1 || pieceNumber == 50) {
                defenseBonus += CORNERBACKRANK;
            } else {
                defenseBonus += MIDDLEBACKRANK;
            }
        }
        posEval += defenseBonus;

        int doubleCornerBonus = 0;
        if (pieceNumber == 45 || pieceNumber == 50 || pieceNumber == 1 || pieceNumber == 6) {
            doubleCornerBonus += DOUBLECORNER;
        }
        posEval += doubleCornerBonus;

        //TODO: balance method by taking difference of pieces on left side and right side
        return posEval;
    }

    /**
     * TODO: a method for calculating runaway pieces (see
     * https://github.com/olsson/checkers/blob/master/eval.c)
     */
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
            node.setBestValue(value);
        }
        return value;
    }
}
