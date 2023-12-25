package day24

import AoCTask
import Vector2
import Vector2D
import Vector2L
import Vector3
import Vector3D
import Vector3L
import cross
import length
import minus
import normalized
import printlnEach

// https://adventofcode.com/2023/day/24

data class Hailstone(val position: Vector3L, val velocity: Vector3L)

fun parseInput(input: List<String>): List<Hailstone> {
    return input.map { line ->
        val (pos, vel) = line.split(" @ ")
        val (px, py, pz) = pos.split(", ").map { it.trim().toLong() }
        val (vx, vy, vz) = vel.split(", ").map { it.trim().toLong() }
        Hailstone(Vector3L(px, py, pz), Vector3L(vx, vy, vz))
    }
}

fun Hailstone.intersection2d(other: Hailstone): Vector2D? {
    val p1 = Vector2L(position.x, position.y)
    val p2 = Vector2L(other.position.x, other.position.y)
    val v1 = Vector2L(velocity.x, velocity.y)
    val v2 = Vector2L(other.velocity.x, other.velocity.y)
    val a1 = if (v1.x != 0L) v1.y.toDouble() / v1.x else 0.0
    val a2 = if (v2.x != 0L) v2.y.toDouble() / v2.x else 0.0
    val c1 = -(a1 * p1.x) + p1.y
    val c2 = -(a2 * p2.x) + p2.y
    val u1 = Vector3D(a1, -1.0, c1)
    val u2 = Vector3D(a2, -1.0, c2)
    val intersection = u1.cross(u2)
    return if (intersection.z == 0.0) {
        null
    } else {
        Vector2D(intersection.x / intersection.z, intersection.y / intersection.z)
    }
}

fun part1(input: List<String>, areaX: LongRange, areaY: LongRange): Int {
    val hailstones = parseInput(input)
    val intersections = hailstones.flatMapIndexed {i, a ->
        hailstones.drop(i+1).mapNotNull { b ->
            val inter = a.intersection2d(b)
            if (inter != null) {
                val v1 = Vector2D(a.velocity.x.toDouble(), a.velocity.y.toDouble())
                val v2 = Vector2D(b.velocity.x.toDouble(), b.velocity.y.toDouble())
                val d1 = inter - Vector2D(a.position.x.toDouble(), a.position.y.toDouble())
                val d2 = inter - Vector2D(b.position.x.toDouble(), b.position.y.toDouble())
                inter.takeIf {
                    (v1.normalized() - d1.normalized()).length() < 0.0001 && (v2.normalized() - d2.normalized()).length() < 0.0001
                }
            } else {
                null
            }
        }
    }
    val intersectionsInArea = intersections.filter {
        it.x in areaX.first.toDouble()..areaX.last.toDouble() &&
                it.y in areaY.first.toDouble()..areaY.last.toDouble()
    }
    return intersectionsInArea.size
}

fun part2(input: List<String>): Int {
    return input.size
}

fun main() = AoCTask("day24").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput, 7L..27L, 7L..27L), 2)
    //check(part2(testInput), 1)

    val range = 200000000000000L..400000000000000L
    println(part1(input, range, range))
    println(part2(input))
}
