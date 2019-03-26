package arrow.benchmarks

import arrow.effects.IO
import arrow.effects.extensions.NonBlocking
import arrow.effects.extensions.continueOn
import arrow.effects.extensions.fx2.fx.monad.followedBy
import arrow.effects.extensions.io.monad.followedBy
import arrow.effects.suspended.fx.Fx
import arrow.effects.suspended.fx.not
import arrow.unsafe
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import arrow.effects.extensions.catchfx.async.shift as bioShift
import arrow.effects.extensions.envfx.async.shift as rioShift
import arrow.effects.extensions.fx.async.shift as fxShift
import arrow.effects.extensions.fx.unsafeRun.runBlocking as fxRunBlocking
import arrow.effects.extensions.fx2.fx.async.shift as fx2Shift
import arrow.effects.extensions.fx2.fx.unsafeRun.runBlocking as fx2RunBlocking
import arrow.effects.extensions.io.async.shift as ioShift
import arrow.effects.extensions.io.unsafeRun.runBlocking as ioRunBlocking


@State(Scope.Thread)
@Fork(2)
@Warmup(iterations = 10, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10)
@CompilerControl(CompilerControl.Mode.DONT_INLINE)
open class Async {

  @Param("3000")
  var size: Int = 0

  private suspend fun fxAsyncLoop(i: Int): suspend () -> Int =
    NonBlocking.continueOn {
      if (i > size) i else fxAsyncLoop(i + 1)()
    }

  private fun ioAsyncLoop(i: Int): IO<Int> =
    NonBlocking.ioShift().followedBy(
      if (i > size) IO { i } else ioAsyncLoop(i + 1)
    )

  private fun fx2AsyncLoop(i: Int): arrow.effects.suspended.fx2.Fx<Int> =
    NonBlocking.fx2Shift().followedBy(
      if (1 > size) arrow.effects.suspended.fx2.Fx { 1 } else fx2AsyncLoop(i + 1)
    )

  @Benchmark
  fun fx(): Int =
    unsafe { fxRunBlocking { Fx { !fxAsyncLoop(0) } } }

  @Benchmark
  fun fx2(): Int =
    unsafe { fx2RunBlocking { fx2AsyncLoop(0) } }

  @Benchmark
  fun io(): Int =
    unsafe { ioRunBlocking { ioAsyncLoop(0) } }

  @Benchmark
  fun catsIO(): Int =
    arrow.benchmarks.effects.scala.cats.`Async$`.`MODULE$`.unsafeIOAsyncLoop(size, 0)

  @Benchmark
  fun scalazZIO(): Int =
    arrow.benchmarks.effects.scala.zio.`Async$`.`MODULE$`.unsafeIOAsyncLoop(size, 0)

}
