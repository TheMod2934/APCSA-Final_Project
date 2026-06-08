import java.math.BigInteger; // needed to overcome the outofmemory errors (basically, im just gonna brute force it)
import java.util.*;

public class Computer 
{
    private Map<BigInteger, float[]> rewardsTable;

    private Game game;
    private double learningRate;
    private double discountFactor;
    private double explorationRate;

    public Computer(Game g, double l, double d, double e)
    {
        rewardsTable = new HashMap<>();

        game = g;
        learningRate = l;
        discountFactor = d;
        explorationRate = e;
    }


    public double getDiscountFactor() { return discountFactor; }
    public double getExplorationRate() { return explorationRate; }
    public double getLearningRate() { return learningRate; }

    public void setExplorationRate(double e) { explorationRate = e; }

    private BigInteger getCompactState(Boolean[][] board) // A replacement for the deepToString method used earlier - compresses the board state into a single integer key so the model can support larger boards without long overflow
    {
        return getBigIntegerState(board);
    }

    private BigInteger getBigIntegerState(Boolean[][] board)
    {
        BigInteger state = BigInteger.ZERO;
        BigInteger base = BigInteger.valueOf(3);

        for (Boolean[] row : board)
        {
            for (Boolean val : row)
            {
                state = state.multiply(base);
                if (val != null)
                {
                    state = state.add(val ? BigInteger.ONE : BigInteger.valueOf(2));
                }
            }
        }

        return state;
    }

    private String getStringState(Boolean[][] board)
    {
        StringBuilder sb = new StringBuilder(game.getNumRows() * game.getNumCols());
        for (Boolean[] row : board) 
        {
            for (Boolean val : row) 
            {
                if (val == null) { sb.append('-'); }
                else if (val) { sb.append('X'); }
                else { sb.append('O'); }
            }
        }
        return sb.toString();
    }

    public float[] getRewards(BigInteger boardState)
    {
        return rewardsTable.computeIfAbsent(boardState, key -> new float[game.getBoardSize()]);
    }

    private boolean isWinningMove(Boolean[][] board, int row, int col, boolean playerMark)
    {
        board[row][col] = playerMark;
        boolean win = isWinningPosition(board, row, col, playerMark);
        board[row][col] = null;
        return win;
    }

    private boolean isWinningPosition(Boolean[][] board, int row, int col, boolean playerMark)
    {
        return countInDirection(board, row, col, 0, 1, playerMark) >= game.getWinCondition()
            || countInDirection(board, row, col, 1, 0, playerMark) >= game.getWinCondition()
            || countInDirection(board, row, col, 1, 1, playerMark) >= game.getWinCondition()
            || countInDirection(board, row, col, 1, -1, playerMark) >= game.getWinCondition();
    }

    private int countInDirection(Boolean[][] board, int row, int col, int dRow, int dCol, boolean playerMark)
    {
        return 1
            + countConsecutive(board, row, col, dRow, dCol, playerMark)
            + countConsecutive(board, row, col, -dRow, -dCol, playerMark);
    }

    private int countConsecutive(Boolean[][] board, int row, int col, int dRow, int dCol, boolean playerMark)
    {
        int count = 0;
        int r = row + dRow;
        int c = col + dCol;

        while (r >= 0 && r < game.getNumRows() && c >= 0 && c < game.getNumCols())
        {
            if (board[r][c] == null || board[r][c] != playerMark)
            {
                break;
            }
            count++;
            r += dRow;
            c += dCol;
        }

        return count;
    }
    
    private int chooseCenterMoveIndex()
    {
        if (game.getNumRows() % 2 != 1 || game.getNumCols() % 2 != 1) { return -1; }

        int centerRow = game.getNumRows() / 2;
        int centerCol = game.getNumCols() / 2;

        return game.isSpotOpen(centerRow, centerCol) ? game.getMoveIndex(centerRow, centerCol) : -1;
    }

    private int chooseImmediateMoveIndex()
    {
        Boolean[][] board = game.getBoard();

        for (int r = 0; r < game.getNumRows(); r++)
        {
            for (int c = 0; c < game.getNumCols(); c++)
            {
                if (!game.isSpotOpen(r, c)) { continue; }
                if (isWinningMove(board, r, c, false))
                {
                    return game.getMoveIndex(r, c);
                }
            }
        }

        for (int r = 0; r < game.getNumRows(); r++)
        {
            for (int c = 0; c < game.getNumCols(); c++)
            {
                if (!game.isSpotOpen(r, c)) { continue; }
                if (isWinningMove(board, r, c, true))
                {
                    return game.getMoveIndex(r, c);
                }
            }
        }

        return -1;
    }

    public int[] chooseMove()
    {
        int moveIndex = chooseMoveIndex();
        return new int[] { game.getRowFromIndex(moveIndex), game.getColFromIndex(moveIndex) };
    }

    public int chooseMoveIndex()
    {
        return chooseMoveIndex(getCompactState(game.getBoard()));
    }

    private int chooseMoveIndex(BigInteger currentState)
    {
        if (game.isBoardFull()) { throw new IllegalStateException("No available moves."); }

        if (Math.random() < explorationRate) // If true, then make a random move (exploration)
        {
            return chooseRandomMoveIndex();
        }

        int immediateMove = chooseImmediateMoveIndex();
        if (immediateMove != -1)
        {
            return immediateMove;
        }

        int centerMove = chooseCenterMoveIndex();
        if (centerMove != -1)
        {
            return centerMove;
        }

        float[] currentRewards = getRewards(currentState);
        int bestMove = -1;   
        double bestScore = Double.NEGATIVE_INFINITY;    

        double centerWeight = 0.5; // tuning parameter for proximity bonus
        int centerRow = game.getNumRows() / 2;
        int centerCol = game.getNumCols() / 2;
        double maxDist = centerRow + centerCol;

        for (int r = 0; r < game.getNumRows(); r++)
        {
            for (int c = 0; c < game.getNumCols(); c++)
            {
                if (!game.isSpotOpen(r, c)) { continue; }

                int move = game.getMoveIndex(r, c);
                double reward = currentRewards[move];

                double dist = Math.abs(r - centerRow) + Math.abs(c - centerCol);
                double centerBonus = (maxDist == 0) ? 0.0 : ((maxDist - dist) / maxDist) * centerWeight;

                double score = reward + centerBonus;
                if (score > bestScore)
                {
                    bestScore = score;
                    bestMove = move;
                }
            }
        }

        return bestMove;
    }

    private int chooseRandomMoveIndex()
    {
        int openSpots = game.getBoardSize() - game.getMoveCount();
        if (openSpots <= 0) { throw new IllegalStateException("No available moves."); }

        int targetOpenSpot = (int) (Math.random() * openSpots);
        int openSpot = 0;

        for (int r = 0; r < game.getNumRows(); r++)
        {
            for (int c = 0; c < game.getNumCols(); c++)
            {
                if (!game.isSpotOpen(r, c)) { continue; }

                if (openSpot == targetOpenSpot)
                {
                    return game.getMoveIndex(r, c);
                }

                openSpot++;
            }
        }

        throw new IllegalStateException("No available moves.");
    }

    // Really the biggest AI generated part of this - the updater for the rewards table
    public void updateRewardsTable(BigInteger oldState, int action, double reward, BigInteger newState)
    {
        float[] oldRewards = getRewards(oldState);
        double oldReward = oldRewards[action];

        double maxFutureReward = 0.0;
        if (newState != null) // not a terminal state
        {
            float[] newRewards = getRewards(newState);
            maxFutureReward = Double.NEGATIVE_INFINITY;

            for (int r = 0; r < game.getNumRows(); r++)
            {
                for (int c = 0; c < game.getNumCols(); c++)
                {
                    if (game.isSpotOpen(r, c))
                    {
                        maxFutureReward = Math.max(maxFutureReward, newRewards[game.getMoveIndex(r, c)]);
                    }
                }
            }

            if (maxFutureReward == Double.NEGATIVE_INFINITY) { maxFutureReward = 0.0; }
        }
        // Apply the Q-learning formula
        oldRewards[action] = (float) (oldReward + learningRate * (reward + discountFactor * maxFutureReward - oldReward));
    }

    public double trainModel(long numSimulations)
    {
        double winRate = 0.0;

        for (long i = 0; i < numSimulations; i++)
        {
            game.resetBoard();
            BigInteger lastState = null;
            int lastMove = -1;

            while (true) 
            { 
                if (game.getCurrentPlayer()) // if it's the "player's" turn, then make a random move
                {
                    int randomMove = chooseRandomMoveIndex();
                    int immediateMove = chooseImmediateMoveIndex();
                    int centerMove = chooseCenterMoveIndex();

                    if (immediateMove != -1)
                    {
                        game.makeMove(game.getRowFromIndex(immediateMove), game.getColFromIndex(immediateMove));
                    }
                    else if (centerMove != -1)
                    {
                        game.makeMove(game.getRowFromIndex(centerMove), game.getColFromIndex(centerMove));
                    }
                    else
                    {
                        game.makeMove(game.getRowFromIndex(randomMove), game.getColFromIndex(randomMove));
                    }
                    if (game.checkWin())
                    {
                        if (lastState != null) // if the random player won, then "punish" the model (hence the reward of -1.0)
                        {
                            updateRewardsTable(lastState, lastMove, -1.0, null); 
                        }
                        winRate += 0;
                        break;
                    }
                }
                else // if it's the computer's turn, then put your thinking cap on
                {
                    BigInteger currentState = getCompactState(game.getBoard());
                    if (lastState != null) 
                    {
                        updateRewardsTable(lastState, lastMove, 0.0, currentState);
                    }

                    int move = chooseMoveIndex(currentState);

                    lastState = currentState;
                    lastMove = move;

                    game.makeMove(game.getRowFromIndex(lastMove), game.getColFromIndex(lastMove));

                    if(game.checkWin()) // If the AI won, then reward the model (hence the reward of 1.0)
                    {
                        updateRewardsTable(lastState, lastMove, 1.0, null);
                        winRate += 1;
                        break;
                    }
                }

                if (game.isBoardFull())
                {
                        if (lastState != null) // if the random player won, then "punish" the model (hence the reward of -1.0)
                        {
                            updateRewardsTable(lastState, lastMove, 0.5, null);
                        }

                        winRate += 0.5;
                        break;
                }
            }
        }

        setExplorationRate(0.05); // IT'S GO TIME, BOIS!!!!!! (no more randomness b.c. we assume perfect rewards)
        return winRate;
    }






    
}
