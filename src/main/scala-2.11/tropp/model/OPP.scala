package tropp.model

case class OPP(krs: String, name: String, salaries: Int, averageSalary: Int, noOfEmployees: Int, noOfBeneficiaries: Int,
               city: String, district: String, voivodeship: String, people: List[String],
               areas: Set[String], forms: Set[String])
