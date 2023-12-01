package day01

import AoCTask

// https://adventofcode.com/2023/day/1

fun part1(input: List<String>): Int {
    return input.sumOf { line ->
        val digits = line.filter { it.isDigit() }
        "${digits.first()}${digits.last()}".toInt()
    }
}

val numbers = listOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine")

fun part2(input: List<String>): Int {
    return input.sumOf { line ->
        val digits = mutableListOf<Int>()
        var currentLine = line
        while (currentLine.isNotEmpty()) {
            val first = currentLine.first()
            currentLine = if (first.isDigit()) {
                digits.add(first.toString().toInt())
                currentLine.drop(1)
            } else {
                val index = numbers.indexOfFirst { currentLine.startsWith(it) }
                if (index >= 0) {
                    digits.add(index+1)
                    // NOTE: numbers may overlap, hence we cannot just drop all chars of the number
                    // currentLine.drop(numbers[index].length)
                    currentLine.drop(1)
                } else {
                    currentLine.drop(1)
                }
            }
        }
        val value = digits.first() * 10 + digits.last()
        println("$line -> $digits -> $value")
        value
    }
}

fun main() = AoCTask("day01").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 142)
    check(part2(readTestInput(2)), 281)

    println(part1(input))
    println(part2(input))
}
