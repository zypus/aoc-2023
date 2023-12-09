package day09

import AoCTask

// https://adventofcode.com/2023/day/9

fun part1(input: List<String>): Int {
    return input.asSequence()
        .map {
            val values = it.split(" ")
              .map { it.toInt() }
            var currentValues = values
            val allLastEntries = mutableListOf(values.last())
            var allZeros = false
            while (!allZeros) {
                var deltasAreAllZeroes = true
                val deltas = currentValues.windowed(2) { (current, next) ->
                    val delta = next - current
                    if (delta != 0) {
                        deltasAreAllZeroes = false
                    }
                    delta
                }
                allLastEntries.add(deltas.last())
                currentValues = deltas
                allZeros = deltasAreAllZeroes
            }
            allLastEntries.sum()
        }.sum()
}

fun part2(input: List<String>): Int {
    return input.asSequence()
        .map {
            val values = it.split(" ")
                .map { it.toInt() }
            var currentValues = values
            val allFirstEntries = mutableListOf(values.first())
            var allZeros = false
            while (!allZeros) {
                var deltasAreAllZeroes = true
                val deltas = currentValues.windowed(2) { (current, next) ->
                    val delta = next - current
                    if (delta != 0) {
                        deltasAreAllZeroes = false
                    }
                    delta
                }
                allFirstEntries.add(deltas.first())
                currentValues = deltas
                allZeros = deltasAreAllZeroes
            }
            allFirstEntries.reversed().reduce { acc, i -> i - acc }
        }.sum()
}

fun main() = AoCTask("day09").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 114)
    check(part2(testInput), 2)

    println(part1(input))
    println(part2(input))
}
