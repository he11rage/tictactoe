package Client

import cats.effect._
import cats.effect.std.Console
import io.grpc.Metadata
import fs2.grpc.syntax.all._
import io.grpc.netty.NettyChannelBuilder
import tictactoe.tictactoe.{Boards, GameResult, Move, TicTacSymbols, TicTacToeBoardFs2Grpc, TicTacToeClientFs2Grpc, TicTacToeComputerFs2Grpc}
import tictactoe.tictactoe

class TicTacToeClientImpl(server: TicTacToeBoardFs2Grpc[IO, Metadata], computer: TicTacToeComputerFs2Grpc[IO, Metadata]) extends TicTacToeClientFs2Grpc[IO, Metadata] {
  override def makePlayerMove(request: Move, ctx: Metadata): IO[Boards] = {
    val moveRow = request.row
    val moveColumn = request.column
    val moveSymbol = request.symbol
    val movePlayer = request.player
    val moveBoard = request.board

    if (movePlayer.isPlayer) {
      val updatedRows = moveBoard.get.rows.zipWithIndex.map {
        case (row, rowIndex) =>
          if (rowIndex == moveRow) {
            val updatedSymbols = row.symbols.zipWithIndex.map {
              case (symbol, columnIndex) =>
                if (columnIndex == moveColumn) {
                  if (symbol == TicTacSymbols.Empty) {
                    TicTacSymbols.X
                  }
                  else {
                    symbol
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
      val updatedBoard = moveBoard.get.copy(rows = updatedRows)


      IO.pure(updatedBoard)
    }
    else {

      IO.pure(moveBoard.get)
    }
  }

  def getBoard(): IO[Boards] = {
    server.getBoard(tictactoe.Nothing.defaultInstance, new Metadata())
  }

  def makeComputerMove(board: Boards): IO[Boards] = {
    computer.makeComputerMove(board, new Metadata())
  }

  def checkGameOver(board: Boards): IO[GameResult] = {
    server.checkGameOver(board, new Metadata())
  }


}

object TicTacToeClientImpl {
  def resource(): Resource[IO, TicTacToeClientImpl] = {
    for {
      channelServer <- NettyChannelBuilder
        .forTarget("server:9001")
        .usePlaintext()
        .resource[IO]
      channelComputer <- NettyChannelBuilder
        .forTarget("computer:9002")
        .usePlaintext()
        .resource[IO]
      server <- TicTacToeBoardFs2Grpc.stubResource[IO](channelServer)
      computer <- TicTacToeComputerFs2Grpc.stubResource[IO](channelComputer)
    } yield new TicTacToeClientImpl(server, computer)
  }
}
