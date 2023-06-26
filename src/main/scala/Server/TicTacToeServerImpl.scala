package Server

import io.grpc.Metadata
import tictactoe.tictactoe.TicTacToeBoardFs2Grpc
import cats.effect.IO
import io.grpc.Metadata
import tictactoe.tictactoe.{Boards, GameResult, Move, Row, TicTacSymbols, TicTacToeBoardFs2Grpc}
import tictactoe.tictactoe

class TicTacToeServerImpl extends TicTacToeBoardFs2Grpc[IO, Metadata] {

  override def getBoard(request: tictactoe.Nothing, ctx: Metadata): IO[Boards] = {
    val rows = List(
      Row(List(TicTacSymbols.Empty, TicTacSymbols.Empty, TicTacSymbols.Empty)),
      Row(List(TicTacSymbols.Empty, TicTacSymbols.Empty, TicTacSymbols.Empty)),
      Row(List(TicTacSymbols.Empty, TicTacSymbols.Empty, TicTacSymbols.Empty))
    )
    IO.pure(Boards(rows))
  }

  override def checkGameOver(requestBoard: Boards, ctx: Metadata): IO[GameResult] = {
    val rows = requestBoard.rows

    for (row <- rows) {
      if (row.symbols.distinct.size == 1 && row.symbols.head != TicTacSymbols.Empty) {
        return IO.pure(GameResult(gameOver = true, row.symbols.head))
      }
    }

    for (column <- rows.head.symbols.indices) {
      val columnSymbols = rows.map(row => row.symbols(column))
      if (columnSymbols.distinct.size == 1 && columnSymbols.head != TicTacSymbols.Empty) {
        return IO.pure(GameResult(gameOver = true, columnSymbols.head))
      }
    }

    val diagonal1Symbols = for (i <- rows.indices) yield rows(i).symbols(i)
    val diagonal2Symbols = for (i <- rows.indices) yield rows(i).symbols(rows.indices.last - i)
    if (diagonal1Symbols.distinct.size == 1 && diagonal1Symbols.head != TicTacSymbols.Empty) {
      return IO.pure(GameResult(gameOver = true, diagonal1Symbols.head))
    }
    if (diagonal2Symbols.distinct.size == 1 && diagonal2Symbols.head != TicTacSymbols.Empty) {
      return IO.pure(GameResult(gameOver = true, diagonal2Symbols.head))
    }

    IO.pure(GameResult(gameOver = false, TicTacSymbols.Empty))
  }
}


