name := "akka-streams-server"

libraryDependencies ++= Seq(
  "com.lightbend.akka" %% "akka-stream-alpakka-unix-domain-socket" % "0.18+1-8ea1467f+20180406-1212",
  "com.typesafe.akka"  %% "akka-stream"                            % "2.5.11"
)

Compile / run / fork := true

Compile / run / connectInput := true
