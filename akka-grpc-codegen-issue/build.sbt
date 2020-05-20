name := "akka-grpc-codegen-issue"

val Versions = new {
  val Akka           = "2.6.5"
  val ScalaPbCommonProtos = "1.17.0-0"
  val Scala = "2.12.11"
}

libraryDependencies ++= Seq(
  "com.typesafe.akka"             %% "akka-actor"                       % Versions.Akka
)

enablePlugins(AkkaGrpcPlugin)

enablePlugins(JavaAgent) // @FIXME this plus next line apparently only required on jre8
javaAgents += "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % "2.0.10" % "runtime;test"

