package day01

import AoCTask

// https://adventofcode.com/2023/day/1

fun part1(input: List<String>): Int {
    return input.sumOf { line ->
        val digits = line.filter { it.isDigit() }.map { it.toString().toInt() }
        digits.first() * 10 + digits.last()
    }
}

val numbers = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

fun part2(input: List<String>): Int {
    return input.sumOf { line ->
        val newLine = numbers.foldIndexed(line) { index, acc, number ->
            acc.replace(number, "$number${index+1}$number")
        }
        val digits = newLine.filter { it.isDigit() }.map { it.toString().toInt() }
        digits.first() * 10 + digits.last()
    }
}

fun main() = AoCTask("day01").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 142)
    check(part2(readTestInput(2)), 281)

    println(part1(input))
    println(part2(input))
}
