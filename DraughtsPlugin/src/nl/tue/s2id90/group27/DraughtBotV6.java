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
 * This version has phases implemented, so there are ints that define the lower bounds
 * of total pieces for a certain phase (E.g. phase0 is 32 pieces or more). So now
 * all weights have a weight for each phase of the game. Furthermore: 
 * - left/right balance is implemented
 * - TEMPO advancement is tweaked a bit (with different weights for different phases)
 * - there is center control for certain pieces in the center (with associated weights)
 * - there is a golden piece, which is the middle back piece
 * - previous BACKRANK is disabled for now because it didn't work well
 * - development of back/rear is on TO-DO, not yet implemented
 * 
 * @author Luca Weibel and Michiel Verburg
 */
public class DraughtBotV6 extends DraughtsPlayer {

    private int bestValue = 0;
    int maxSearchDepth;

    final static int NRCOLUMNS = 5;
    final static int NRROWS = 10;
    
    //all below values have values for game phases 0, 1, 2 and 3 respectively
    final static int[] KING = {3000, 3000, 3000, 3000}; //value of king
    final static int PIECE = 1000; //value of normal piece
    final static int[] ROWMULTIPLIER = {20, 25, 30, 50}; //for taking tempi into account
//    final static int[] CORNERBACKRANK = {150, 150, 150, 150}; //value for corner in the back, less valuable as others
//    final static int[] MIDDLEBACKRANK = {250, 250, 250, 250}; //value for middle in the backrank
    final static int[] DOUBLECORNER = {75, 75, 75, 75}; //play from the double corner (e.g. field 45 and 50)
    final static int[] SIDEPIECE = {-100, -100, -100, -100}; //minus points for pieces on the left or right side because they can be blocked
    final static int[] CENTERCONTROL = {45 , 50, 55, 55}; //bonus points for center control
    final static int[] BALANCE = {-50, -50, -50, -20}; //minus points for imbalanced left wing vs right wing (left 3 columns vs right 3 columns)
    final static int[] GOLDENPIECE = {60, 60, 15, 40}; //bonus for piece in the back center
    final static int[] BACKDEVELOPMENT = {55, 50, 35, 5}; //bonus for development of backline, kind of defense

    final static int PHASE0 = 32; // >=32 pieces is phase 0
    final static int PHASE1 = 24; // >=24 
    final static int PHASE2 = 16; // >=16
    //PHASE3 is then with <= 15 pieces

    /**
     * boolean that indicates that the GUI asked the player to stop thinking.
     */
    private boolean stopped;

    public DraughtBotV6(int maxSearchDepth) {
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
    int evaluate(DraughtsState state) {
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
        
        int phase = game_phase(totalPieces); //get current phase of game based on total pieces

        whiteScore += PIECE * tileCounts[1] + KING[phase] * tileCounts[3];
        blackScore += PIECE * tileCounts[2] + KING[phase] * tileCounts[4];
        int difference = whiteScore - blackScore; //we get the difference between the two,
        //so that we maximize whiteCount and minimize blackCount in order to get a higher value.

        //add balance between left and right wing to the score
        difference += BALANCE[phase]*leftRightBalance(state);
        
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
        int phase = game_phase(totalPieces);
        if (pieceType == 2 || pieceType == 4) {
            return 0; //for now no extra yet for kings, they already have a higher value
        }
        int posEval = 0;

        //todo minus points for edge pieces , not very successful yet.
        int centerMinus = 0; //minus points for having non-center pieces
        if (pieceNumber % 10 == 6 || pieceNumber % 10 == 5) {
            centerMinus += SIDEPIECE[phase];
        }
//        posEval += centerMinus;

        //bonus for center control, for pieces 27, 28, 29, 32, 33, 34, 37, 38, 39 (for white's side, mirrored for black)
        int centerBonus = 0;
        if (pieceType == 1) {
            if (pieceNumber == 28) {
                centerBonus += 3*CENTERCONTROL[phase]; //this one is most important
            } else if (pieceNumber == 29 || pieceNumber == 32 || pieceNumber == 33) {
                centerBonus += 2*CENTERCONTROL[phase];
            } else if (pieceNumber == 27 || pieceNumber == 34 || pieceNumber == 37 || 
                    pieceNumber == 38 || pieceNumber == 39) {
                centerBonus += CENTERCONTROL[phase];
            }
        } else if (pieceType == 2) {
            if (pieceNumber == 23) { 
                centerBonus += 3*CENTERCONTROL[phase]; //this one is most important
            } else if (pieceNumber == 22 || pieceNumber == 18 || pieceNumber == 19) {
                centerBonus += 2*CENTERCONTROL[phase];
            } else if (pieceNumber == 24 || pieceNumber == 17 || pieceNumber == 12 || 
                    pieceNumber == 13 || pieceNumber == 14) {
                centerBonus += CENTERCONTROL[phase];
            }
        }
        posEval += centerBonus;

        int rowNr = (int) Math.ceil((double) pieceNumber / NRCOLUMNS); //correct if piece is black, 
        //but if piece is white this needs to be reversed
        if (pieceType == 1) {
            rowNr = NRROWS + 1 - rowNr;
        }
        int tempoScore = rowNr * ROWMULTIPLIER[phase];
        posEval += tempoScore;

        
        //golden piece bonus (center backrank)
        int golden = 0;
        if ((pieceType == 1 && pieceNumber == 48) || (pieceType == 2 && pieceNumber ==3)) {
            golden += GOLDENPIECE[phase];
        }
        posEval += golden;
        
//        //development of rear
//        int rearDev = 0;
//        if (pieceType == 1) {
//            
//        }
        
        //Disabled defense bonus as implemented below so far because could not find good
        //weight distribution over phases of the game.
        /**int defenseBonus = 0;
        // give defense bonus if leaving pieces to defend backrow, except for corner pieces
        if (rowNr == 1) {
            if (pieceNumber == 46 || pieceNumber == 5 || pieceNumber == 1 || pieceNumber == 50) {
                defenseBonus += CORNERBACKRANK[phase];
            } else {
                defenseBonus += MIDDLEBACKRANK[phase];
            }
        }
        posEval += defenseBonus;
        **/

        int doubleCornerBonus = 0;
        if (pieceNumber == 45 || pieceNumber == 50 || pieceNumber == 1 || pieceNumber == 6) {
            doubleCornerBonus += DOUBLECORNER[phase];
        }
        posEval += doubleCornerBonus;

        return posEval;
    }

    /**
     * A method that calculates the balance between left and right of board by
     * taking absolute difference between 2 wings (left 3 columns and right 3
     * columns) as a penalty
     * @return score difference between white's balance score and black's balance score
     */
    int leftRightBalance(DraughtsState state) {
        int wingScore = 0;
        int whiteBalance = 0;
        int blackBalance = 0;
        int[] pieces = state.getPieces();
        for (int i = 1; i <= 50; i++) {
            int piece = pieces[i];

            //using modulo, we can check if it is column 1, 2, 3 (in that order)
            if (i % 6 == 0 || i % 10 == 1 || i % 7 == 0 ) {
                if (piece == 1 || piece == 3) {
                    whiteBalance += 1;
                } else if (piece == 2 || piece == 4) {
                    blackBalance += 1;
                }
            }
            //now we check the right wing
            if ( i % 4 == 0 || i % 10 == 0 || i % 5 == 0) {
                 if (piece == 1 || piece == 3) {
                    whiteBalance -= 1;
                } else if (piece == 2 || piece == 4) {
                    blackBalance -= 1;
                }
            }
        }
        
        return Math.abs(whiteBalance) - Math.abs(blackBalance);
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
    
    int game_phase(int totalPieces) {
        if (totalPieces >= PHASE0) {
            return 0;
        } else if (totalPieces >= PHASE1) {
            return 1;
        } else if (totalPieces >= PHASE2) {
            return 2;
        } else {
            return 3;
        }
    }
}
