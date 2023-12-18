import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import kotlin.math.abs

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
    fun check(actual: Long, expected: Long) {
        if (actual != expected) {
            throw RuntimeException("Expected $expected, got $actual")
        }
    }
}

/**
 * Converts string to md5 hash.
 */
fun String.md5(): String = BigInteger(1, MessageDigest.getInstance("MD5").digest(toByteArray())).toString(16)

fun <E> Collection<E>.split(limit: Int = -1, condition: (E) -> Boolean): List<List<E>> {
    val splits = mutableListOf<List<E>>()
    var currentSplit = mutableListOf<E>()
    for (element in this) {
        if (condition(element) && (limit == -1 || splits.size - 1 < limit)) {
            splits.add(currentSplit)
            currentSplit = mutableListOf()
        } else {
            currentSplit.add(element)
        }
    }
    if (currentSplit.isNotEmpty()) {
        splits.add(currentSplit)
    }
    return splits
}

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

    val manhattanLength: Int get() = abs(x) + abs(y)
}

operator fun Vector2.plus(other: Vector2) = Vector2(x + other.x, y + other.y)
operator fun Vector2.minus(other: Vector2) = Vector2(x - other.x, y - other.y)
operator fun Vector2.unaryMinus(): Vector2 = Vector2(-x, -y)

operator fun Vector2.times(factor: Int) = Vector2(x * factor, y * factor)

operator fun Int.times(vector: Vector2) = Vector2(vector.x * this, vector.y * this)

fun Vector2.manhattanDistanceTo(other: Vector2) = abs(x - other.x) + abs(y - other.y)


data class Vector2L(val x: Long = 0L, val y: Long = 0L) {

    constructor(x: Number, y: Number) : this(x.toLong(), y.toLong())

    override fun toString(): String {
        return "($x,$y)"
    }

    companion object {
        val ZERO = Vector2L(0, 0)
        val LEFT = Vector2L(-1, 0)
        val RIGHT = Vector2L(1, 0)
        val UP = Vector2L(0, -1)
        val DOWN = Vector2L(0, 1)
    }
}

operator fun Vector2L.plus(other: Vector2L) = Vector2L(x + other.x, y + other.y)
operator fun Vector2L.minus(other: Vector2L) = Vector2L(x - other.x, y - other.y)

operator fun Vector2L.times(factor: Int) = Vector2L(x * factor, y * factor)

operator fun Int.times(vector: Vector2L) = Vector2L(vector.x * this, vector.y * this)

fun Vector2L.manhattanDistanceTo(other: Vector2L) = abs(x - other.x) + abs(y - other.y)

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

fun List<String>.transpose(): List<String> {
    val transposed = MutableList(first().length) { "" }
    forEach { line ->
        line.forEachIndexed { index, c ->
            transposed[index] = transposed[index] + c
        }
    }
    return transposed
}
/**
 * The cleaner shorthand for printing output.
 */
fun Any?.println() = println(this)

fun <E> Collection<E>.printlnEach() = forEach { println(it) }

class Grid<T>(cells2D: List<List<T>>): Collection<T> {

    val width: Int = cells2D.getOrNull(0)?.size ?: 0
    val height: Int = cells2D.size

    val cells: List<T> = cells2D.flatten()
    operator fun get(x: Int, y: Int): T = cells[y * width + x]
    fun getOrNull(x: Int, y: Int): T? = if (x in 0..<width && y in 0..<height) {
        get(x, y)
    } else {
        null
    }

    operator fun get(position: Vector2): T = get(position.x, position.y)
    fun getOrNull(position: Vector2): T? = getOrNull(position.x, position.y)

    fun forEachRow(block: (row: List<T>) -> Unit) = cells.windowed(width, width).forEach(block)
    fun forEachRowIndexed(block: (index: Int, row: List<T>) -> Unit) = cells.windowed(width, width).forEachIndexed(block)
    fun <R> mapRows(block: (row: List<T>) -> R) = cells.windowed(width, width).map(block)
    fun <R> mapRowsIndexed(block: (index: Int, row: List<T>) -> R) = cells.windowed(width, width).mapIndexed(block)

    override val size: Int = width * height

    override fun isEmpty(): Boolean {
        return cells.isEmpty()
    }

    override fun iterator(): Iterator<T> {
        return cells.iterator()
    }

    override fun containsAll(elements: Collection<T>): Boolean {
        return cells.containsAll(elements)
    }

    override fun contains(element: T): Boolean {
        return cells.contains(element)
    }

}

fun <L> reconstructPath(cameFrom: Map<L, L>, end: L): List<L> {
    val path = mutableListOf(end)
    var current = end
    while (current in cameFrom) {
        current = cameFrom[current]!!
        path.add(0, current)
    }
    return path
}

val FOURWAY_DIRECTIONS = listOf(
    Vector2.LEFT,
    Vector2.RIGHT,
    Vector2.UP,
    Vector2.DOWN
)
val EIGHTWAY_DIRECTIONS = listOf(
    Vector2.LEFT,
    Vector2.RIGHT,
    Vector2.UP,
    Vector2.DOWN,
    Vector2.LEFT + Vector2.UP,
    Vector2.LEFT + Vector2.DOWN,
    Vector2.RIGHT + Vector2.UP,
    Vector2.RIGHT + Vector2.DOWN
)

fun fourWayNeighbourhood(pos: Vector2): List<Vector2> {
    return FOURWAY_DIRECTIONS.map { pos + it }
}

fun eightWayNeighbourhood(pos: Vector2): List<Vector2> {
    return EIGHTWAY_DIRECTIONS.map { pos + it }
}

class MutableMapWithDefault<K, V>(val map: MutableMap<K,V>, val default: () -> V) : MutableMap<K, V> by map {
    override fun get(key: K): V {
        return map.getOrElse(key, default)
    }
}

fun <K, V> MutableMap<K,V>.withDefault(default: () -> V): MutableMapWithDefault<K, V> {
    return if (this is MutableMapWithDefault) {
        MutableMapWithDefault(map, default)
    } else {
        MutableMapWithDefault(this, default)
    }
}

fun <L> aStar(
    start: L,
    isGoal: (L) -> Boolean,
    cost: (from: L, to: L) -> Int,
    neighbourhood: (pos: L) -> List<L>,
    heuristic: (pos: L) -> Int
): List<L>? {
    val cameFrom = mutableMapOf<L, L>()
    val gScore = mutableMapOf<L, Int>().withDefault { Int.MAX_VALUE }
    val fScore = mutableMapOf<L, Int>().withDefault { Int.MAX_VALUE }
    val openSet = PriorityQueue<L> {
        a, b -> fScore[a].compareTo(fScore[b])
    }
    gScore[start] = 0
    fScore[start] = heuristic(start)
    openSet.add(start)

    while (openSet.isNotEmpty()) {
        val current = openSet.remove()
        if (isGoal(current)) {
            return reconstructPath(cameFrom, current)
        }
        neighbourhood(current).forEach { neighbour ->
            val tentativeGScore = gScore[current] + cost(current, neighbour)
            if (tentativeGScore < gScore[neighbour]) {
                cameFrom[neighbour] = current
                gScore[neighbour] = tentativeGScore
                fScore[neighbour] = tentativeGScore + heuristic(neighbour)
                if (neighbour !in openSet) {
                    openSet.add(neighbour)
                }
            }
        }
    }
    return null
}

fun String.bold() = "\u001B[1m$this\u001B[0m"
fun String.green() = "\u001B[32m$this\u001B[0m"