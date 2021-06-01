name := "balancer"

version := "0.1"

scalaVersion := "2.12.7"

val akkaVersion = "2.5.13"
val akkaHttpVersion = "10.2.4"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor"           % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"          % akkaVersion,
  "com.typesafe.akka" %% "akka-http"            % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-testkit"         % akkaVersion,
  "org.scalatest"     %% "scalatest"            % "3.0.5"
)
