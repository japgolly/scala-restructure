package japgolly.scala_restructure

import japgolly.{scala_restructure => E}

sealed abstract class EngineDef(final val priority: Int, final val engine: Engine)

object EngineDef {

  case object AlignDirectoriesToPackages extends EngineDef(1, E.AlignDirectoriesToPackages)
  case object AlignFileToTypes           extends EngineDef(2, E.AlignFileToTypes)

  val enabled = Set[EngineDef](
    AlignDirectoriesToPackages,
    AlignFileToTypes,
  )
}
