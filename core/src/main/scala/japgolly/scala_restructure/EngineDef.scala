package japgolly.scala_restructure

import japgolly.{scala_restructure => E}

sealed abstract class EngineDef(final val engine: Engine)

object EngineDef {

  case object AlignFileToTypes           extends EngineDef(E.AlignFileToTypes)
  case object AlignDirectoriesToPackages extends EngineDef(E.AlignDirectoriesToPackages)

  val enabled = Set[EngineDef](
    AlignFileToTypes,
    AlignDirectoriesToPackages,
  )
}
