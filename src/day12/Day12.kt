package day12

import AoCTask
import split

// https://adventofcode.com/2023/day/12

data class Row(val values: String, val pattern: List<Int>)

data class Combination(val value: String, val multiplier: Long = 1)

fun String.toRunLengthEncoding(): List<Int> {
    val result = mutableListOf<Int>()
    var count = 0
    for (c in this) {
        if (c == '#') {
            count++
        } else if (count > 0) {
            result.add(count)
            count = 0
        }
    }
    if (count > 0) {
        result.add(count)
    }
    return result
}

fun bruteForceApproach(input: List<String>): Long {
    val rows = input.map { line ->
        val (values, pattern) = line.split(" ")
        Row(values, pattern.split(",").map { it.toInt() })
    }
    val rowsWithCombinations = rows.parallelStream()
        .map {
            it to computeCombinations2(it)
        }
        .map { (row, combinations) ->
            row to combinations.filter {
                it.toRunLengthEncoding() == row.pattern
            }
        }.map {
            it.first to it.second.size.toLong()
        }
        .toList()

    return rowsWithCombinations.sumOf { it.second }
}

fun computeCombinations(input: String, index: Int = 0): List<String> {
    return if (index == input.length) {
        listOf(input)
    } else {
        if (input[index] == '?') {
            computeCombinations(input.replaceFirst('?', '.'), index + 1) +
                    computeCombinations(input.replaceFirst('?', '#'), index + 1)
        } else {
            computeCombinations(input, index + 1)
        }
    }
}

tailrec fun computeCombinations2(row: Row, index: Int = 0, combinations: List<String> = listOf(row.values)): List<String> {
    var currentIndex = index
    while (currentIndex < row.values.length && row.values[currentIndex] != '?') {
        currentIndex++
    }
    return if (currentIndex == row.values.length) {
        combinations
    } else {
        val newCombinations = combinations.flatMap { combination ->
            if (combination[currentIndex] == '?') {
                listOf(combination.replaceFirst('?', '.'), combination.replaceFirst('?', '#'))
            } else {
                listOf(combination)
            }
        }.filter {
            val partialPattern = it.take(currentIndex+1).toRunLengthEncoding()
            (partialPattern.isEmpty() || (partialPattern.size <= row.pattern.size && partialPattern.dropLast(1).zip(row.pattern).all { it.first == it.second }) && partialPattern.last() <= row.pattern[partialPattern.size-1])
        }
        computeCombinations2(row, currentIndex + 1, newCombinations)
    }
}

tailrec fun computeCombinations3(row: Row, index: Int = 0, combinations: List<Combination> = listOf(Combination(row.values))): List<Combination> {
    var currentIndex = index
    while (currentIndex < row.values.length && row.values[currentIndex] != '?') {
        currentIndex++
    }
    return if (currentIndex == row.values.length) {
        combinations
    } else {
        val newCombinations = combinations.flatMap { combination ->
            if (combination.value[currentIndex] == '?') {
                listOf(
                    combination.copy(value=combination.value.replaceFirst('?', '.')),
                    combination.copy(value=combination.value.replaceFirst('?', '#'))
                )
            } else {
                listOf(combination)
            }
        }.map {
            it to it.value.take(currentIndex+1).toRunLengthEncoding()
        }.filter { (combination, partialPattern) ->
            (partialPattern.isEmpty() || (partialPattern.size <= row.pattern.size && partialPattern.dropLast(1).zip(row.pattern).all { it.first == it.second }) && partialPattern.last() <= row.pattern[partialPattern.size-1])
        }
        val (lessThanTwoRanges, moreThanTwoRanges) = newCombinations.partition { it.second.size < 2}
        val combinationGroups = moreThanTwoRanges.groupBy { it.second to it.first.value[currentIndex] }
        val combinedCombinations = combinationGroups.map {
            it.value.first().first.copy(multiplier = it.value.sumOf { it.first.multiplier })
        }
        computeCombinations3(row, currentIndex + 1, lessThanTwoRanges.map { it.first } + combinedCombinations)
    }
}

fun bruteForceApproach2(input: List<String>): Long {
    val rows = input.map { line ->
        val (values, pattern) = line.split(" ")
        Row(values, pattern.split(",").map { it.toInt() })
    }
    val rowsWithCombinations = rows.parallelStream()
        .map {
            it to computeCombinations3(it)
        }
        .map { (row, combinations) ->
            row to combinations.filter {
                it.value.toRunLengthEncoding() == row.pattern
            }
        }.map {
            it.first to it.second.sumOf { it.multiplier }
        }
        .toList()

    return rowsWithCombinations.sumOf { it.second }
}

fun part1(input: List<String>): Long {
    return bruteForceApproach(input)
}

fun part2(input: List<String>): Long {
    val repeatedInputs = input.map {line ->
        val (values, pattern) = line.split(" ")
        val repeatedValues = List(5) {values}.joinToString("?")
        val repeatedPattern = List(5) {pattern}.joinToString(",")
        "$repeatedValues $repeatedPattern"
    }
    return bruteForceApproach2(repeatedInputs)
}

fun main() = AoCTask("day12").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 21)
    check(part2(testInput), 525152)

    println(part1(input))
    println(part2(input))
}
