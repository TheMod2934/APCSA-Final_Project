import java.util.*;

public class Computer 
{
    private final Map<String, float[]> rewardsTable;

    private final Game game;
    private final double learningRate;
    private final double discountFactor;
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
    private String getCompactState(Boolean[][] board) { return getBoardState(board); }
    public void setExplorationRate(double e) { explorationRate = e; }

    private String getBoardState(Boolean[][] board)
    {
        // compact string encoding: '0' = empty, '1' = true, '2' = false
        String s = new String();

        for (Boolean[] row : board)
        {
            for (Boolean val : row)
            {
                if (val == null)
                {
                    s += '0';
                }
                else if (val)
                {
                    s += '1';
                }
                else
                {
                    s += '2';
                }
            }
        }

        return s;
    }

    public float[] getRewards(String boardState)
    {
        // Ensure a rewards array exists for this board state, then return it.
        if (!rewardsTable.containsKey(boardState))
        {
            rewardsTable.put(boardState, new float[game.getBoardSize()]); // if not, then put something there
        }
        return rewardsTable.get(boardState);
    }

    // Simulate the next move (hense the row/col, and the currentPlayer), and return true if it wins
    public boolean isWinningMove(Boolean[][] board, int row, int col, boolean currentPlayer)
    {
        board[row][col] = currentPlayer;
        boolean win = false;
        for (int i = 0; i < 4; i++)
        {
            if (game.checkDirection(board, row, col, i, game.getWinCondition()))
            {
                win = true;
            }
        }
        board[row][col] = null;
        return win;
    }

    
    public int chooseCenterMoveIndex()
    {
        if (game.getNumRows() % 2 != 1 || game.getNumCols() % 2 != 1) { return -1; }

        int centerRow = game.getNumRows() / 2;
        int centerCol = game.getNumCols() / 2;

        if (game.isSpotOpen(centerRow, centerCol))
        {
            return game.getMoveIndex(centerRow, centerCol);
        }
        else
        {
            return -1;
        }
    }

    public int chooseImmediateMoveIndex()
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

    private int chooseMoveIndex(String currentState)
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
        double bestScore = (-1) * Double.MAX_VALUE;

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
                double centerBonus;
                if (maxDist == 0)
                {
                    centerBonus = 0.0;
                }
                else
                {
                    centerBonus = ((maxDist - dist) / maxDist) * centerWeight;
                }

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
    public void updateRewardsTable(String oldState, int action, double reward, String newState)
    {
        float[] oldRewards = getRewards(oldState);
        double oldReward = oldRewards[action];

        double maxFutureReward = 0.0;
        if (newState != null) // not a terminal state
        {
            float[] newRewards = getRewards(newState);
            maxFutureReward = (-1) * Double.MAX_VALUE;

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

            if (maxFutureReward == (-1) * Double.MAX_VALUE) { maxFutureReward = 0.0; }
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
            String lastState = null;
            int lastMove = -1;

            while (true) 
            { 
                if (game.getCurrentPlayer()) // if it's the "player's" turn, then make a random move, and use the heuristics if neccesary
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
                    String currentState = getCompactState(game.getBoard());
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

        setExplorationRate(0.05); // IT'S GO TIME, BOIS!!!!!! (much less randomness b.c. we assume perfect rewards)
        return winRate;
    }






    
}
