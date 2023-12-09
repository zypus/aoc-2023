package day08

import AoCTask
import split
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

// https://adventofcode.com/2023/day/8

data class Node(val id: String, val left: String, val right: String) {
    val isStartNode: Boolean = id.endsWith("A")
    val isEndNode: Boolean = id.endsWith("Z")
}

val NODE_REGEX = """(\w{3}) = \((\w{3}), (\w{3})\)""".toRegex()

private fun parseInput(input: List<String>): Pair<String, Map<String, Node>> {
    val (instructionSection, nodeSection) = input.split { it.isBlank() }
    val instructions = instructionSection.first()
    val nodes = nodeSection.map {
        val match = NODE_REGEX.matchEntire(it)!!
        val (id, left, right) = match.destructured
        Node(id, left, right)
    }
    val nodeLookup = nodes.associateBy { it.id }
    return Pair(instructions, nodeLookup)
}

fun part1(input: List<String>): Int {
    val (instructions, nodeLookup) = parseInput(input)
    var currentNode = nodeLookup["AAA"]!!
    var steps = 0
    var instructionPointer = 0
    while (currentNode.id != "ZZZ") {
        val direction = instructions[instructionPointer]
        currentNode = if (direction == 'L') {
            nodeLookup[currentNode.left]!!
        } else {
            nodeLookup[currentNode.right]!!
        }
        instructionPointer = (instructionPointer + 1) % instructions.length
        steps++
    }
    return steps
}

data class PathNode(val node: Node, val instructionPointer: Int)

class Path(firstNode: Node) {
    val visitedNodes: MutableList<PathNode> = mutableListOf(PathNode(firstNode, -1))
    var cycleStart = -1
    var cursor = 0

    fun addNode(node: Node, instructionPointer: Int) {
        val pathNode = PathNode(node, instructionPointer)
        if (pathNode !in visitedNodes) {
            visitedNodes.add(pathNode)
            cursor++
        } else if (cycleStart == -1) {
            cycleStart = visitedNodes.indexOf(pathNode)
            cursor = cycleStart
        } else {
            cursor = visitedNodes.indexOf(pathNode)
        }
    }

    val indexOfEndNode: Int get() = visitedNodes.indexOfFirst { it.node.isEndNode }

    val cycleLength: Int get() = visitedNodes.size - cycleStart

    override fun toString(): String {
        return "$visitedNodes ($cycleStart)"
    }
}

fun primeFactors(value: Long): List<Long> {
    var n = value
    val factors = mutableListOf<Long>()
    var i = 2L
    while (i * i <= n) {
        while (n % i == 0L) {
            factors.add(i.toLong())
            n /= i
        }
        i++
    }
    if (n > 1) {
        factors.add(n)
    }
    return factors
}

/**
 * Returns the smallest common product of a and b, by first getting the prime factors of a and b.
 */
fun leastCommonMultiple(a: Long, b: Long): Long {
    val factorsA = primeFactors(a)
    val factorsB = primeFactors(b)
    val factorACounts = factorsA.groupBy { it }.mapValues { (_, values) -> values.size }
    val factorBCounts = factorsB.groupBy { it }.mapValues { (_, values) -> values.size }
    val allFactors = factorsA.toSet() + factorsB.toSet()
    return allFactors.map {
        it.toDouble().pow(max(factorACounts.getOrDefault(it, 0), factorBCounts.getOrDefault(it, 0))).toLong()
    }.reduce { x, y -> x * y}
}

fun part2(input: List<String>): Long {
    val (instructions, nodeLookup) = parseInput(input)
    var currentNodes = nodeLookup.values.filter { it.isStartNode }
    val paths = currentNodes.map { Path(it) }
    var steps = 0L
    var instructionPointer = 0
    println(currentNodes.map { it.id })
    while (!currentNodes.all { it.isEndNode }) {
        val direction = instructions[instructionPointer]
        currentNodes = currentNodes.map { node ->
            if (direction == 'L') {
                nodeLookup[node.left]!!
            } else {
                nodeLookup[node.right]!!
            }
        }
        paths.zip(currentNodes).forEach { (path, node) ->
            path.addNode(node, instructionPointer)
        }
        instructionPointer = (instructionPointer + 1) % instructions.length
        steps++
        if (paths.all { it.cycleStart != -1 }) {
            break
        }
    }
    if(!currentNodes.all { it.isEndNode }) {
        paths.forEach { println("${it.cycleStart} ${it.cursor} ${it.indexOfEndNode} ${it.cycleLength} ${it.visitedNodes.size} ${primeFactors(it.cycleLength.toLong())}") }
        val factors = paths.map {
            primeFactors(it.cycleLength.toLong())
        }.reduce { acc, factors ->
            factors.fold(acc) { current, factor ->
                if (factor !in current) {
                    current + factor
                } else {
                    current
                }
            }
        }
        println(factors)
        val smallestCommonProduct = factors.reduce { acc, factor -> acc * factor }
        println("smallestCommonProduct = $smallestCommonProduct")

        fun updatePaths(paths: List<Path>, newSteps: Long) {
            paths.forEach {path ->
                path.cursor = ((path.cursor - path.cycleStart + newSteps) % path.cycleLength + path.cycleStart).toInt()
            }
        }

        var combinedCycleLength = 0L
        var currentPrimeFactors = emptySet<Long>()

        var totalStepsTaken = 0L

        paths.sortedBy { it.cycleLength }.forEach {path ->
            val stepsNeededForAlignment = if (path.cursor < path.indexOfEndNode) {
                path.indexOfEndNode - path.cursor
            } else {
                path.visitedNodes.size - path.cursor + path.indexOfEndNode - path.cycleStart
            }
            val stepsNeeded = if (combinedCycleLength > 0) {
                var cycleShift = combinedCycleLength % path.cycleLength
                if (cycleShift > path.cycleLength / 2) {
                    cycleShift -= path.cycleLength
                }
                var cyclesNeeded = 0
                println("$cycleShift $stepsNeededForAlignment")
                while (cyclesNeeded * combinedCycleLength % path.cycleLength != stepsNeededForAlignment.toLong()) {
                    cyclesNeeded++
                }
//                val cyclesNeeded = if (cycleShift > 0) {
//                    leastCommonMultiple(cycleShift, stepsNeededForAlignment.toLong()) / cycleShift
//                } else {
//                    leastCommonMultiple(-cycleShift, (path.cycleLength - stepsNeededForAlignment).toLong()) / -cycleShift
//                }
                println("Needs $cyclesNeeded cycles")
                val stepsToTake = cyclesNeeded * combinedCycleLength
                println("$stepsToTake $stepsNeededForAlignment ${stepsToTake % path.cycleLength}")
                val newPrimesFactors = primeFactors(path.cycleLength.toLong())
                currentPrimeFactors = currentPrimeFactors + newPrimesFactors
                combinedCycleLength = currentPrimeFactors.reduce { acc, factor -> acc * factor }
                stepsToTake
            } else {
                currentPrimeFactors = primeFactors(path.cycleLength.toLong()).toSet()
                combinedCycleLength = path.cycleLength.toLong()
                stepsNeededForAlignment.toLong()
            }
            totalStepsTaken += stepsNeeded
            updatePaths(paths, stepsNeeded)
            println("After alignment ($combinedCycleLength):")
            paths.sortedBy { it.cycleLength }.forEach { path ->
                println("${path.cursor} ${path.indexOfEndNode} ${path.cursor == path.indexOfEndNode}")
            }
        }

//        val maxStepForAlignment = paths.maxOf {
//            if (it.cursor < it.indexOfEndNode) {
//                it.indexOfEndNode - it.cursor
//            } else {
//                it.visitedNodes.size - it.cursor + it.indexOfEndNode - it.cycleStart
//            }
//        }
//        paths.forEach {path ->
//            path.cursor = ((path.cursor - path.cycleStart + maxStepForAlignment + smallestCommonProduct) % path.cycleLength + path.cycleStart).toInt()
//        }

        steps += totalStepsTaken
        check(paths.all { it.cursor == it.indexOfEndNode })
    }
    return steps
}

fun main() = AoCTask("day08").run {

    check(leastCommonMultiple(4, 6), 12)
    check(leastCommonMultiple(48, 180), 720)
    check(leastCommonMultiple(3, 17), 51)

    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 2)
    check(part1(readTestInput(2)), 6)
    check(part2(readTestInput(3)), 6)

    println(part1(input))
    println(part2(input))
}
