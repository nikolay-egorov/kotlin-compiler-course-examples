// import org.itmo.logging.plugin.*
import org.itmo.logging.plugin.annotations.StateLogging
import org.itmo.logging.plugin.annotations.ToLogFunction


@StateLogging
class SomeClass {
}

@StateLogging
class Another {
    val x = 1

    fun <T> foo(x: T) = x

    @ToLogFunction(true)
    fun testFunc(x: Int, y: Int) = x + y

    fun test(y: Int): Int {
        return x + x + 42
    }

}

fun box(): String {
    val a = Another()
    a.testFunc(12, 12)
    val b = 1
    return "OK"
}