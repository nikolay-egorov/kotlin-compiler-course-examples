import org.itmo.logging.plugin.annotations.StateLogging
import org.itmo.logging.plugin.annotations.ToLogFunction


@StateLogging
open class A {

}

@StateLogging
<!REPEATED_ANNOTATION!>@StateLogging<!>
class <!ANCESTOR_WITH_ANNOTATION!>B<!>: A() {
    private val x = 47

    @ToLogFunction(false)
    fun foo(y: Int) = x + y
}

class C {
    var x = 1
    @ToLogFunction(true)
    fun <!NO_LOGGER_CLASS_ANNOTATION, RETURN_HAS_NO_EFFECT!>withEffect<!>() {
        x = 42
    }
}
@StateLogging
enum class <!TARGET_SHOULD_NOT_BE_ENUM_CLASS!>WrongUsage<!>

<!WRONG_ANNOTATION_TARGET!>@ToLogFunction<!>
val z = 1

<!WRONG_ANNOTATION_TARGET!>@StateLogging<!>
val asClass = 42