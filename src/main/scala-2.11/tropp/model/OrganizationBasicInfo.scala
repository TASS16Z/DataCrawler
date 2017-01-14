package tropp.model

/**
  * Row from organization list.
  *
  * @param krs          NR KRS
  * @param nip          NR NIP
  * @param name         NAZWA
  * @param voivodeship  WOJEWÓDZTWO
  * @param district     POWIAT
  * @param commune      GMINA
  * @param city         MIEJSCOWOŚĆ
  */
case class OrganizationBasicInfo(krs: String, nip: Option[String], name: String,
                                 voivodeship: String, district: String, commune: String, city: String)
