import scala.quoted._
import scala.tasty.Reflection

object Lib {

  inline def foo[T](arg: => T): T = ${ impl('arg) }

  private def impl[T: Type](arg: Expr[T])(implicit refl: Reflection): Expr[T] = {
    arg match {
      case e @ '{ $x: Boolean } => '{ println("Boolean: " + $e); $e }
      case e @ '{ $x: Int } => '{ println("Int: " + $x); $x }
      case '{ println("hello"); $arg } => '{ println("Printed hello and returned " + $arg); $arg }
      case '{ println("world"); $arg: T } => '{ println("Printed world and returned " + $arg); $arg }
      case e @ '{ Some($x: Int) } => '{ println("Some: " + $x); $e }
      case arg => '{ ??? }
    }
  }
}
