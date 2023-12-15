package day14

import AoCTask
import printlnEach
import transpose

// https://adventofcode.com/2023/day/14

enum class ShiftDirection {
    NORTH, EAST, SOUTH, WEST
}

fun shiftRocks(rocks: List<String>, reversed: Boolean): List<String> {
    return rocks.map {
        var newColumn = ""
        var gap = 0
        val column = if (reversed) it.reversed() else it
        column.forEach { c ->
            when (c) {
                'O' -> newColumn += 'O'
                '.' -> gap++
                '#' -> {
                    newColumn += ".".repeat(gap) + '#'
                    gap = 0
                }
            }
        }
        newColumn += ".".repeat(gap)
        if (reversed) newColumn.reversed() else newColumn
    }
}

fun cycleRocks(input: List<String>): List<String> {
    var board = input.transpose()
    board = shiftRocks(board, false)
    board = board.transpose()
    board = shiftRocks(board, false)
    board = board.transpose()
    board = shiftRocks(board, true)
    board = board.transpose()
    board = shiftRocks(board, true)
    return board
}

fun scoreRocks(input: List<String>): Int {
    val scores = input.transpose().flatMap {
        it.reversed().mapIndexedNotNull { index, c ->
            if (c == 'O') index + 1 else null
        }
    }
    return scores.sum()
}

fun part1(input: List<String>): Int {
    val columns = input.transpose()
    val movedRocks = shiftRocks(columns, false)
    return scoreRocks(movedRocks.transpose())
}

fun part2(input: List<String>): Int {
    var board = input

    val recordedBoards = mutableListOf<List<String>>()
    val signatures = mutableListOf<Pair<Int, Int>>()
    val signatureSet = mutableSetOf<Pair<Int, Int>>()
    var cycleStart = 0
    var cycleEnd = 0

    for(cycle in 1..1_000_000_000) {
        board = cycleRocks(board)
        recordedBoards.add(board)
        cycleEnd = cycle
        val signature = board.hashCode() to scoreRocks(board)
        if (signature !in signatureSet) {
            signatures.add(signature)
            signatureSet.add(signature)
        } else {
            cycleStart = signatures.indexOf(signature) + 1
            break
        }
    }

    if (cycleEnd != 1_000_000_000) {
        val cycleLength = cycleEnd - cycleStart
        val remainingCycles = 1_000_000_000 - cycleEnd
        val leftOverCycles = remainingCycles % cycleLength
        (1..leftOverCycles).forEach {
            board = cycleRocks(board)
        }
    }
    return scoreRocks(board)
}

fun main() = AoCTask("day14").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 136)
    check(part2(testInput), 64)

    println(part1(input))
    println(part2(input))
}
