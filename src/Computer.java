import java.util.*;

public class Computer 
{
    private final Map<String, float[]> rewardsTable;

    private final Game game;
    private double learningRate;
    private final double discountFactor;
    private double explorationRate;
    private double heuristicRate;

    public Computer(Game g, double l, double d, double e, double h)
    {
        rewardsTable = new HashMap<>();

        game = g;
        learningRate = l; // Learning Rate: how fast the model adjusts its rewards (constant used in the updator)
        discountFactor = d; // Discount Factor: how much the model values a future reward (constant used in the updator)
        explorationRate = e; // Exploration Rate: the big one, or the probabilty (as defined with Math.random()) that the model takes an exploitatory/exploratory path
        heuristicRate = h; // Heuristic Rate: the the probabilty (as defined with Math.random()) that the model uses a heuristic
    }

    // Some getters (and a setter, my god)
    public double getDiscountFactor() { return discountFactor; }
    public double getExplorationRate() { return explorationRate; }
    public double getLearningRate() { return learningRate; }
    public void setLearningRate(double l) { learningRate = l; }
    public void setExplorationRate(double e) { explorationRate = e; }
    public void setHeuristicRate(double h) { heuristicRate = h; }

    public String getBoardState(Boolean[][] board, boolean currentPlayer)
    {   
        // compact string encoding: '0' = empty, '1' = true, '2' = false
        String s = new String();

        // first, tack on who is playing
        if (currentPlayer)
        {
            s += '0';
        }
        else
        {
            s += '1';
        }

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

    // The center heuristic - optimize choosing moves near the center
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

    // Simulate the next move (hense the row/col, and the currentPlayer), and return true if it wins (or would win the if not blocked pretty soon)
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

    public int chooseImmediateMoveIndex()
    {
        Boolean[][] board = game.getBoard();

        // ATTACK!!!!!!
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

        // Setup attacks
        if (game.getWinCondition() > 3)
        {
            for (int r = 0; r < game.getNumRows(); r++)
            {
                for (int c = 0; c < game.getNumCols(); c++)
                {
                    if (!game.isSpotOpen(r, c)) { continue; }
                    
                    board[r][c] = true;
                    for (int i = 0; i < 4; i++)
                    {
                        if (game.checkDirection(board, r, c, i, game.getWinCondition() - 1))
                        {
                            board[r][c] = null;
                            return game.getMoveIndex(r, c);
                        }
                    }
                    board[r][c] = null;
                }
            }
        }

        // Make blocks
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

        // Make sure the opponent isn't setting anything up
        if (game.getWinCondition() > 3)
        {
            for (int r = 0; r < game.getNumRows(); r++)
            {
                for (int c = 0; c < game.getNumCols(); c++)
                {
                    if (!game.isSpotOpen(r, c)) { continue; }
                    
                    board[r][c] = true;
                    for (int i = 0; i < 4; i++)
                    {
                        if (game.checkDirection(board, r, c, i, game.getWinCondition() - 1))
                        {
                            board[r][c] = null;
                            return game.getMoveIndex(r, c);
                        }
                    }
                    board[r][c] = null;
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
        return chooseMoveIndex(getBoardState(game.getBoard(), game.getCurrentPlayer()));
    }

    public int chooseMoveIndex(String currentState)
    {
        if (game.isBoardFull()) { throw new IllegalStateException("No available moves."); }

        if (Math.random() < explorationRate) // If true, then make a random move (exploration)
        {
            return chooseRandomMoveIndex();
        }
        else 
        {
            if (Math.random() < heuristicRate) // If not, then first try the heuristics if possible...
            {
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
            }
            // then do what worked in the past (exploitation)

            float[] currentRewards = getRewards(currentState);
            int bestMove = -1;   
            double bestScore = (-1) * Double.MAX_VALUE;

            double centerWeight = 0.5; // tuning parameter for center proximity bonus
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

                    // prioritize moves near the center if possible - gives the bot a little more strategy
                    double centerBonus;
                    if (maxDist == 0)
                    {
                        centerBonus = 0.0;
                    }
                    else // update the center bonus to figure out whether or not the center is where its at
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
    }

    public int chooseRandomMoveIndex()
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
        setLearningRate(learningRate + 0.7); // make the bot more eager to change rewarsd (just for training purposes)

        for (long i = 0; i < numSimulations; i++)
        {
            game.resetBoard();
            String lastPlayerState = null;
            int lastPlayerMove = -1;
            String lastBotState = null;
            int lastBotMove = -1;

            while (true) // Training against the bot itself - any time it wins, it updates its weights based on what it learned
            { 
                if (game.getCurrentPlayer())
                {
                    String playerState = getBoardState(game.getBoard(), game.getCurrentPlayer());
                    if (lastPlayerState != null && lastPlayerMove != -1) 
                    {
                        updateRewardsTable(lastPlayerState, lastPlayerMove, 0.0, playerState); // keep weights the same if a neutral move has been made, where the move just made is the new state
                    }

                    int move = chooseMoveIndex(playerState);

                    lastPlayerState = playerState;
                    lastPlayerMove = move;

                    if (lastPlayerMove == -1) { break; }

                    game.makeMove(game.getRowFromIndex(lastPlayerMove), game.getColFromIndex(lastPlayerMove));

                    if(game.checkWin())
                    {
                        updateRewardsTable(lastPlayerState, lastPlayerMove, 1.0, null); // if the "player" computer wins, then reward it (no new state as the game is over)
                        
                        if (lastBotState != null && lastBotMove != -1)
                        {
                            updateRewardsTable(lastBotState, lastBotMove, -1.0, null); // and "punish" the "bot" computer for losing (no new state as the game is over)
                        }
                        winRate += 1;
                        break;
                    }
                }
                else
                {
                    String botState = getBoardState(game.getBoard(), game.getCurrentPlayer());
                    if (lastBotState != null && lastBotMove != -1) 
                    {
                        updateRewardsTable(lastBotState, lastBotMove, 0.0, botState);
                    }

                    int move = chooseMoveIndex(botState);

                    lastBotState = botState;
                    lastBotMove = move;

                    if (lastBotMove == -1) { break; }

                    game.makeMove(game.getRowFromIndex(lastBotMove), game.getColFromIndex(lastBotMove));

                    if(game.checkWin())
                    {
                        updateRewardsTable(lastBotState, lastBotMove, 1.0, null); // if the "bot" computer wins, then reward it (no new state as the game is over)
                        // Penalize the player for losing
                        if (lastPlayerState != null && lastPlayerMove != -1)
                        {
                            updateRewardsTable(lastPlayerState, lastPlayerMove, -1.0, null); // and "punish" the "player" computer for losing (no new state as the game is over)
                        }
                        winRate += 1;
                        break;
                    }
                }

                if (game.isBoardFull()) // ties should be handled with somewhat positive rewards (hence the 0.5 reward)
                {
                        if (lastPlayerState != null && lastPlayerMove != -1)
                        {
                            updateRewardsTable(lastPlayerState, lastPlayerMove, 0.5, null);
                        }
                        if (lastBotState != null && lastBotMove != -1)
                        {
                            updateRewardsTable(lastBotState, lastBotMove, 0.5, null);
                        }
                        winRate += 0.5;
                        break;
                }
            }
        }
        
        setLearningRate(learningRate - 0.7); // restore the learning rate to what it was
        setExplorationRate(0.05); // IT'S GO TIME, BOIS!!!!!! (much less randomness b.c. we assume perfect rewards)
        return winRate;
    }






    
}
