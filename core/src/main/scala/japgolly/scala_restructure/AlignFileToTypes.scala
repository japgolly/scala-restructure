package japgolly.scala_restructure

import scala.annotation.tailrec
import scala.collection.mutable
import scala.meta._

object AlignFileToTypes extends Engine.Simple {

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
          case _      =>
//            if (value.toString contains "import java.net") {
//              println()
//              for (t <- ts)
//                println(s"- ${t.pos.start} -> ${t.pos.end}")
//            }
            ts.last
        }
      }
    }

    final case class Comment(value: TokenAst, eol: Option[TokenAst]) extends FlatRepr {
      override def pos = value.pos
      override val lastToken = value
      val asString = eol.fold(value.toString)(value.toString + _)
    }

    final case class BlankLine(value: TokenAst) extends FlatRepr {
      override def pos = value.pos
      override val lastToken = value
    }

//    final case class Token(value: TokenAst) extends FlatRepr {
//      override def pos = value.pos
//      override val lastToken = value
//    }

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

          case t =>
//            println(s" --> [$t] ${t.getClass}")
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

  override def process(file: Path, src: Source): Engine.Result = {

//    println()
//    println("="*100)

    val flatRepr = FlatRepr.parse(src)
//    println(FlatRepr.toScala(flatRepr))
//    println()
//    println("="*100)

    println("="*100)
    println("FlatReprs:")
    for (r <- flatRepr) {
      //      println(s"[${r.pos.start},${r.pos.startLine}:${r.pos.startColumn}] ${r.toString.replace('\n', ' ')}")
      println(s"[${r.pos.start}] ${r.toString.replace('\n', ' ')}")
    }
    println("="*100)
//    println()

    type Result = Map[String, Vector[FlatRepr]]
/*
[0] Comment(// ah,Some( ))
[6] Stmt(package a.b package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 },None)
[18] Stmt(package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 },Some(package a.b package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 }))
[30] BlankLine( )
[31] Comment(// JAVA!,Some( ))
[40] Stmt(import java.net._,Some(package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 }))
[58] BlankLine( )
[59] Comment(// yay,Some( ))
[66] Stmt(class Yay,Some(package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 }))
[76] BlankLine( )
[77] Stmt(import java.io._,Some(package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 }))
[94] Stmt(trait Z,Some(package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 }))
[102] BlankLine( )
[103] BlankLine( )
[104] Comment(/** blah1   * blah2   */,Some( ))
[129] Stmt(object Yay { def hehe = 1 },Some(package c.d  // JAVA! import java.net._  // yay class Yay  import java.io._ trait Z   /** blah1   * blah2   */ object Yay { def hehe = 1 }))

--------------------------------------------------------------------------------------------
[0]
[6] comment=$0
[18] pkg=($0, $6)
[30] pkg=($0, $6, $18)
[31] pkg=($0, $6, $18, $30)
[40] pkg=($0, $6, $18, $30) comment=$31
[58] pkg=($0, $6, $18, $30, $31, $40)
[59] pkg=($0, $6, $18, $30, $31, $40) blank=$58
[66] pkg=($0, $6, $18, $30, $31, $40) blank=$58 comment=$59
[76] CLASS Yay! Add $pkg + $blank + $comment to Yay, continue to scan for Yay only

---- Yay only
[76]
[77] blank=$76?
[94] pkg=($76, $77)
[102] pkg=($76, $77) IGNORE trait Z
[103] pkg=($76, $77) blank=$102?
[104] pkg=($76, $77) blank=$103?
[129] pkg=($76, $77) blank=$103? comment=$104
FOUND OBJECT Yay,  Add $pkg + $blank + $comment to Yay, continue to scan for Yay only

---- Post-Yay
[66]
[76] pkg=($0, $6, $18, $30, $31, $40) blank=$58?
[77] pkg=($0, $6, $18, $30, $31, $40) blank=$76
[94] pkg=($0, $6, $18, $30, $31, $40, $76, $77)
TRAIT Z! Add $pkg to Z, continue to scan for Z only

---- Z only
[102]
[103] blank=$102?
[104] blank=$103?
[129] blank=$103? comment=$104
 Skip

---- Post-Z
[94]
[102] pkg=($0, $6, $18, $30, $31, $40, $76, $77)
[103] pkg=($0, $6, $18, $30, $31, $40, $76, $77) blank=$102
[104] pkg=($0, $6, $18, $30, $31, $40, $76, $77) blank=($102, $103)
[129] pkg=($0, $6, $18, $30, $31, $40, $76, $77) blank=($102, $103) comment=$104
 Skip
*/

    // TODO: tailrec
    def blah(rem: Vector[FlatRepr],
             ctxName: String,
             ctx: Vector[FlatRepr],
             blank: Vector[FlatRepr],
             keepBlank: Boolean,
             comment: Vector[FlatRepr],
             processed: Set[String],
             res: Result,
            ): Result = {

      def ctxFlushed = {
        var ctx2 = ctx
        if (blank.nonEmpty) ctx2 ++= blank
        if (comment.nonEmpty) ctx2 ++= comment
        ctx2
      }

      def found(name: String, f: FlatRepr): Result = {
        println(s"---- PUSH $name")
        val newContent = ctxFlushed :+ f

        println(s"     Adding content:")
        val ind = "       "
        println(ind + FlatRepr.toScala(newContent).replace("\n", "\n" + ind))

        val content = res.get(name).fold(newContent)(_ ++ newContent)
        val res2 = res.updated(name, content)

        val x =
        blah(
          rem = rem.tail,
          ctxName = name,
          ctx = Vector.empty,
          blank = Vector.empty,
          keepBlank = false,
          comment = Vector.empty,
          processed = processed,
          res = res2,
        )

        println(s"---- POP $name")
        x
      }

      if (rem.isEmpty)
        res
      else {

        def showFS(fs: Vector[FlatRepr]) = fs.map(_.pos.start.toString).mkString("(", ",", ")")
        val bf = if (keepBlank) "" else "?"
        println(s"[${rem.head.pos.start}] ctx=${showFS(ctx)} blank=${showFS(blank)}$bf comm=${showFS(comment)}")

        rem.head match {

          case c: FlatRepr.Comment =>
            blah(
              rem = rem.tail,
              ctxName = ctxName,
              ctx = ctx,
              blank = blank,
              keepBlank = keepBlank,
              comment = comment :+ c,
              processed = processed,
              res = res,
            )

          case b: FlatRepr.BlankLine =>
            if (comment.nonEmpty)
              blah(
                rem = rem.tail,
                ctxName = ctxName,
                ctx = ctx,
                blank = blank,
                keepBlank = keepBlank,
                comment = comment :+ b,
                processed = processed,
                res = res,
              )
            else
              blah(
                rem = rem.tail,
                ctxName = ctxName,
                ctx = ctx,
                blank = if (keepBlank) blank :+ b else Vector.empty[FlatRepr] :+ b,
                keepBlank = keepBlank,
                comment = comment,
                processed = processed,
                res = res,
              )

          case f@ FlatRepr.Stmt(s, _) =>
            val typeName: String =
              s match {
                case d: Defn.Class  => d.name.value
                case d: Defn.Object => d.name.value
                case d: Defn.Trait  => d.name.value
                case _              => ""
              }

            if (typeName.isEmpty)
              // Contextual statement
              blah(
                rem = rem.tail,
                ctxName = ctxName,
                ctx = ctxFlushed :+ f,
                blank = Vector.empty,
                keepBlank = false,
                comment = Vector.empty,
                processed = processed,
                res = res,
              )
            else if (
              (ctxName.nonEmpty && ctxName != typeName) // we're scanning for a different type
              || processed.contains(typeName) // this type has already been processed
            )
              // Skip this top-level type
              blah(
                rem = rem.tail,
                ctxName = ctxName,
                ctx = ctx,
                blank = Vector.empty,
                keepBlank = false,
                comment = Vector.empty,
                processed = processed,
                res = res,
              )
            else
              // Branch for this top-level type, then resume
              blah(
                rem = rem.tail,
                ctxName = ctxName,
                ctx = ctx,
                blank = blank,
                keepBlank = false,
                comment = Vector.empty,
                processed = processed + typeName,
                res = found(typeName, f),
              )
        }
      }
    }

    var xxx =
    blah(flatRepr, "", Vector.empty, Vector.empty, true, Vector.empty, Set.empty, Map.empty)


//    println("RESULTS:")
//
//    xxx.foreach {
//      case (name, reprs) =>
//        println(s">> $name.scala --------------------------------------------------------------------")
//        println(FlatRepr.toScala(reprs))
//    }

//    println()
    println("="*100)
//    println()

    var yyy = xxx.map { case (name, fs) =>
      (file.copy(file = s"$name.scala"), FlatRepr.toScala(fs))
    }

    if (yyy.sizeCompare(1) == 0 && yyy.head._1 == file)
      yyy = Map.empty

    yyy.foreach {
      case (name, content) =>
        println(s">> $name --------------------------------------------------------------------")
        println(content)
    }
    println("="*100)

    val er =
    if (yyy.isEmpty)
      Engine.Result.empty
    else {
      // TODO: might need to retain orig file
      val init =
        if (yyy.contains(file))
          Engine.Result.empty
        else
          Cmd.Delete(file).toEngineResult
      yyy.foldLeft(init) { case (rs, (newFile, content)) =>
        rs ++ Cmd.Write(newFile, content).toEngineResult
      }
    }

    er
  }
}
