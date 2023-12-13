package day00

import AoCTask

// <link>

fun part1(input: List<String>): Long {
    return input.size.toLong()
}

fun part2(input: List<String>): Long {
    return input.size.toLong()
}

fun main() = AoCTask("day00").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 1)
    //check(part2(testInput), 1)

    println(part1(input))
    println(part2(input))
}
