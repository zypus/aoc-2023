package day13

import AoCTask
import Vector2
import printlnEach
import split
import transpose

// https://adventofcode.com/2023/day/13

fun findReflectionPlane(input: List<String>): Int? {
    var reflectionCandidates = (1..<input.first().length).toList()
    for (row in input) {
        reflectionCandidates = reflectionCandidates.filter {
            val leftSide = row.take(it).reversed()
            val rightSide = row.drop(it)
            leftSide.zip(rightSide).all { it.first == it.second }
        }
        if (reflectionCandidates.isEmpty()) {
            return null
        }
    }
    return reflectionCandidates.singleOrNull()
}

enum class ReflectionOrientation {
    VERTICAL, HORIZONTAL
}

data class ReflectionScore(val score: Int, val reflectionPlane: Int, val direction: ReflectionOrientation)

fun computeReflectionScores(input: List<String>, direction: ReflectionOrientation): List<ReflectionScore> {
    val width = input.first().length
    val reflectionCandidates = (1..<width).toList()
    return reflectionCandidates.map { reflectionPlane ->
        val score = input.map { row ->
            val leftSide = row.take(reflectionPlane).reversed()
            val rightSide = row.drop(reflectionPlane)
            leftSide.zip(rightSide).count { it.first != it.second }
        }.sum()
        ReflectionScore(score, reflectionPlane, direction)
    }
}

fun part1(input: List<String>): Int {
    val boards = input.split { it.isEmpty() }
    val verticalReflections = boards.map { board ->
        findReflectionPlane(board) ?: 0
    }
    val horizontalReflections = boards.map { board ->
        100 * (findReflectionPlane(board.transpose())?: 0)
    }
    return verticalReflections.sum() + horizontalReflections.sum()
}

fun part2(input: List<String>): Int {
    val boards = input.split { it.isEmpty() }

    val scores = boards.map { board ->
        val vertical = computeReflectionScores(board, ReflectionOrientation.VERTICAL)
        val horizontal = computeReflectionScores(board.transpose(), ReflectionOrientation.HORIZONTAL)
        val scores = vertical + horizontal
        scores.first { it.score == 1 }
    }
    return scores.sumOf { score ->
        if (score.direction == ReflectionOrientation.VERTICAL) {
            score.reflectionPlane
        } else {
            100 * score.reflectionPlane
        }
    }
}

fun main() = AoCTask("day13").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 405)
    check(part2(testInput), 400)

    println(part1(input))
    println(part2(input))
}
