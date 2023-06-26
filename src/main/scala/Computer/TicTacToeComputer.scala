package Computer

import cats.effect._
import io.grpc.Metadata
import fs2.grpc.syntax.all._
import io.grpc.netty.{NettyChannelBuilder, NettyServerBuilder}
import io.grpc.protobuf.services.ProtoReflectionService
import tictactoe.tictactoe.{Boards, TicTacToeBoardFs2Grpc, TicTacToeComputerFs2Grpc}

object TicTacToeComputer extends ResourceApp.Forever {
  override def run(args: List[String]): Resource[IO, Unit] = {
    for {
      channelServer <- NettyChannelBuilder
        .forTarget("server:9001")
        .usePlaintext()
        .resource[IO]
      serverR <- TicTacToeBoardFs2Grpc.stubResource[IO](channelServer)
      service <- TicTacToeComputerFs2Grpc.bindServiceResource(new TicTacToeComputerImpl(serverR))
      server <- NettyServerBuilder.forPort(9002).addService(service)
        .addService(ProtoReflectionService.newInstance())
        .resource[IO]
      _ <- Resource.eval(IO.println("computer server start"))
    } yield server.start()
  }
}
