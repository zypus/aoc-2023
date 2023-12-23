package day22

import AoCTask
import printlnEach

// https://adventofcode.com/2023/day/22

fun IntRange.intersects(other: IntRange): Boolean {
    return other.first in this || other.last in this || this.first in other || this.last in other
}

val letters = ('A'..'Z').toList().map { it.toString() }

data class Brick(val name: String, val xRange: IntRange, val yRange: IntRange, val zRange: IntRange) {

    fun intersects(other: Brick): Boolean {
        return xRange.intersects(other.xRange) && yRange.intersects(other.yRange) && zRange.intersects(other.zRange)
    }

    fun movedBy(dx: Int, dy: Int, dz: Int): Brick {
        return Brick(
            name,
            xRange.first + dx..xRange.last + dx,
            yRange.first + dy..yRange.last + dy,
            zRange.first + dz..zRange.last + dz,
        )
    }

}

fun parseInput(input: List<String>): List<Brick> {
    return input.mapIndexed { index, line ->
        val (from, to) = line.split("~")
        val (fromX, fromY, fromZ) = from.split(",").map { it.toInt() }
        val (toX, toY, toZ) = to.split(",").map { it.toInt() }
        val name = letters.getOrNull(index) ?: index.toString();
        Brick(name, fromX..toX, fromY..toY, fromZ..toZ)
    }
}

fun settleBricks(bricks: List<Brick>): List<Brick> {
    val sortedBricks = bricks.sortedBy { it.zRange.first }
    return sortedBricks.fold(emptyList()) { stack, brick ->
        var currentBrick = brick
        while (stack.none { currentBrick.intersects(it) } && currentBrick.zRange.first > 1) {
            currentBrick = currentBrick.movedBy(0, 0, -1)
        }
        stack + currentBrick.movedBy(0, 0, 1)
    }
}

fun findBricksSupportedBy(brick: Brick, others: List<Brick>): List<Brick> {
    val raisedBrick = brick.movedBy(0, 0, 1)
    return others.filter {
        it != brick && it.intersects(raisedBrick)
    }
}

fun findSupportingBricks(brick: Brick, others: List<Brick>): List<Brick> {
    val loweredBrick = brick.movedBy(0, 0, -1)
    return others.filter {
        it != brick && it.intersects(loweredBrick)
    }
}

fun part1(input: List<String>): Int {
    val bricks = parseInput(input)
    val settledBricks = settleBricks(bricks)
    val desintegratableBricks = settledBricks.filter {
        val supportedBricks = findBricksSupportedBy(it, settledBricks)
        supportedBricks.isEmpty() || supportedBricks.all { findSupportingBricks(it, settledBricks).size > 1 }
    }
    return desintegratableBricks.size
}

fun part2(input: List<String>): Int {
    val bricks = parseInput(input)
    val settledBricks = settleBricks(bricks)
    return settledBricks.sumOf {brick ->
        val remainingBricks = settledBricks - brick
        val movedBricks = settleBricks(remainingBricks)
        (movedBricks.toSet() - remainingBricks.toSet()).size
    }
}

fun main() = AoCTask("day22").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 5)
    check(part2(testInput), 7)

    println(part1(input))
    println(part2(input))
}
