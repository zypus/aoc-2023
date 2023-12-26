package day24

import AoCTask
import Vector2D
import Vector2L
import Vector3D
import Vector3L
import cross
import div
import dot
import length
import minus
import normalized
import plus
import println
import times
import toVector3D
import unaryMinus
import xy
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sqrt
import kotlin.random.Random

// https://adventofcode.com/2023/day/24

data class Hailstone(val id: Int, val position: Vector3L, val velocity: Vector3L)

fun parseInput(input: List<String>): List<Hailstone> {
    return input.mapIndexed { i, line ->
        val (pos, vel) = line.split(" @ ")
        val (px, py, pz) = pos.split(", ").map { it.trim().toLong() }
        val (vx, vy, vz) = vel.split(", ").map { it.trim().toLong() }
        Hailstone(i, Vector3L(px, py, pz), Vector3L(vx, vy, vz))
    }
}

fun Hailstone.intersection2d(other: Hailstone): IntersectionResult {
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
        val distance = abs(c1-c2) / sqrt(1 + a1*a1)
        IntersectionResult.NoIntersection(distance)
    } else {
        IntersectionResult.Intersection(Vector3D(intersection.x / intersection.z, intersection.y / intersection.z))
    }
}

sealed class IntersectionResult {

    abstract val distance: Double
    data class NoIntersection(override val distance: Double) : IntersectionResult()
    data class Intersection(val point: Vector3D) : IntersectionResult() {
        override val distance: Double = 0.0
    }
}

fun Hailstone.intersection3d(other: Hailstone): IntersectionResult {
    val epsilon = 0.5
    val p1 = (position + velocity * -10000000000000).toVector3D()
    val p2 = (position + velocity * 10000000000000).toVector3D()
    val p3 = (other.position + other.velocity * -10000000000000).toVector3D()
    val p4 = (other.position + other.velocity * 10000000000000).toVector3D()

    val p13 = p1 - p3
    val p43 = p4 - p3
    val p21 = p2 - p1

    if (p43.length() < epsilon || p21.length() < epsilon) return IntersectionResult.NoIntersection(Double.MAX_VALUE)

    val d1343 = p13.dot(p43)
    val d4321 = p43.dot(p21)
    val d1321 = p13.dot(p21)
    val d4343 = p43.dot(p43)
    val d2121 = p21.dot(p21)
    val denom = d2121 * d4343 - d4321 * d4321

    if (abs(denom) < epsilon) return IntersectionResult.NoIntersection(Double.MAX_VALUE)

    val numer = (d1343 * d4321 - d1321 * d4343)
    val mua = numer / denom
    val mub = (d1343 + d4321 * mua) / d4343

    val pA = p1 + p21 * mua
    val pB = p3 + p43 * mub
    val distance = (pA - pB).length()
    return if (mua in 0.0..1.0 && mub in 0.0..1.0) {
        IntersectionResult.Intersection((pA + pB) / 2.0)
    } else {
        IntersectionResult.NoIntersection(distance)
    }
}

fun part1(input: List<String>, areaX: LongRange, areaY: LongRange): Int {
    val hailstones = parseInput(input)
    val intersections = hailstones.flatMapIndexed {i, a ->
        hailstones.drop(i+1).mapNotNull { b ->
            val inter = a.intersection2d(b)
            if (inter is IntersectionResult.Intersection) {
                val v1 = Vector2D(a.velocity.x.toDouble(), a.velocity.y.toDouble())
                val v2 = Vector2D(b.velocity.x.toDouble(), b.velocity.y.toDouble())
                val d1 = inter.point.xy - Vector2D(a.position.x.toDouble(), a.position.y.toDouble())
                val d2 = inter.point.xy - Vector2D(b.position.x.toDouble(), b.position.y.toDouble())
                inter.takeIf {
                    (v1.normalized() - d1.normalized()).length() < 0.0001 && (v2.normalized() - d2.normalized()).length() < 0.0001
                }
            } else {
                null
            }
        }
    }
    val intersectionsInArea = intersections.filter {
        it.point.x in areaX.first.toDouble()..areaX.last.toDouble() &&
                it.point.y in areaY.first.toDouble()..areaY.last.toDouble()
    }
    return intersectionsInArea.size
}

fun computeAll3DIntersections(hailstones: List<Hailstone>): List<Pair<Int, IntersectionResult>> {
    return hailstones.flatMapIndexed { i, a ->
        hailstones.drop(i + 1).map { b ->
            a.id to a.intersection3d(b)
        }
    }
//    return hailstones.drop(1).map { b ->
//            hailstones.first().intersection3d(b)
//        }
}

fun computeAll2DIntersections(hailstones: List<Hailstone>): List<Pair<Int, IntersectionResult>> {
    return hailstones.flatMapIndexed { i, a ->
        hailstones.drop(i + 1).map { b ->
            a.id to a.intersection2d(b)
        }
    }
}

fun addRelativeVelocity(hailstones: List<Hailstone>, velocity: Vector3L): List<Hailstone> {
    return hailstones.map { h ->
        h.copy(velocity = h.velocity + velocity)
    }
}

fun computeIntersectionError(intersections: List<Pair<Int, IntersectionResult>>): Double {

    val intersectionsById = intersections.groupBy { it.first }.mapValues { it.value.map { it.second } }

    val (inter, noIntersections) = intersections.map { it.second }.partition { it is IntersectionResult.Intersection }

    val globalScore = if (inter.size > 1) {
        val intersectionPoints = (inter as List<IntersectionResult.Intersection>).map { it.point }
        val average = intersectionPoints.reduce { x, y -> x + y } / intersections.size.toDouble()
        intersectionPoints.sumOf { (it - average).length() }
    } else {
        1e200
    }


    val localScore = intersectionsById.entries.sumOf {
        val (id, intersectionOfId) = it
        val (inter, noIntersections) = intersectionOfId.partition { it is IntersectionResult.Intersection }
        val multiplier = if (inter.size > 0) {
            1e-22
        } else {
            1.0
        }
        multiplier * noIntersections.size*noIntersections.size*noIntersections.size* 1e20+ noIntersections.sumOf { it.distance }
    }

    return globalScore + localScore
}

fun part3(input: List<String>): Int {
    val hailstones = parseInput(input)

    var maxScore = 0.0
    var bestVelocity = Vector3L(0, 0, 0)

    (-100..100L).forEach { vx ->
        (-100..100L).forEach { vy ->
            val currentVelocity = Vector3L(vx, vy, 0)
            val transformedStones = addRelativeVelocity(hailstones, currentVelocity)
            val intersections = computeAll3DIntersections(transformedStones)
            val intersectionCount = intersections.size
            val error = computeIntersectionError(intersections)
            val score = intersectionCount - error
            if (score > maxScore) {
                println(intersections)
                println(error)
                maxScore = score
                bestVelocity = currentVelocity
            }
        }
    }

    println(maxScore)
    println(bestVelocity)

    return input.size
}

fun determineVelocityAndIntersections(hailstones: List<Hailstone>, initialVelocity: Vector3L = Vector3L()): Pair<Vector3L, List<IntersectionResult>> {
    var currentVelocity = initialVelocity

    var done = false

    var searchRange = 1L
    var stepSize = 1L

    while (!done) {
//        val searchPoints = listOf(-10000*searchRange, -1000*searchRange, -100*searchRange, -10*searchRange, -searchRange, 0, 10*searchRange, searchRange, 100*searchRange, 1000*searchRange, 10000*searchRange)
        val searchPoints = listOf(-searchRange, 0, searchRange)
        val points = searchPoints.flatMap { vx ->
            searchPoints.flatMap { vy ->
                searchPoints.map { vz ->
                    Vector3L(vx, vy, vz)
                }
            }
        } + (1..64).flatMap {
            listOf(
                Vector3L(Random.nextLong(-2*searchRange, 2*searchRange), Random.nextLong(-2*searchRange, 2*searchRange), Random.nextLong(-2*searchRange, 2*searchRange)),
                Vector3L(Random.nextLong(-1000*searchRange, 1000*searchRange), Random.nextLong(-1000*searchRange, 1000*searchRange), Random.nextLong(-1000*searchRange, 1000*searchRange)),
            )
        }
        val gradient = points.parallelStream().map { dv ->
            val velocity = currentVelocity + dv
            val transformedStones = addRelativeVelocity(hailstones, -velocity)
            val intersections = computeAll3DIntersections(transformedStones)
            val error = computeIntersectionError(intersections)
            velocity to (error to intersections)
        }.toList()
        val nextVelocity = gradient.minBy { it.second.first }
        if (currentVelocity == nextVelocity.first) {
            if (nextVelocity.second.second.map { it.second }.count { it is IntersectionResult.NoIntersection } == 0) {
                done = true
            } else {
                searchRange += 1
                stepSize += 1
                println("$stepSize, $searchRange")
            }
        } else {
            searchRange = 1L
            stepSize = 1L
        }
        currentVelocity = nextVelocity.first
        val error = nextVelocity.second.first
        val (inter, no) = nextVelocity.second.second.partition { it.second is IntersectionResult.Intersection }
        println("$currentVelocity -> ($error, ${no.size}, ${inter.distinctBy { it.first }.size})")
    }

    val transformedStones = addRelativeVelocity(hailstones, -currentVelocity)
    val intersections = computeAll3DIntersections(transformedStones)

    return currentVelocity to intersections.map { it.second }
}

fun determineVelocityAndIntersections2d(hailstones: List<Hailstone>, initialVelocity: Vector3L = Vector3L()): Pair<Vector3L, List<IntersectionResult>> {
    var currentVelocity = initialVelocity

    var done = false

    while (!done) {
        val gradient = (-10..10L).flatMap { vx ->
            (-10..10L).map { vy ->
                val velocity = currentVelocity + Vector3L(vx, vy, 0)
                val transformedStones = addRelativeVelocity(hailstones, -velocity)
                val intersections = computeAll2DIntersections(transformedStones)
                val error = computeIntersectionError(intersections)
                velocity to error
            }
        }
        val nextVelocity = gradient.minBy { it.second }
        if (currentVelocity == nextVelocity.first) {
            done = true
        }
        currentVelocity = nextVelocity.first
        println("$currentVelocity -> ${nextVelocity.second}")
    }

    val transformedStones = addRelativeVelocity(hailstones, -currentVelocity)
    val intersections = computeAll3DIntersections(transformedStones)

    return currentVelocity to intersections.map { it.second }
}

fun part2(input: List<String>, initialVelocity: Vector3L = Vector3L()): Long {
    val hailstones = parseInput(input)

//    val (xyVelocity, xyInters) = determineVelocityAndIntersections2d(hailstones)

    val (velocity, inter) = determineVelocityAndIntersections(hailstones, initialVelocity)

//    println(xyVelocity)
    println(velocity)

    val inters = inter.filterIsInstance<IntersectionResult.Intersection>()
    val noIntersections = inter.filterIsInstance<IntersectionResult.NoIntersection>()

    println("${inters.size} intersections, ${noIntersections.size} no intersections")

    val p = inter.filterIsInstance<IntersectionResult.Intersection>().first()
    println(p)
    val roundedP = Vector3L(round(p.point.x).toLong(), round(p.point.y).toLong(), round(p.point.z).toLong())
    println(roundedP)
    return roundedP.x + roundedP.y + roundedP.z
}

fun main() = AoCTask("day24").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput, 7L..27L, 7L..27L), 2)
    check(part2(testInput), 47)

    val range = 200000000000000L..400000000000000L
    println(part1(input, range, range))
    println(part2(input))
}

fun hailPositionOfT(hailstone: Hailstone): (t: Long) -> Vector3L {
    return { t ->
        hailstone.position + hailstone.velocity * t
    }
}

fun main3() = AoCTask("day24").run {
    val hailstones = parseInput(input)
    // position ranges
    val xRange = hailstones.map { it.position.x..it.position.x }.reduce { a, b ->
        minOf(a.first, b.first)..maxOf(a.last, b.last)
    }
    val yRange = hailstones.map { it.position.y..it.position.y }.reduce { a, b ->
        minOf(a.first, b.first)..maxOf(a.last, b.last)
    }
    val zRange = hailstones.map { it.position.z..it.position.z }.reduce { a, b ->
        minOf(a.first, b.first)..maxOf(a.last, b.last)
    }
    println("x: $xRange -> ${xRange.last-xRange.first}")
    println("y: $yRange -> ${yRange.last-yRange.first}")
    println("z: $zRange -> ${zRange.last-zRange.first}")

    // velocity ranges
    val vxRange = hailstones.map { it.velocity.x..it.velocity.x }.reduce { a, b ->
        minOf(a.first, b.first)..maxOf(a.last, b.last)
    }
    val vyRange = hailstones.map { it.velocity.y..it.velocity.y }.reduce { a, b ->
        minOf(a.first, b.first)..maxOf(a.last, b.last)
    }
    val vzRange = hailstones.map { it.velocity.z..it.velocity.z }.reduce { a, b ->
        minOf(a.first, b.first)..maxOf(a.last, b.last)
    }
    println("vx: $vxRange -> ${vxRange.last-vxRange.first}")
    println("vy: $vyRange -> ${vyRange.last-vyRange.first}")
    println("vz: $vzRange -> ${vzRange.last-vzRange.first}")

    val (hail1, hail2) = hailstones.take(2)
    val h1 = hailPositionOfT(hail1)
    val h2 = hailPositionOfT(hail2)

    val p1 = h1(0)
    (1L..100000000L).asSequence().map() {t ->
        val p2 = h2(t)
        val v = (p1 - p2)
        Result(t, p1, p2, v)
    }.filter {
        val (t, _, _, v) = it
        v.x % t == 0L && v.y % t == 0L && v.z % t == 0L
    }.map {
        it.copy(v = it.v / it.t)
    }.forEach {
        with(it) {
            println("$t: $p1 -> $p2 @ $v")
        }
    }
}

data class Result(val t: Long, val p1: Vector3L, val p2: Vector3L, val v: Vector3L)

fun main4() = AoCTask("day24").run {
    val hailstones = parseInput(testInput)
    val hailstoneFunctions = hailstones.map { hailPositionOfT(it) }
    val t1Positions = hailstoneFunctions.map { it(1L) }
    val average = t1Positions.reduce { acc, pos -> acc + pos }.toVector3D() / t1Positions.size.toDouble()
    val closest = t1Positions.minBy { (it.toVector3D()-average).length() }
    println(closest)
}