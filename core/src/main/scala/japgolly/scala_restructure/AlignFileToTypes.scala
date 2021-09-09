package japgolly.scala_restructure

import scala.annotation.tailrec
import scala.collection.mutable
import scala.meta._

/** Ensures that files contain only 1 top-level type and that this filename represents it.
  * This is effectively Java-style file-naming.
  */
object AlignFileToTypes extends Engine.Simple {

  override def process(file: Path, src: Source): Engine.Result = {

    val scanResults =
      scan(
        rem       = FlatRepr.parse(src),
        ctxName   = "",
        ctx       = Vector.empty,
        blank     = Vector.empty,
        keepBlank = true,
        comment   = Vector.empty,
        processed = Set.empty,
        res       = Map.empty,
      )

    val newFiles: Map[Path, String] =
      scanResults.map { case (name, fs) =>
        (updatedPath(file, name), FlatRepr.toScala(fs))
      }

    // A sole result means the content is unchanged
    val soleResult: Option[(Path, String)] =
      if (newFiles.sizeCompare(1) == 0)
        Some(newFiles.head)
      else
        None

    soleResult match {
      case _ if newFiles.isEmpty => Engine.Result.empty
      case Some((`file`, _))     => Engine.Result.empty
      case Some((newFile, _))    => Cmd.Rename(from = file, to = newFile).toEngineResult
      case _ =>

        val firstCmd =
          if (newFiles.contains(file))
            Engine.Result.empty
          else
            Cmd.Delete(file).toEngineResult

        newFiles.foldLeft(firstCmd) { case (rs, (newFile, content)) =>
          val cmd: Cmd =
            if (newFile == file)
              Cmd.Update(file, content)
            else
              Cmd.Create(newFile, content)
          rs ++ cmd.toEngineResult
        }
    }
  }

  private type ScanResults = Map[String, Vector[FlatRepr]]

  private val dirAndFile = "^(.+)/([^/]+)$".r

  private def updatedPath(file: Path, scanResultName: String): Path =
    scanResultName match {

      case dirAndFile(d, f) =>
        import file.{dir => origDir}
        if (origDir == d || origDir.endsWith("/" + d))
          file.copy(file = s"$f.scala")
        else
          Path(s"$origDir/$d", s"$f.scala")

      case _ =>
        file.copy(file = s"$scanResultName.scala")
    }

  @tailrec
  private def scan(rem      : Vector[FlatRepr],
                   ctxName  : String,
                   ctx      : Vector[FlatRepr],
                   blank    : Vector[FlatRepr],
                   keepBlank: Boolean,
                   comment  : Vector[FlatRepr],
                   processed: Set[String],
                   res      : ScanResults,
                  ): ScanResults = {

    def ctxFlushed: Vector[FlatRepr] = {
      var ctx2 = ctx
      if (blank.nonEmpty) ctx2 ++= blank
      if (comment.nonEmpty) ctx2 ++= comment
      ctx2
    }

    if (rem.isEmpty)
      res
    else
      rem.head match {

        case c: FlatRepr.Comment =>
          scan(
            rem       = rem.tail,
            ctxName   = ctxName,
            ctx       = ctx,
            blank     = blank,
            keepBlank = keepBlank,
            comment   = comment :+ c,
            processed = processed,
            res       = res,
          )

        case b: FlatRepr.BlankLine =>
          if (comment.nonEmpty)
            scan(
              rem       = rem.tail,
              ctxName   = ctxName,
              ctx       = ctx,
              blank     = blank,
              keepBlank = keepBlank,
              comment   = comment :+ b,
              processed = processed,
              res       = res,
            )
          else
            scan(
              rem       = rem.tail,
              ctxName   = ctxName,
              ctx       = ctx,
              blank     = if (keepBlank) blank :+ b else Vector.empty[FlatRepr] :+ b,
              keepBlank = keepBlank,
              comment   = comment,
              processed = processed,
              res       = res,
            )

        case f@ FlatRepr.Stmt(s, _) =>
          val typeName: String =
            s match {
              case d: Defn.Class  => d.name.value
              case d: Defn.Object => d.name.value
              case d: Defn.Trait  => d.name.value
              case p: Pkg.Object  => p.name.value + "/package"
              case _              => ""
            }

          if (typeName.isEmpty)
            // Contextual statement (eg. an `import`)
            scan(
              rem       = rem.tail,
              ctxName   = ctxName,
              ctx       = ctxFlushed :+ f,
              blank     = Vector.empty,
              keepBlank = false,
              comment   = Vector.empty,
              processed = processed,
              res       = res,
            )
          else if (
            (ctxName.nonEmpty && ctxName != typeName) // we're scanning for a different type
            || processed.contains(typeName) // this type has already been processed
          )
            // Skip this top-level type
            scan(
              rem       = rem.tail,
              ctxName   = ctxName,
              ctx       = ctx,
              blank     = Vector.empty,
              keepBlank = false,
              comment   = Vector.empty,
              processed = processed,
              res       = res,
            )
          else {
            // Branch for this top-level type, then resume
            val branchResults =
              separateType(
                name       = typeName,
                newContent = ctxFlushed :+ f,
                rem        = rem,
                processed  = processed,
                res        = res,
              )
            scan(
              rem       = rem.tail,
              ctxName   = ctxName,
              ctx       = ctx,
              blank     = blank,
              keepBlank = false,
              comment   = Vector.empty,
              processed = processed + typeName,
              res       = branchResults,
            )
          }
      }
  }

  private def separateType(name      : String,
                           newContent: Vector[FlatRepr],
                           rem       : Vector[FlatRepr],
                           processed : Set[String],
                           res       : ScanResults,
                          ): ScanResults = {

    val content = res.get(name).fold(newContent)(_ ++ newContent)
    val res2    = res.updated(name, content)

    scan(
      rem       = rem.tail,
      ctxName   = name,
      ctx       = Vector.empty,
      blank     = Vector.empty,
      keepBlank = false,
      comment   = Vector.empty,
      processed = processed,
      res       = res2,
    )
  }

  // ===================================================================================================================

  private implicit val orderingPosition: Ordering[Position] =
    Ordering.by(_.start)

  sealed trait FlatRepr {
    def pos: Position
    def lastToken: Token
  }

  object FlatRepr {
    import scala.meta.{Token => TokenAst}

    final case class Stmt(value: Stat, parent: Option[Pkg]) extends FlatRepr {
      override def pos = value.pos

      override val lastToken: TokenAst = {
        val ts = value.tokens
        value match {
          case _: Pkg => ts.takeWhile(!_.isInstanceOf[TokenAst.LF]).lastOption.getOrElse(ts.last)
          case _      => ts.last
        }
      }
    }

    final case class Comment(value: TokenAst, eol: Option[TokenAst]) extends FlatRepr {
      override def pos       = value.pos
      override def lastToken = value
      val asString           = eol.fold(value.toString)(value.toString + _)
    }

    final case class BlankLine(value: TokenAst) extends FlatRepr {
      override def pos       = value.pos
      override def lastToken = value
    }

    def parse(src: Source): Vector[FlatRepr] = {
      val results = mutable.ArrayBuffer.empty[FlatRepr]

      def getStmts(parent: Option[Pkg], stmts: List[Stat]): Unit =
        stmts.foreach {

          case s: Pkg =>
            results += FlatRepr.Stmt(s, parent)
            getStmts(Some(s), s.stats)

          case s =>
            results += FlatRepr.Stmt(s, parent)
        }

      getStmts(None, src.stats)

      val ignoreTokens = results.iterator.map(_.lastToken.end).toSet
      var tokens       = src.tokens.toList

      while (tokens.nonEmpty) {
        val token = tokens.head
        tokens = tokens.tail
        token match {

          case c: TokenAst.Comment =>
            tokens match {
              case (eol: TokenAst.LF) :: tokensTail =>
                tokens = tokensTail
                results += FlatRepr.Comment(c, Some(eol))
              case _=>
                results += FlatRepr.Comment(c, None)
            }

          case t: TokenAst.LF =>
            if (!ignoreTokens.contains(t.pos.start))
              results += FlatRepr.BlankLine(t)

          case _ =>
        }
      }

      results.sortInPlaceBy(_.pos).toVector
    }

    def toScala(fs: IterableOnce[FlatRepr]): String = {
      val sb = new java.lang.StringBuilder()
      for (r <- fs.iterator) {
        r match {
          case FlatRepr.Stmt(p: Pkg, _) =>
            sb.append(p.copy(stats = Nil))
            sb.append('\n')

          case FlatRepr.Stmt(value, _) =>
            sb.append(value)
            sb.append('\n')

          case c: FlatRepr.Comment =>
            sb.append(c.asString)

          case _: FlatRepr.BlankLine =>
            sb.append('\n')
        }
      }
      sb.toString
    }
  }
}
