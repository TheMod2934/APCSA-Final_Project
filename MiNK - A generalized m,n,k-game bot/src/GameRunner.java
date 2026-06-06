import java.util.*;

public class GameRunner 
{
    public static void main(String[] args)
    {
        Scanner input = new Scanner(System.in);

        System.out.println("What is the length of the board?");
        int numCols = input.nextInt();

        System.out.println("What is the width of the board?");
        int numRows = input.nextInt();

        System.out.println("How many Xes or Oes are needed to win?");
        int winCondition = input.nextInt();

        System.out.println("How many times do you want the model to train? (for more spaces on the board, it is recommended to have more training sessions in order for intelligent play)");
        long sims = input.nextLong();
        input.nextLine();

        Game game = new Game(numRows, numCols, winCondition);
        Computer hal9000 = new Computer(game, 0.2, 0.9, 0.3);
        hal9000.trainModel(sims);

        while (true) 
        {
            game.resetBoard();
            System.out.print("\n--- New Game ---\n");

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
                        System.out.println("The computer beat you, m8.");
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
