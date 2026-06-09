import java.util.*;

public class Game 
{
    // IVs are final b.c. VS Code yelled at me
    private final Boolean[][] board;
    private boolean isPlayerGoing;
    private final int numRows;
    private final int numCols;
    private final int numToWin;
    
    public Game(int rows, int cols, int toWin)
    {
        numRows = rows;
        numCols = cols;
        numToWin = toWin;

        board = new Boolean[rows][cols];
        resetBoard();
    }

    public Game() 
    {
        numRows = 3;
        numCols = 3;
        numToWin = 3;

        board = new Boolean[3][3];
        resetBoard();
    }

    // Getters for the various IVs

    public Boolean[][] getBoard() { return board; }
    public boolean getCurrentPlayer() { return isPlayerGoing; }
    public int getNumRows() { return numRows; }
    public int getNumCols() { return numCols; }
    public int getWinCondition() { return numToWin; }
    public int getBoardSize() { return numRows * numCols; }

    public int getMoveIndex(int row, int col)
    {
        return row * numCols + col;
    }

    public int getRowFromIndex(int index)
    {
        return index / numCols;
    }

    public int getColFromIndex(int index)
    {
        return index % numCols;
    }

    public boolean isSpotOpen(int row, int col)
    {
        return row >= 0 && row < numRows && col >= 0 && col < numCols && board[row][col] == null;
    }

    public int getMoveCount()
    {
        return getBoardSize() - getAvailableMoves().size();
    }

    public void resetBoard()
    {
        for (int i = 0; i < numRows; i++)
        {
            for (int j = 0; j < numCols; j++)
            {
                board[i][j] = null;
            }
        }

        isPlayerGoing = true;
    }

    public boolean makeMove(int row, int col)
    {
        if (isSpotOpen(row, col))
        {
            board[row][col] = isPlayerGoing;
            isPlayerGoing = !isPlayerGoing;
            return true;
        }
        return false; // indicates that the spot has been taken
    }

    /**
     * checkDirection checks whether there is a win in a specific direction for the player that just went.
     * @param row is the row of the piece that was just placed
     * @param col is the column of the piece that was just placed
     * @param direction is the direction that is to be checked 
     * (0 is horizontal, 1 is vertical, 2 is the leftward diagonal, 3 is the rightward diagonal)
     * @return value is whether or not the player that just went won (true if yes, false if no)
     */

    public boolean checkDirection(Boolean[][] board, int row, int col, int direction, int win)
    {
        Boolean target = board[row][col];
        if (target == null) return false;

        int[][] directions = {
            {0, 1},  // horizontal
            {1, 0},  // vertical
            {1, 1},  // diagonal down-right
            {1, -1}  // diagonal down-left
        };

        int dRow = directions[direction][0];
        int dCol = directions[direction][1];

        int count = 1;

        int r = row + dRow;
        int c = col + dCol;
        while (r >= 0 && r < numRows && c >= 0 && c < numCols && Objects.equals(board[r][c], target))
        {
            count++;
            r += dRow;
            c += dCol;
        }

        r = row - dRow;
        c = col - dCol;
        while (r >= 0 && r < numRows && c >= 0 && c < numCols && Objects.equals(board[r][c], target))
        {
            count++;
            r -= dRow;
            c -= dCol;
        }

        return count >= win;
    }

    /**
     * checkWin uses checkDirection to see if either the player or the computer won.
     * @return value is true if a win was detected; false if otherwise
     */

    public boolean checkWin()
    {
        for (int r = 0; r < numRows; r++)
        {
            for (int c = 0; c < numCols; c++)
            {
                if (board[r][c] == null) { continue; }
                for (int i = 0; i < 4; i++)
                {
                    if (checkDirection(board, r, c, i, numToWin)) { return true; }
                }
            }
        }
        return false;
    }

    public boolean isBoardFull()
    {
        for (Boolean[] row : board)
        {
            for (Boolean square : row)
            {
                if (square == null) { return false; }
            }
        }
        
        return true; 
    }

    /**
     * getAvailableMoves spits out all squares that are null (i.e. available)
     * @return value is an ArrayList of Integer[] coordinates, where moves[0] is the row, and moves[1] is the column.
     */

    public ArrayList<int[]> getAvailableMoves()
    {
        ArrayList<int[]> moves = new ArrayList<>();

        for (int r = 0; r < numRows; r++)
        {
            for (int c = 0; c < numCols; c++)
            {
                if (board[r][c] == null) { moves.add(new int[]{ r, c }); }
            }
        }

        return moves;
    }

    public void printBoard()
    {
        char maxFile = (char) (numCols + 96); 
        int maxRank = numRows;

        int r = numRows;
        for (Boolean[] row : board)
        {
            if (r > 9)
            {
                System.out.print(r + " ");
            }
            else
            {
                System.out.print(r + "  ");
            }
            for (Boolean space : row)
            {
                if (space == null)
                {
                    System.out.print(Colors.ANSI_BLACK + "_ " + Colors.ANSI_RESET);
                }
                else if (space)
                {
                    System.out.print(Colors.ANSI_RED + "X " + Colors.ANSI_RESET);
                }
                else
                {
                    System.out.print(Colors.ANSI_BLUE + "O " + Colors.ANSI_RESET);
                }
            }

            System.out.println();
            r--;
        }

        System.out.print("   "); // part of the coordinate printer
        for (char c = 'a'; c <= maxFile; c++)
        {
            System.out.print(c + " ");
        }
        System.out.println();
    }
}
