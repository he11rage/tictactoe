package Client

import cats.effect._
import cats.effect.std.Console
import cats.effect.unsafe.implicits.global
import cats.implicits.{catsSyntaxApplicativeId, toTraverseOps}
import io.grpc.Metadata
import tictactoe.tictactoe._

object Client extends IOApp.Simple {
  override def run: IO[Unit] = {
    TicTacToeClientImpl.resource().use { client =>
      playGame(client, client.getBoard().unsafeRunSync())
    }
  }

  def playGame(client: TicTacToeClientImpl, board: Boards): IO[Unit] = {
    for {
      _ <- IO.println("Player turn:")
      move <- getPlayerMove(board)
      newPlayerMoveBoard <- client.makePlayerMove(move, new Metadata())
      _ <- showBoard(newPlayerMoveBoard)
      gameOverPlayer <- client.checkGameOver(newPlayerMoveBoard)
      _ <- if (gameOverPlayer.gameOver) IO.println(s"Game over, winner: ${symbolToString(gameOverPlayer.winner)}") else if (isBoardFull(newPlayerMoveBoard).unsafeRunSync()) {
        IO.println("Game over, draw!")
      } else {
        for {
          _ <- IO.println("Computer turn:")
          newComputerMoveBoard <- client.makeComputerMove(newPlayerMoveBoard)
          _ <- showBoard(newComputerMoveBoard)
          gameOverComputer <- client.checkGameOver(newComputerMoveBoard)
          _ <- if (gameOverComputer.gameOver) IO.println(s"Game over, winner: ${symbolToString(gameOverComputer.winner)}") else if (isBoardFull(newComputerMoveBoard).unsafeRunSync()) {
            IO.println("Game over, draw!")
          } else playGame(client, newComputerMoveBoard)
        } yield ()
      }
    } yield ()
  }

  def getPlayerMove(board: Boards): IO[Move] = {
    for {
      _ <- IO.println("Enter line number(0-2):")
      row <- inputNumber(board)
      _ <- IO.println("Enter column number(0-2):")
      column <- inputNumber(board)
      move = Move(row, column, TicTacSymbols.X, Player.PLAYER, Option(board))
      free <- isFreeCell(board, move)
      fin <- if (free) IO.pure(move) else IO.println("This cell is already occupied!") >> getPlayerMove(board)
    } yield fin
  }

  def inputNumber(board: Boards): IO[Int] = {
    for {
      value <- Console[IO].readLine.flatMap { input =>
        val parsed = input.toInt
        if (parsed >= 0 && parsed <= 2) {
          IO.pure(parsed)
        } else {
          IO.println("The entered number exceeds the size of the board!") >> IO.raiseError(new IllegalArgumentException("Error!"))
        }
      }.handleErrorWith { error =>
        IO.println("Please try again!") >> inputNumber(board)
      }
    } yield value
  }


  def isFreeCell(board: Boards, move: Move): IO[Boolean] = {
    IO.pure(board.rows(move.row).symbols(move.column) == TicTacSymbols.Empty)
  }

  def isBoardFull(board: Boards): IO[Boolean] = {
    board.rows.forall(row => !row.symbols.contains(TicTacSymbols.Empty)).pure[IO]
  }

  private def symbolToString(symbol: TicTacSymbols): String = symbol match {
    case TicTacSymbols.Empty => "-"
    case TicTacSymbols.X => "X"
    case TicTacSymbols.O => "O"
  }

  private def showBoard(currentBoard: Boards): IO[Unit] = {
    for {
      _ <- IO.println("Current board:")
      _ <- currentBoard.rows.traverse { row =>
        row.symbols.traverse { symbol =>
          Console[IO].print(" ") >> Console[IO].print(symbolToString(symbol))
        } >> Console[IO].println("")
      }
    } yield ()
  }
}
