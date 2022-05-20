package foo.bar

import org.itmo.logging.plugin.annotations.SomeAnnotation

@SomeAnnotation
fun test() {
    val s = <!UNRESOLVED_REFERENCE!>MyClass<!>().foo()
    s.<!OVERLOAD_RESOLUTION_AMBIGUITY!>inc<!>() // should be an error
}
