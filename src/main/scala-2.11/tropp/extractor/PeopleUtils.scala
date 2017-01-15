package tropp.extractor

object PeopleUtils {
  def parsePersonNames(raw: List[String]): List[String] = {

    def parse(remain: List[String], parsed: List[String]): List[String] = remain match {
      case x1 :: x2 :: xs =>
        if (x1.count(_ == ' ') == 1 && x2.count(_ == ' ') == 0)
          parse(xs, (x1 :: x2 :: Nil).mkString(" ") :: parsed)
        else
          parse(x2 :: xs, x1 :: parsed)

      case x1 :: Nil =>
        parse(Nil, x1 :: parsed)

      case Nil =>
        parsed
    }

    parse(raw.map(parseLine), Nil)
  }

  def parseLine(list: String): String = {
    def parse(remain: List[String], parsed: List[String]): List[String] = remain match {
      case x1 :: x2 :: xs =>
        // Fix 'Kowal ski' errors
        if(x2.charAt(0).isLower)
          parse(xs, parsed :+ (x1 + x2))
        else
          parse(x2 :: xs, parsed :+ x1)

      case x1 :: Nil =>
        parse(Nil, parsed :+ x1)

      case Nil =>
        parsed
    }

    parse(list.split(" ").toList, Nil).mkString(" ")
  }
}
