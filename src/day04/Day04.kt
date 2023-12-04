package day04

import AoCTask
import kotlin.math.pow

// https://adventofcode.com/2023/day/4

data class Card(val id: Int, val winningNumbers: Set<Int>, val numbers: Set<Int>, var instances: Int = 1) {
    companion object {
        fun fromString(string: String): Card {
            val (card, rest) = string.split(":")
            val id = card.split(" ").last().toInt()
            val (winningNumbers, numbers) = rest.split("|")
            return Card(
                id,
                winningNumbers.trim().split("\\s+".toRegex()).map { it.toInt() }.toSet(),
                numbers.trim().split("\\s+".toRegex()).map { it.toInt() }.toSet()
            )
        }
    }

    val matchingNumbers: Set<Int> get() = numbers.intersect(winningNumbers)

    val value: Int by lazy {
        val count = matchingNumbers.size
        if (count > 0) {
            2.0.pow(count-1).toInt()
        } else {
            0
        }
    }
}

fun part1(input: List<String>): Int {
    val cards = input.map { Card.fromString(it) }
    return cards.sumOf { it.value }
}

fun part2(input: List<String>): Int {
    val cards = input.map { Card.fromString(it) }
    cards.forEachIndexed { index, card ->
        val count = card.matchingNumbers.size
        val cardsToCopy = cards.drop(index + 1).take(count)
        cardsToCopy.forEach { copy -> copy.instances += card.instances}
    }
    return cards.sumOf { it.instances }
}

fun main() = AoCTask("day04").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 13)
    check(part2(testInput), 30)

    println(part1(input))
    println(part2(input))
}
