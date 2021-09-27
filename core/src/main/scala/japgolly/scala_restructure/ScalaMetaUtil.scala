package japgolly.scala_restructure

import scala.meta._

object ScalaMetaUtil {

  object Implicits {

    final implicit class PositionOps(private val pos: Position) extends AnyVal {

      def isWithin(outer: Position): Boolean =
        pos.start >= outer.start && pos.end <= outer.end
    }

  }
}
