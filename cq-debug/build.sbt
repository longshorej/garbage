lazy val Version = new {
  val streambed = "0.20.0"
}

resolvers += "streambed-repositories" at "https://repositories.streambed.io/jars/"

libraryDependencies ++= Seq(
  "com.cisco.streambed"  %% "streambed-core"  % Version.streambed,
  "com.cisco.streambed"  %% "chronicle-queue" % Version.streambed
)
