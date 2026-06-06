import java.util.*;

public class Computer 
{
    private static final int MAX_LONG_STATE_CELLS = 39;

    private Map<Object, float[]> rewardsTable;

    private Game game;
    private double learningRate;
    private double discountFactor;
    private double explorationRate;
    private boolean useLongStateKeys;

    public Computer(Game g, double l, double d, double e)
    {
        rewardsTable = new HashMap<>();

        game = g;
        learningRate = l;
        discountFactor = d;
        explorationRate = e;
        useLongStateKeys = game.getBoardSize() <= MAX_LONG_STATE_CELLS;
    }


    public double getDiscountFactor() { return discountFactor; }
    public double getExplorationRate() { return explorationRate; }
    public double getLearningRate() { return learningRate; }

    public void setExplorationRate(double e) { explorationRate = e; }

    private Object getCompactState(Boolean[][] board) // A replacement for the deepToString method used earlier - compresses the board state into something more memory efficient (this prevents outofmemory errors when training it millions of times)
    {
        if (useLongStateKeys) { return getLongState(board); }
        return getStringState(board);
    }

    private long getLongState(Boolean[][] board)
    {
        long state = 0;
        long placeValue = 1;

        for (Boolean[] row : board) 
        {
            for (Boolean val : row) 
            {
                if (val != null)
                {
                    state += (val ? 1L : 2L) * placeValue;
                }
                placeValue *= 3L;
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

    public float[] getRewards(Object boardState)
    {
        return rewardsTable.computeIfAbsent(boardState, key -> new float[game.getBoardSize()]);
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

    private int chooseMoveIndex(Object currentState)
    {
        if (game.isBoardFull()) { throw new IllegalStateException("No available moves."); }

        if (Math.random() < explorationRate) // If true, then make a random move (exploration)
        {
            return chooseRandomMoveIndex();
        }
        else // If not, then do what worked before (exploitation)
        {
            float[] currentRewards = getRewards(currentState);
            int bestMove = -1;   
            double bestReward = Double.NEGATIVE_INFINITY;    

            for (int r = 0; r < game.getNumRows(); r++)
            {
                for (int c = 0; c < game.getNumCols(); c++)
                {
                    if (!game.isSpotOpen(r, c)) { continue; }

                    int move = game.getMoveIndex(r, c);
                    double currentReward = currentRewards[move];
                    if (currentReward > bestReward)
                    {
                        bestReward = currentReward;
                        bestMove = move;
                    }
                }
            }

            return bestMove;
        }
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
    public void updateRewardsTable(Object oldState, int action, double reward, Object newState)
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

    public void trainModel(long numSimulations)
    {
        for (long i = 0; i < numSimulations; i++)
        {
            game.resetBoard();
            Object lastState = null;
            int lastMove = -1;

            while (true) 
            { 
                if (game.getCurrentPlayer()) // if it's the "player's" turn, then make a random move
                {
                    int randomMove = chooseRandomMoveIndex();
                    game.makeMove(game.getRowFromIndex(randomMove), game.getColFromIndex(randomMove));

                    if (game.checkWin())
                    {
                        if (lastState != null) // if the random player won, then "punish" the model (hence the reward of -1.0)
                        {
                            updateRewardsTable(lastState, lastMove, -1.0, null); 
                        }
                        break;
                    }
                }
                else // if it's the computer's turn, then put your thinking cap on
                {
                    Object currentState = getCompactState(game.getBoard());
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
                        break;
                    }
                }

                if (game.isBoardFull())
                {
                        if (lastState != null) // if the random player won, then "punish" the model (hence the reward of -1.0)
                        {
                            updateRewardsTable(lastState, lastMove, 0.5, null);
                        }
                        break;
                }
                }
            }

        System.out.println("Training complete.");
        setExplorationRate(0.0); // IT'S GO TIME, BOIS!!!!!! (no more randomness b.c. we assume perfect rewards)
    }






    
}
