package japgolly.scala_restructure

import scala.meta._

object AlignDirectoriesToPackages extends Engine.Simple {

  override def process(file: Path, src: Source): Engine.Result = {

    // =================================================================================================================
    // Parse

    var candidateDirs = Set.empty[String]
    var pkgObjs = Set.empty[String]

    def process(parentPkg: Option[String], stats: List[Stat]): Unit = {
      var foundDefn = false
      var foundPkgObj = false
      var subs = List.empty[() => Unit]

      for (s <- stats) {
        s match {
          case Pkg(ref, body) =>
            var pkg = ref.toString.replace('.', '/')
            parentPkg.foreach(prefix => pkg = s"$prefix/$pkg")
            subs ::= (() => process(Some(pkg), body))

          case Pkg.Object(_, name, _) =>
            foundPkgObj = true
            pkgObjs += parentPkg.fold("")(_ + "/") + name.value

          case _: Defn | _: Decl =>
            foundDefn = true

          case _ =>
        }
      }

      (foundDefn, foundPkgObj) match {
        case (false, false) => subs.foreach(_())
        case (true , false) => parentPkg.foreach(pkg => candidateDirs += pkg)
        case (_    , true ) =>
      }
    }

    process(None, src.stats)

    // =================================================================================================================
    // Analyse results

    def mv(to: Path): Engine.Result =
      if (file == to)
        Engine.Result.empty
      else
        Cmd.Rename(from = file, to = to).toEngineResult

    def setDirectoryTo(newDir: String): Engine.Result =
      mv(file.copy(dir = newDir))

    (pkgObjs.size, candidateDirs.size) match {

      case (1, _) =>
        mv(Path(pkgObjs.head, "package.scala"))

      case (n, _) if n > 1 =>
        val desc = pkgObjs.toArray.sorted.iterator.map("  - " + _).mkString("\n")
        Engine.Error(file, "Multiple package objects detected:\n" + desc).toResult

      case (0, 0) =>
        setDirectoryTo("")

      case (0, 1) =>
        setDirectoryTo(candidateDirs.head)

      case (0, n) if n > 1 =>
        val desc = candidateDirs.toArray.sorted.iterator.map("  - " + _).mkString("\n")
        Engine.Error(file, "Multiple packages detected:\n" + desc).toResult
    }
  }

}
