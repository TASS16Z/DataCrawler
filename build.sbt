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
    "org.odftoolkit" % "odfdom-java" % "0.8.7",

    // OCR library
    "org.bytedeco.javacpp-presets" % "tesseract-platform" % "3.04.01-1.3",

    // PDF -> JPG converter
    //    "org.apache.pdfbox" % "pdfbox" % "2.0.4",
    "org.ghost4j" % "ghost4j" % "1.0.1"
  )
}

resolvers ++= Seq(
  "Geotoolkit.org project" at "http://maven.geotoolkit.org",
  "anormcypher" at "http://repo.anormcypher.org/",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
)