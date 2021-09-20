package japgolly.scala_restructure

final case class Cmds(asVector: Vector[Cmd]) {

  @inline def isEmpty = asVector.isEmpty
  @inline def nonEmpty = asVector.nonEmpty
  @inline def length = asVector.length

  def append(cmds: Cmd*): Cmds =
    Cmds(asVector ++ cmds)

  def ++(cmds: IterableOnce[Cmd]): Cmds = {
    val it = cmds.iterator
    if (it.hasNext)
      Cmds(asVector ++ it)
    else
      this
  }

  def ++(cmds: Cmds): Cmds =
    this ++ cmds.asVector

  def toEngineResult: Engine.Result =
    Engine.Result.empty.copy(cmds = this)

  def fileIterator(): Iterator[Path] =
    asVector.iterator.flatMap(_.files)
}

object Cmds {
  val empty: Cmds =
    apply(Vector.empty)
}
