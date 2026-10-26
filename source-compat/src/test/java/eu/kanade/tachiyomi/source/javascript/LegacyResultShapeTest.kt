package eu.kanade.tachiyomi.source.javascript

import app.cash.quickjs.QuickJs
import app.cash.quickjs.QuickJsException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

class LegacyResultShapeTest {

    // quickjs-kt returns Long, List and JsObject. Extensions written for the old binding test `result is Array<*>`
    // (and find no pages when it isn't) or cast with `as Int`. The expectations are the old binding's results for the
    // same scripts on a device.
    @Test
    fun `results keep the old binding's shapes`() {
        QuickJs.create().use { js ->
            js.evaluate("6 / 2") shouldBe 3
            js.evaluate("-7") shouldBe -7
            js.evaluate("2 ** 40") shouldBe 1.099511627776E12
            js.evaluate("1.5") shouldBe 1.5
            js.evaluate("[1, 'a', 2.5]") shouldBe arrayOf<Any?>(1, "a", 2.5)
            val nested = js.evaluate("[1, [2, 3]]").shouldBeInstanceOf<Array<*>>()
            nested[0] shouldBe 1
            nested[1] shouldBe arrayOf<Any?>(2, 3)
            js.evaluate("({ a: 1 })") shouldBe null
            js.evaluate("(function () {})") shouldBe null
            shouldThrow<QuickJsException> { js.evaluate("[{ a: 1 }]") }
        }
    }
}
