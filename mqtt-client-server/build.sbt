lazy val root = project.in(file(".")).aggregate(client, server).settings(
  name := "mqtt-publish-disconnect"
)

lazy val client = project.in(file("client")).settings(
  commonSettings
)

lazy val server = project.in(file("server")).settings(
  commonSettings
)

lazy val commonSettings = Seq(
  libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-mqtt-streaming" % "1.0-M1+64-f2e0c1ca"
)