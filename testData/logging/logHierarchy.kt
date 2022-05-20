import org.itmo.logging.plugin.annotations.StateLogging
import org.itmo.logging.plugin.annotations.ToLogFunction


@StateLogging
open class A {

}

class B: A() {
    private val x = 47
    private var z = 42

    @ToLogFunction(false)
    fun foo(y: Int) = x + y

    @ToLogFunction(true)
    fun bar(s: String): Int {
        z = s.length
        return x + z + s.length
    }
}

fun box(): String {
    val b = B()
    val result = b.foo(13)
    b.bar("Kotlin is cool")
    return if (result == 60) { "OK" } else { "Fail: $result" }
}