package day11

import AoCTask
import Vector2
import Vector2L
import manhattanDistanceTo
import kotlin.math.abs

// https://adventofcode.com/2023/day/11

private fun computeDistances(input: List<String>, dialation: Long): Long {
    val width = input[0].length.toLong()
    val height = input.size.toLong()
    val stars = input.flatMapIndexed { y, row ->
        row.mapIndexedNotNull { x, c ->
            if (c == '#') {
                Vector2L(x, y)
            } else {
                null
            }
        }
    }

    val emptyColumns = (0..<width).toSet() - stars.map { it.x }.distinct().toSet()
    val emptyRows = (0..<height).toSet() - stars.map { it.y }.distinct().toSet()

    val rightShiftedStars = emptyColumns.reversed().fold(stars) { acc, x ->
        acc.map {
            if (it.x > x) it.copy(x = it.x + dialation - 1) else it
        }
    }
    val shiftedStars = emptyRows.reversed().fold(rightShiftedStars) { acc, y ->
        acc.map {
            if (it.y > y) it.copy(y = it.y + dialation - 1) else it
        }
    }

    val starPairs = shiftedStars.flatMapIndexed { index: Int, star: Vector2L ->
        shiftedStars.drop(index + 1).map { star to it }
    }

    val distances = starPairs.map {
        it.first.manhattanDistanceTo(it.second)
    }

    return distances.sum()
}

fun part1(input: List<String>): Long {
    return computeDistances(input, 2L)
}

fun part2(input: List<String>): Long {
    return computeDistances(input, 1_000_000L)
}

fun main() = AoCTask("day11").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 374)
    check(computeDistances(testInput, 10), 1030)
    check(computeDistances(testInput, 100), 8410)
    //check(part2(testInput), 1)

    println(part1(input))
    println(part2(input))
}
