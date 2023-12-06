package day06

import AoCTask

// https://adventofcode.com/2023/day/6

fun part1(input: List<String>): Int {
    val times = input.first().split(":").last().trim().split("\\s+".toRegex()).map { it.toInt() }
    val distances = input.last().split(":").last().trim().split("\\s+".toRegex()).map { it.toInt() }
    return times.zip(distances).map { (time, highscore) ->
        (0..time).map { holdTime ->
            val distanceTraveled = (time - holdTime) * holdTime
            distanceTraveled
        }.count {
            it > highscore
        }
    }.fold(1) {
        acc, it -> acc * it
    }
}

fun part2(input: List<String>): Long {
    val time = input.first().split(":").last().replace("\\s+".toRegex(), "").toLong()
    val highscore = input.last().split(":").last().replace("\\s+".toRegex(), "").toLong()
    val distanceTravelled = { holdTime: Long ->
        (time - holdTime) * holdTime
    }
    var minTime = 0L
    var maxTime = time / 2
    var minHoldTime = 0L
    var maxHoldTime = time
    var lastTime = -1L
    while (lastTime != minHoldTime) {
        lastTime = minHoldTime
        minHoldTime = (minTime + maxTime) / 2
        if (distanceTravelled(minHoldTime) > highscore) {
            maxTime = minHoldTime
        } else {
            minTime = minHoldTime + 1
        }
    }
    minTime = time / 2
    maxTime = time
    lastTime = -1
    while (lastTime != maxHoldTime) {
        lastTime = maxHoldTime
        maxHoldTime = (minTime + maxTime) / 2
        if (distanceTravelled(maxHoldTime) > highscore) {
            minTime = maxHoldTime
        } else {
            maxTime = maxHoldTime
        }
    }
    return maxHoldTime - minHoldTime + 1
}

fun main() = AoCTask("day06").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 288)
    check(part2(testInput), 71503)

    println(part1(input))
    println(part2(input))
}
