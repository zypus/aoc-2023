package day18

import AoCTask
import Vector2
import Vector2L
import blue
import green
import magenta
import plus
import red
import times
import unaryMinus
import yellow
import kotlin.math.abs

// https://adventofcode.com/2023/day/18

data class Instruction(val direction: Vector2, val distance: Int, val color: String)

enum class Location {
    OUTSIDE, ENTERING_EDGE, INSIDE, EXITING_EDGE
}

private fun parseInstruction(input: List<String>) = input.map {
    val (dir, dist, color) = it.split(" ")
    val direction = when (dir) {
        "U" -> Vector2.UP
        "D" -> Vector2.DOWN
        "L" -> Vector2.LEFT
        "R" -> Vector2.RIGHT
        else -> throw IllegalArgumentException()
    }
    Instruction(direction, dist.toInt(), color)
}

data class Cube(val position: Vector2, val color: String, val digDirection: Vector2)

private fun determineBounds(edge: MutableList<Cube>): Pair<IntRange, IntRange> {
    var minX = Int.MAX_VALUE
    var maxX = Int.MIN_VALUE
    var minY = Int.MAX_VALUE
    var maxY = Int.MIN_VALUE
    edge.forEach {
        if (it.position.x < minX) minX = it.position.x
        if (it.position.x > maxX) maxX = it.position.x
        if (it.position.y < minY) minY = it.position.y
        if (it.position.y > maxY) maxY = it.position.y
    }
    val rangeX = minX..maxX
    val rangeY = minY..maxY
    return Pair(rangeX, rangeY)
}

fun part1(input: List<String>): Int {
    val instructions = parseInstruction(input)
    var currentCube = Cube(Vector2.ZERO, "#000000", Vector2.ZERO)
    val edge = mutableListOf(currentCube)
    instructions.forEach { instruction ->
        repeat(instruction.distance) {
            currentCube = Cube(currentCube.position + instruction.direction, instruction.color, instruction.direction)
            edge.add(currentCube)
        }
    }
    edge.removeAt(0)
    val (rangeX, rangeY) = determineBounds(edge)
    val edgeSet = edge.map { it.position }.toSet()
    var location = Location.OUTSIDE
    var space = 0

    val rows = edge.groupBy {
        it.position.y
    }

    var outsideDirection: Vector2? = null

    val rowRanges = rows.mapValues {
        var rangeStart = -2
        var rangeEnd = -2
        val ranges = mutableListOf<IntRange>()
        it.value.sortedBy { it.position.x }.forEach { cube ->
            if (cube.position.x == rangeEnd + 1) {
                rangeEnd = cube.position.x
            } else {
                if (rangeStart != -2) {
                    ranges.add(rangeStart..rangeEnd)
                }
                rangeStart = cube.position.x
                rangeEnd = rangeStart
            }
        }
        if (rangeStart != -2) {
            ranges.add(rangeStart..rangeEnd)
        }
        ranges
    }

    rangeY.forEach { y ->
        val rowRange = rowRanges[y] ?: emptyList()
        rangeX.forEach { x ->
            val pos = Vector2(x, y)
            if (pos in edgeSet) {
                space++
                val cubeIndex = edge.indexOfFirst { it.position == pos }
                val cube = edge[cubeIndex]
                val range = rowRange.find { it.contains(pos.x) }!!
                val digDirection = if (cube.digDirection == Vector2.LEFT) {
                    val leftRangeIndex = edge.indexOfFirst { it.position.y == y && it.position.x == range.last }
                    if (leftRangeIndex < cubeIndex) {
                        edge.getOrNull(leftRangeIndex-1)?.digDirection ?: edge.last().digDirection
                    } else {
                        edge.getOrNull(leftRangeIndex+1)?.digDirection ?: edge[0].digDirection
                    }
                } else if (cube.digDirection == Vector2.RIGHT) {
                    val rightRangeIndex = edge.indexOfFirst { it.position.y == y && it.position.x == range.last }
                    if (rightRangeIndex < cubeIndex) {
                        edge.getOrNull(rightRangeIndex-1)?.digDirection ?: edge.last().digDirection
                    } else {
                        edge.getOrNull(rightRangeIndex+1)?.digDirection ?: edge[0].digDirection
                    }
                } else {
                    cube.digDirection
                }
                if (outsideDirection == null && (digDirection == Vector2.UP || digDirection == Vector2.DOWN)) {
                    outsideDirection = digDirection
                }
                if (digDirection == outsideDirection) {
                    location = Location.ENTERING_EDGE
                } else if (-digDirection == outsideDirection) {
                    location = Location.EXITING_EDGE
                }
                if (pos == Vector2.ZERO) {
                    print("X".yellow())
                } else {
                    when(cube.digDirection) {
                        Vector2.UP -> print("U".blue())
                        Vector2.DOWN -> print("D".blue())
                        Vector2.LEFT -> print("L".red())
                        Vector2.RIGHT -> print("R".red())
                    }
                }
            } else {
                if (location == Location.ENTERING_EDGE) {
                    location = Location.INSIDE
                } else if (location == Location.EXITING_EDGE) {
                    location = Location.OUTSIDE
                }
                if (location == Location.INSIDE) {
                    space++
                    print("#".green())
                } else {
                    print(".")
                }
            }
        }
        location = Location.OUTSIDE
        println()
    }
    return space
}

data class CubeRange(val from: Vector2L, val to: Vector2L, val digDirection: Vector2L)

fun part2(input: List<String>): Long {
    val instructions = parseInstruction(input)
    var minX = Long.MAX_VALUE
    var maxX = Long.MIN_VALUE
    var minY = Long.MAX_VALUE
    var maxY = Long.MIN_VALUE
    val updateBounds = { pos: Vector2L ->
        if (pos.x < minX) minX = pos.x
        if (pos.x > maxX) maxX = pos.x
        if (pos.y < minY) minY = pos.y
        if (pos.y > maxY) maxY = pos.y
    }
    var currentPosition = Vector2L.ZERO
    val cubeRanges = instructions.map {
        updateBounds(currentPosition)
        val hex = it.color.trim('#', '(', ')')
        val distance = hex.dropLast(1).toInt(16)
        val direction = when (hex.last()) {
            '0' -> Vector2L.RIGHT
            '1' -> Vector2L.DOWN
            '2' -> Vector2L.LEFT
            '3' -> Vector2L.UP
            else -> throw IllegalArgumentException()
        }
        val nextPosition = currentPosition + distance * direction
        updateBounds(nextPosition)
        val cubeRange = CubeRange(currentPosition, nextPosition, direction)
        currentPosition = nextPosition
        cubeRange
    }
//    println("Bounds: $minX..$maxX, $minY..$maxY")
    val (verticalRanges, horizontalRanges) = cubeRanges.partition {
        it.digDirection == Vector2L.UP || it.digDirection == Vector2L.DOWN
    }
    var outsideDirection: Vector2L? = null
    var space = verticalRanges.sumOf { abs(it.to.y - it.from.y) + 1 } +
            horizontalRanges.sumOf { abs(it.to.x - it.from.x) - 1 }
    (minY..maxY).forEach { y ->
        val inRangeRanges = verticalRanges.filter {(from, to, _) ->
            minOf(from.y, to.y) <= y && y <= maxOf(from.y, to.y)
        }.sortedBy {
            it.from.x
        }
        val horizontalInRangeRanges = horizontalRanges.filter {(from, _, _) ->
            from.y == y
        }.map {
            minOf(it.from.x, it.to.x)..maxOf(it.from.x, it.to.x)
        }
        if (outsideDirection == null) {
            outsideDirection = inRangeRanges.first().digDirection
        }
        inRangeRanges.windowed(2) { (current, next) ->
            if (!(-current.digDirection == outsideDirection && next.digDirection == outsideDirection)) {
                val center = (current.from.x + next.from.x) / 2
                if (horizontalInRangeRanges.none { center in it }) {
                    space += maxOf(abs(current.from.x - next.from.x) - 1, 0)
                }
            }
        }
    }
    return space
}

fun part2For1(input: List<String>): Long {
    val instructions = parseInstruction(input)
    var minX = Long.MAX_VALUE
    var maxX = Long.MIN_VALUE
    var minY = Long.MAX_VALUE
    var maxY = Long.MIN_VALUE
    val updateBounds = { pos: Vector2L ->
        if (pos.x < minX) minX = pos.x
        if (pos.x > maxX) maxX = pos.x
        if (pos.y < minY) minY = pos.y
        if (pos.y > maxY) maxY = pos.y
    }
    var currentPosition = Vector2L.ZERO
    val cubeRanges = instructions.map {
        updateBounds(currentPosition)
        val distance = it.distance
        val direction = Vector2L(it.direction.x, it.direction.y)
        val nextPosition = currentPosition + distance * direction
        updateBounds(nextPosition)
        val cubeRange = CubeRange(currentPosition, nextPosition, direction)
        currentPosition = nextPosition
        cubeRange
    }
    println("Bounds: $minX..$maxX, $minY..$maxY")
    val (verticalRanges, horizontalRanges) = cubeRanges.partition {
        it.digDirection == Vector2L.UP || it.digDirection == Vector2L.DOWN
    }
    var outsideDirection: Vector2L? = null
    var space = verticalRanges.sumOf { abs(it.to.y - it.from.y) + 1 } +
            horizontalRanges.sumOf { abs(it.to.x - it.from.x) - 1 }
    (minY..maxY).forEach { y ->
        val inRangeRanges = verticalRanges.filter {(from, to, _) ->
            minOf(from.y, to.y) <= y && y <= maxOf(from.y, to.y)
        }.sortedBy {
            it.from.x
        }
        val horizontalInRangeRanges = horizontalRanges.filter {(from, _, _) ->
            from.y == y
        }.map {
            minOf(it.from.x, it.to.x)..maxOf(it.from.x, it.to.x)
        }
        if (outsideDirection == null) {
            outsideDirection = inRangeRanges.first().digDirection
        }
        inRangeRanges.windowed(2) { (current, next) ->
            if (!(-current.digDirection == outsideDirection && next.digDirection == outsideDirection)) {
                val center = (current.from.x + next.from.x) / 2
                if (horizontalInRangeRanges.none { center in it }) {
                    space += maxOf(abs(current.from.x - next.from.x) - 1, 0)
                }
            }
        }
        val xRanges = inRangeRanges.windowed(2) { (current, next) ->
            val inside = if (!(-current.digDirection == outsideDirection && next.digDirection == outsideDirection)) {
                val center = (current.from.x + next.from.x) / 2
                horizontalInRangeRanges.none { center in it }
            } else {
                false
            }
            current.from.x..next.from.x to inside
        }
        (minX..maxX).forEach { x ->
            val xRange =  xRanges.firstOrNull { x in it.first }
            val range = inRangeRanges.firstOrNull { x == it.from.x}
            if (range != null) {
                if (range.digDirection == Vector2L.UP) {
                    print("#".blue())
                } else {
                    print("#".red())
                }
            } else {
                if (xRange != null) {
                    if (xRange.second) {
                        print("#".green())
                    } else {
                        print("#".magenta())
                    }
                } else {
                    print(".")
                }
            }
        }
        println()
    }
    return space
}

fun main() = AoCTask("day18").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 62)
//    check(part2For1(testInput), 62)
//    check(part2For1(input), 70253)
    check(part2(testInput), 952408144115)

    println(part1(input))
    println(part2(input))
}
