import org.itmo.logging.plugin.annotations.StateLogging
import org.itmo.logging.plugin.annotations.ToLogFunction

@StateLogging
class A {
    private val myStorage = mutableListOf<Int>()

    @ToLogFunction(true)
    fun oddAddition(x: Int): String {
        if ((myStorage.size + x) % 2 == 0) {
            return "EVEN!"
        } else {
            myStorage.add(x)
            return myStorage.size.toString()
        }
    }

}


fun box(): String {
    val a = A()
    a.oddAddition(12)
    a.oddAddition(43)
    return "OK"
}



