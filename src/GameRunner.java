import java.util.*;

public class GameRunner 
{
    public static void main(String[] args) throws InterruptedException
    {
        Scanner input = new Scanner(System.in);

        int numRows = Integer.MAX_VALUE;
        int numCols = Integer.MAX_VALUE;
        int winCondition = Integer.MAX_VALUE;

        while (true)
        {
            System.out.println("What is the length of the board?");
            numCols = input.nextInt();

            if (numCols <= 26)
            {
                break;
            }
            else
            {
                System.out.println("Invalid length. Please input a length shorter than 26.");
            }
        }

        while (true) 
        { 
            System.out.println("What is the width of the board?");
            numRows = input.nextInt();

            if (numRows <= 26)
            {
                break;
            }
            else
            {
                System.out.println("Invalid length. Please input a width shorter than 26.");
            }
        }

        while (true) 
        {
            System.out.println("How many Xes or Oes are needed to win?");
            winCondition = input.nextInt();

            if (winCondition <= Math.min(numRows, numCols))
            {
                break;
            }
            else
            {
                System.out.println("Invalid win condition. Please input a win condition that is smaller than the shorter of the two dimensions");
            }
        }

        input.nextLine();

        Game game = new Game(numRows, numCols, winCondition);
        Computer hal9000 = new Computer(game, 0.2, 0.9, 0.5, 0.5);

        String[] spinner = {"/", "-", "\\", "|"};
        System.out.print("Training in progress: ");

        final boolean[] spinnerRunning = { true };
        Thread spinnerThread = new Thread(() -> {
            int i = 0;
            while (spinnerRunning[0]) {
                System.out.print(spinner[i % spinner.length] + "\rTraining in progress: ");
                i++;
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        spinnerThread.start();

        // dynamic win counting - if the wins are more than the win rate, then stop the training
        double totalWinScore = 0.0;
        long totalSimulations = 0;
        double averageWinRate = 0.0;

        while (averageWinRate <= 1.0)
        {
            double batchWinRate = hal9000.trainModel(10000);
            totalSimulations += 1000;
            totalWinScore += batchWinRate * 1000;
            averageWinRate = totalWinScore / totalSimulations;
        }

        spinnerRunning[0] = false;
        spinnerThread.join();
        System.out.println("\rTraining complete.          ");

        while (true) 
        {
            game.resetBoard();
            System.out.print("\n---- New Game ----\n");

            while (true) 
            {
                game.printBoard();

                if (game.getCurrentPlayer())
                {
                    int[] move = null;
                    while (move == null) { move = takeInput(game, input); }

                    if (!game.makeMove(move[0], move[1]))
                    {
                        System.out.println("Spot taken!");
                        continue;
                    }

                    if (game.checkWin()) 
                    {
                        game.printBoard();
                        System.out.println("You're Winner!!!!!!!!!!!!!");
                        break;
                    }
                }
                else
                {
                    System.out.println("AI is having a beeg think");
                    int halMove = hal9000.chooseMoveIndex();
                    game.makeMove(game.getRowFromIndex(halMove), game.getColFromIndex(halMove));

                    if (game.checkWin()) 
                    {
                        game.printBoard();
                        System.out.println("The computer beat you, m8. Soz.");
                        break;
                    }
                }

                if (game.isBoardFull()) 
                {
                    game.printBoard();
                    System.out.println("Tied Game!");
                    break;
                }
            }

            break;
        }

        input.close();

    }

    public static int[] takeInput(Game game, Scanner input)
    {
        char maxFile = (char) (game.getNumCols() + 96); 
        int maxRank = game.getNumRows();
        
        System.out.print("Enter move (a1-" + maxFile + maxRank + "): ");
        String move = input.nextLine().trim().toLowerCase();

        // Return null if the input is not valid
        if (move.length() < 2)
        {
            System.out.println("Invalid input. Please try again.");
            return null;
        }

        char file = move.charAt(0);
        int rank;

        try 
        {
            rank = Integer.parseInt(move.substring(1));
        }
        catch (NumberFormatException e)
        {
            System.out.println("Invalid input. Please try again.");
            return null;
        }

        if (file < 'a' || maxFile < file || rank < 1 || maxRank < rank)
        {
            System.out.println("Invalid input. Please try again.");
            return null;
        }
        
        // Convert the input to row and column numbers
        int row = game.getNumRows() - rank;
        int col = file - 'a';

        return new int[] {row, col};
    }
}
