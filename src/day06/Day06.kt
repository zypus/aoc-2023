package day06

import AoCTask

// https://adventofcode.com/2023/day/6

const val WHITESPACE_PATTERN = "\\s+"

/**
 * Calculates the score based on the input lists of moments and distances.
 *
 * @param input The list of strings containing the input data. The first string should contain the moments separated by a colon (':').
 *              The last string should contain the distances separated by a colon (':').
 * @return The calculated score.
 */
fun part1(input: List<String>): Int {
    val momentList = convertToIntegerList(input.first())
    val distanceList = convertToIntegerList(input.last())

    return momentList.zip(distanceList).map { (moment, threshold) ->
        val scoreCounts = (0..moment).map { holdingTime ->
            val traveledDistance = (moment - holdingTime) * holdingTime
            traveledDistance
        }.count { it > threshold }
        scoreCounts
    }.reduce { accumulated, score -> accumulated * score }
}

private fun convertToIntegerList(inputString: String): List<Int> {
    val lastTerm = inputString.split(":").last()
    val trimmedResult = lastTerm.trim()
    val splitByWhitespace = trimmedResult.split(WHITESPACE_PATTERN.toRegex())

    return splitByWhitespace.map { it.toInt() }
}

/**
 * Calculates the range of holding times that result in a score higher than the given highscore.
 *
 * @param input The list of strings containing the input data. The first string should contain the time and highscore separated by a colon (':').
 * @return The range of holding times that result in a score higher than the given highscore.
 */
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
