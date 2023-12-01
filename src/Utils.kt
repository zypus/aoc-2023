import java.io.File
import java.math.BigInteger
import java.security.MessageDigest

data class AoCTask(val day: String) {

    val workingDir = File("src/$day")
    val inputFileName = day.replaceFirstChar { it.uppercase() }

    /**
     * Reads lines from the given input txt file.
     */
    fun readInput(name: String): List<String> {
        return workingDir.resolve("$name.txt").readLines()
    }

    fun <T> processInput(name: String, block: (input: Sequence<String>) -> T): T {
        return workingDir.resolve("$name.txt").useLines {
            block(it)
        }
    }

    val input by lazy {
        readInput(inputFileName)
    }

    val testInput by lazy {
        readInput(inputFileName + "_test")
    }
    fun readTestInput(n: Int) = readInput(inputFileName + "_test$n")

    fun <T> withInput(block: (Sequence<String>) -> T): T {
        return processInput(inputFileName, block)
    }

    fun check(actual: Int, expected: Int) {
        if (actual != expected) {
            throw RuntimeException("Expected $expected, got $actual")
        }
    }
}

/**
 * Converts string to md5 hash.
 */
fun String.md5(): String = BigInteger(1, MessageDigest.getInstance("MD5").digest(toByteArray())).toString(16)

data class Vector2(val x: Int = 0, val y: Int = 0) {
    override fun toString(): String {
        return "($x,$y)"
    }

    companion object {
        val ZERO = Vector2(0, 0)
        val LEFT = Vector2(-1, 0)
        val RIGHT = Vector2(1, 0)
        val UP = Vector2(0, -1)
        val DOWN = Vector2(0, 1)
    }
}

operator fun Vector2.plus(other: Vector2) = Vector2(x + other.x, y + other.y)
operator fun Vector2.minus(other: Vector2) = Vector2(x - other.x, y - other.y)

data class Vector3(val x: Int = 0, val y: Int = 0, val z: Int = 0) {
    override fun toString(): String {
        return "($x,$y,$z)"
    }

    val elements: List<Int> get() = listOf(x, y, z)
}

operator fun Vector3.plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
operator fun Vector3.minus(other: Vector3) = Vector3(x - other.x, y - other.y, z - other.z)

operator fun Vector3.unaryMinus() = Vector3(-x, -y, -z)

fun Int.squared(): Long = this.toLong() * this.toLong()

fun Vector3.squaredLength(): Long = x.squared() + y.squared() + z.squared()

fun <T> List<T>.permutations(): List<List<T>> {
    return if (size == 1) {
        listOf(this)
    } else {
        flatMapIndexed { index, it ->
            val subList = this.slice(indices.toList().filter { i -> i != index })
            subList.permutations().map { perm ->
                listOf(it) + perm
            }
        }
    }
}

/**
 * The cleaner shorthand for printing output.
 */
fun Any?.println() = println(this)
