package day02

import AoCTask

// https://adventofcode.com/2023/day/2

data class Game(
    val id: Int,
    val draws: List<Draw>
)

data class Draw(
    val reds: Int,
    val greens: Int,
    val blues: Int
)

fun parseGames(input: List<String>): List<Game> {
    return input.map { line ->
        val (game, drawsString) = line.split(":")
        val id = game.split(" ").last().toInt()
        val draws = drawsString.split(";").map { draw ->
            val counts = draw.split(",").map { it.trim() }
            val colorCounts = counts.associate {
                val (count, color) = it.split(" ")
                color to count.toInt()
            }
            Draw(
                colorCounts.getOrDefault("red", 0),
                colorCounts.getOrDefault("green", 0),
                colorCounts.getOrDefault("blue", 0)
            )
        }
        Game(id, draws)
    }
}

fun part1(input: List<String>): Int {
    val games = parseGames(input)
    val possibleGames = games.filter {
        it.draws.all { draw ->
            draw.reds <= 12 && draw.greens <= 13 && draw.blues <= 14
        }
    }
    return possibleGames.sumOf { it.id }
}

fun part2(input: List<String>): Int {
    val games = parseGames(input)
    val minimumDraws = games.map {
        it.draws.fold(Draw(0, 0, 0)) { acc, draw ->
            Draw(maxOf(acc.reds, draw.reds), maxOf(acc.greens, draw.greens), maxOf(acc.blues, draw.blues))
        }
    }
    return minimumDraws.sumOf {
        it.reds * it.greens * it.blues
    }
}

fun main() = AoCTask("day02").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 8)
    check(part2(testInput), 2286)

    println(part1(input))
    println(part2(input))
}
