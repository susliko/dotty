package dotty.tools
package dotc
package transform

import core._
import TypeErasure.ErasedValueType
import Types._
import Contexts._
import Symbols._
import Names.Name

import dotty.tools.dotc.core.Decorators.*

object TypeUtils {
  /** A decorator that provides methods on types
   *  that are needed in the transformer pipeline.
   */
  extension (self: Type) {

    def isErasedValueType(using Context): Boolean =
      self.isInstanceOf[ErasedValueType]

    def isPrimitiveValueType(using Context): Boolean =
      self.classSymbol.isPrimitiveValueClass

    def isErasedClass(using Context): Boolean =
      self.underlyingClassRef(refinementOK = true).typeSymbol.is(Flags.Erased)

    /** Is this type a checked exception? This is the case if the type
     *  derives from Exception but not from RuntimeException. According to
     *  that definition Throwable is unchecked. That makes sense since you should
     *  neither throw nor catch `Throwable` anyway, so we should not define
     *  a capability to do so.
     */
    def isCheckedException(using Context): Boolean =
      self.derivesFrom(defn.ExceptionClass)
      && !self.derivesFrom(defn.RuntimeExceptionClass)

    def isByName: Boolean =
      self.isInstanceOf[ExprType]

    def ensureMethodic(using Context): Type = self match {
      case self: MethodicType => self
      case _ => if (ctx.erasedTypes) MethodType(Nil, self) else ExprType(self)
    }

    def widenToParents(using Context): Type = self.parents match {
      case Nil => self
      case ps => ps.reduceLeft(AndType(_, _))
    }

    /** The arity of this tuple type, which can be made up of EmptyTuple, TupleX and `*:` pairs,
     *  or -1 if this is not a tuple type.
     */
    def tupleArity(using Context): Int = self match {
      case AppliedType(tycon, _ :: tl :: Nil) if tycon.isRef(defn.PairClass) =>
        val arity = tl.tupleArity
        if (arity < 0) arity else arity + 1
      case self: SingletonType =>
        if self.termSymbol == defn.EmptyTupleModule then 0 else -1
      case self if defn.isTupleClass(self.classSymbol) =>
        self.dealias.argInfos.length
      case _ =>
        -1
    }

    /** The element types of this tuple type, which can be made up of EmptyTuple, TupleX and `*:` pairs */
    def tupleElementTypes(using Context): List[Type] = self match {
      case AppliedType(tycon, hd :: tl :: Nil) if tycon.isRef(defn.PairClass) =>
        hd :: tl.tupleElementTypes
      case self: SingletonType =>
        assert(self.termSymbol == defn.EmptyTupleModule, "not a tuple")
        Nil
      case self if defn.isTupleClass(self.classSymbol) =>
        self.dealias.argInfos
      case _ =>
        throw new AssertionError("not a tuple")
    }

    /** The `*:` equivalent of an instance of a Tuple class */
    def toNestedPairs(using Context): Type =
      TypeOps.nestedPairs(tupleElementTypes)

    def refinedWith(name: Name, info: Type)(using Context) = RefinedType(self, name, info)

    /** The TermRef referring to the companion of the underlying class reference
     *  of this type, while keeping the same prefix.
     */
    def mirrorCompanionRef(using Context): TermRef = self match {
      case OrType(tp1, tp2) =>
        val r1 = tp1.mirrorCompanionRef
        val r2 = tp2.mirrorCompanionRef
        assert(r1.symbol == r2.symbol, em"mirrorCompanionRef mismatch for $self: $r1, $r2 did not have the same symbol")
        r1
      case self @ TypeRef(prefix, _) if self.symbol.isClass =>
        prefix.select(self.symbol.companionModule).asInstanceOf[TermRef]
      case self: TypeProxy =>
        self.underlying.mirrorCompanionRef
    }

    /** Is this type a methodic type that takes implicit parameters (both old and new) at some point? */
    def takesImplicitParams(using Context): Boolean = self.stripPoly match
      case mt: MethodType => mt.isImplicitMethod || mt.resType.takesImplicitParams
      case _ => false
  }
}
