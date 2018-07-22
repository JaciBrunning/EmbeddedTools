package jaci.gradle

interface Resolver<T> {

    fun resolve(o: Any): T

    class ResolveFailedException(msg: String): RuntimeException(msg)
}