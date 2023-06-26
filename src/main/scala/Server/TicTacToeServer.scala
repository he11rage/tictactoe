package Server

import cats.effect._
import io.grpc.Metadata
import fs2.grpc.syntax.all._
import io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService
import tictactoe.tictactoe.{Boards, TicTacToeBoardFs2Grpc}

object TicTacToeServer extends ResourceApp.Forever {

  override def run(args: List[String]): Resource[IO, Unit] = {
    for {
      service <- TicTacToeBoardFs2Grpc.bindServiceResource(new TicTacToeServerImpl)
      server <- NettyServerBuilder.forPort(9001).addService(service)
        .addService(ProtoReflectionService.newInstance())
        .resource[IO]
      _ <- Resource.eval(IO.println("server start"))
    } yield server.start()
  }
}
