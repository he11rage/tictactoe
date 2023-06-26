package Computer

import cats.effect.unsafe.implicits.global
import io.grpc.Metadata
import cats.effect.IO
import cats.implicits.catsSyntaxApplicativeId
import tictactoe.tictactoe.{Boards, GameResult, Move, Player, TicTacSymbols, TicTacToeBoardFs2Grpc, TicTacToeComputerFs2Grpc}

import scala.util.Random

class TicTacToeComputerImpl(server: TicTacToeBoardFs2Grpc[IO, Metadata]) extends TicTacToeComputerFs2Grpc[IO, Metadata] {

  override def makeComputerMove(request: Boards, ctx: Metadata): IO[Boards] = {
    def getAvailableMoves(board: Boards): Seq[Move] = {
      board.rows.zipWithIndex.flatMap {
        case (row, rowIndex) =>
          row.symbols.zipWithIndex.flatMap {
            case (symbol, columnIndex) =>
              if (symbol == TicTacSymbols.Empty) {
                Some(Move(rowIndex, columnIndex, TicTacSymbols.O, Player.COMPUTER, Option(board)))
              }
              else {
                None
              }
          }
      }
    }

    def getAvailablePlayerMoves(board: Boards): Seq[Move] = {
      board.rows.zipWithIndex.flatMap {
        case (row, rowIndex) =>
          row.symbols.zipWithIndex.flatMap {
            case (symbol, columnIndex) =>
              if (symbol == TicTacSymbols.Empty) {
                Some(Move(rowIndex, columnIndex, TicTacSymbols.X, Player.PLAYER, Option(board)))
              }
              else {
                None
              }
          }
      }
    }

    def makeMove(move: Move): Boards = {
      val moveRow = move.row
      val moveColumn = move.column
      val moveBoard = move.board

      val updatedRows = moveBoard.get.rows.zipWithIndex.map {
        case (row, rowIndex) =>
          if (rowIndex == moveRow) {
            val updatedSymbols = row.symbols.zipWithIndex.map {
              case (symbol, columnIndex) =>
                if (columnIndex == moveColumn) {
                  if (move.symbol == TicTacSymbols.O) {
                    TicTacSymbols.O
                  }
                  else {
                    TicTacSymbols.X
                  }
                } else {
                  symbol
                }
            }
            row.copy(symbols = updatedSymbols)
          } else {
            row
          }
      }
      moveBoard.get.copy(rows = updatedRows)
    }


    def isWinningMove(move: Move): IO[Boolean] = {
      val newBoard = makeMove(move)

      server.checkGameOver(newBoard, new Metadata()).map(_.winner == TicTacSymbols.O)
    }

    def findWinningMove(moves: Seq[Move]) = {
      moves.find(move => isWinningMove(move).unsafeRunSync())
    }

    def isBlockingMove(move: Move): IO[Boolean] = {
      val newBoard = makeMove(move)

      server.checkGameOver(newBoard, new Metadata()).map(_.winner == TicTacSymbols.X)
    }

    def findBlockingMove(moves: Seq[Move]) = {
      moves.find(move => isBlockingMove(move).unsafeRunSync())
    }

    def getRandomMove(moves: Seq[Move]): Move = {
      val randomIndex = Random.nextInt(moves.length)
      moves(randomIndex)
    }

    val availableMoves = getAvailableMoves(request)

    val availablePlayerMoves = getAvailablePlayerMoves(request)

    val winningMove = findWinningMove(availableMoves)

    val blockingMove = findBlockingMove(availablePlayerMoves)

    if (winningMove.isDefined) {
      makeMove(winningMove.get).pure[IO]
    }
    else if (blockingMove.isDefined) {
        makeMove(blockingMove.get.copy(symbol = TicTacSymbols.O, player = Player.COMPUTER)).pure[IO]
      }
    else {
      makeMove(getRandomMove(availableMoves)).pure[IO]
    }
  }
}
