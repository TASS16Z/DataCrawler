name := "trOPP"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= {

  Seq(
    // HTTP client
    "com.eed3si9n" %% "gigahorse-core" % "0.1.1",

    // Neo4j client
    "org.anormcypher" %% "anormcypher" % "0.9.1",

    // ODF toolkit
    "org.odftoolkit" % "odfdom-java" % "0.8.7"
  )
}

resolvers ++= Seq(
  "anormcypher" at "http://repo.anormcypher.org/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)