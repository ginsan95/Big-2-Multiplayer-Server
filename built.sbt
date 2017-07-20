name := "ScalaChorDaiDeeAkkaServer"

version := "1.0"

scalaVersion := "2.11.7"

offline := true
resolvers += "Local Maven Repository" at "file:///"+Path.userHome+ "/.ivy2/cache"
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= Seq(
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
  "com.typesafe.akka" %% "akka-actor" % "2.4.12",
  "com.typesafe.akka" %% "akka-remote" % "2.4.12"
)

mainClass in assembly := Some("big2.Application")

EclipseKeys.executionEnvironment := Some(EclipseExecutionEnvironment.JavaSE18)

//fork := true

connectInput in run := true