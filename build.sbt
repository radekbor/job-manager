name := "jobs"

version := "0.3"

scalaVersion := "2.13.4"

enablePlugins(DockerPlugin)
enablePlugins(JavaAppPackaging)

lazy val Versions = new {
  val akka = "2.6.11"
  val akkaHttp = "10.2.0"
  val slf4j = "1.7.30"
  val akkaHttpCirce = "1.31.0"
  val circe = "0.13.0"
  val enumeratum = "1.6.1"
  val scalatest = "3.2.2"

  val log4s               = "1.10.0-M4"
  val log4j2              = "2.10.0"
}

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % Versions.akka,
  "com.typesafe.akka" %% "akka-stream" % Versions.akka,
  "com.typesafe.akka" %% "akka-testkit" % Versions.akka % Test,

  "com.typesafe.akka" %% "akka-http" % Versions.akkaHttp,
  "com.typesafe.akka" %% "akka-http-spray-json" % Versions.akkaHttp,
  "org.slf4j" % "slf4j-simple" % Versions.slf4j,

  "de.heikoseeberger" %% "akka-http-circe" % Versions.akkaHttpCirce,

  "io.circe" %% "circe-core" % Versions.circe,
  "io.circe" %% "circe-generic" % Versions.circe,
  "io.circe" %% "circe-generic-extras" % Versions.circe,
  "io.circe" %% "circe-parser" % Versions.circe,

  "com.beachape"               %% "enumeratum" % Versions.enumeratum,

  "com.typesafe.akka"   %% "akka-testkit"             % Versions.akka          % Test,
  "com.typesafe.akka"   %% "akka-stream-testkit"      % Versions.akka          % Test,
  "com.typesafe.akka"   %% "akka-http-testkit"        % Versions.akkaHttp      % Test,
  "org.scalatest"       %% "scalatest"                % Versions.scalatest     % Test,

  "org.log4s"                %% "log4s"           % Versions.log4s,
  "org.apache.logging.log4j" % "log4j-api"        % Versions.log4j2,
  "org.apache.logging.log4j" % "log4j-jul"        % Versions.log4j2,
  "org.apache.logging.log4j" % "log4j-core"       % Versions.log4j2,
  "org.apache.logging.log4j" % "log4j-slf4j-impl" % Versions.log4j2
)

scalacOptions ++= Seq(
  "-encoding",
  "utf8", // Option and arguments on same line
  "-Xfatal-warnings", // New lines for each options
  "-deprecation",
  "-unchecked",
  "-language:implicitConversions",
  "-language:higherKinds",
  "-language:existentials",
  "-language:postfixOps"
)

packageName in Docker := "dotdata/" +  packageName.value