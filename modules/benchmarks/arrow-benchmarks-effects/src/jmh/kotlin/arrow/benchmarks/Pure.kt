package arrow.benchmarks

import arrow.core.Either
import arrow.core.getOrHandle
import arrow.core.right
import arrow.effects.IO
import arrow.effects.suspended.error.CatchFx
import arrow.effects.suspended.error.toFx
import arrow.effects.suspended.fx.Fx
import arrow.effects.suspended.fx.not
import arrow.unsafe
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import arrow.effects.extensions.fx.unsafeRun.runBlocking as fxRunBlocking
import arrow.effects.extensions.io.unsafeRun.runBlocking as ioRunBlocking

@State(Scope.Thread)
@Fork(2)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
open class Pure {

  @Param("50000")
  var size: Int = 0

  private tailrec suspend fun fxPureLoop(i: Int): suspend () -> Int =
    if (i > size) suspend { i } else fxPureLoop(i + 1)

  @Benchmark
  fun fx(): Int =
    unsafe { fxRunBlocking { Fx { !fxPureLoop(0) } } }

  private tailrec suspend fun <E> catchFxPureLoop(i: Int): Either<E, Int> =
    if (i > size) i.right() else catchFxPureLoop(i + 1)

  @Benchmark
  fun catchFx(): Int =
    unsafe { fxRunBlocking { CatchFx { catchFxPureLoop<String>(0) }.toFx() }.getOrHandle { 0 } }

  private tailrec suspend fun <R, E> envFxPureLoop(i: Int): (R) -> Either<E, Int> {
    val succeed: R.() -> Either<E, Int> = { i.right() }
    return if (i > size) succeed else envFxPureLoop(i + 1)
  }

  @Benchmark
  fun envFx(): Int =
    unsafe {
      fxRunBlocking {
        Fx { envFxPureLoop<Int, Int>(0)(0) }
      }.getOrHandle { 0 }
    }

  private fun ioPureLoop(i: Int): IO<Int> =
    IO.just(i).flatMap { j ->
      if (j > size) IO.just(j) else ioPureLoop(j + 1)
    }

  @Benchmark
  fun io(): Int =
    unsafe { ioRunBlocking { ioPureLoop(0) } }

  @Benchmark
  fun catsIO(): Int =
    arrow.benchmarks.effects.scala.cats.`Pure$`.`MODULE$`.unsafeIOPureLoop(size, 0)

  @Benchmark
  fun scalazZio(): Int =
    arrow.benchmarks.effects.scala.zio.`Pure$`.`MODULE$`.unsafeIOPureLoop(size, 0)

}