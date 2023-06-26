ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.11"

version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "tictactoe"
  )

libraryDependencies ++= Seq(
  "io.grpc" % "grpc-netty" % scalapb.compiler.Version.grpcJavaVersion,
  "io.grpc" % "grpc-services" % scalapb.compiler.Version.grpcJavaVersion,
)

dockerRepository := Some("grishnn")

enablePlugins(Fs2Grpc)
enablePlugins(JavaAppPackaging)


