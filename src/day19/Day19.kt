package day19

import AoCTask
import split
import times

// https://adventofcode.com/2023/day/19

data class Item(val x: Int, val m: Int, val a: Int, val s: Int) {
    fun getProperty(key: String) = when (key) {
        "x" -> x
        "m" -> m
        "a" -> a
        "s" -> s
        else -> throw IllegalArgumentException()
    }
}

data class SetItem(val x: Set<Int>, val m: Set<Int>, val a: Set<Int>, val s: Set<Int>) {
    fun getProperty(key: String) = when (key) {
        "x" -> x
        "m" -> m
        "a" -> a
        "s" -> s
        else -> throw IllegalArgumentException()
    }

    fun copyWith(key: String, values: Set<Int>) = when (key) {
        "x" -> copy(x = values)
        "m" -> copy(m = values)
        "a" -> copy(a = values)
        "s" -> copy(s = values)
        else -> throw IllegalArgumentException()
    }

    fun isNotEmpty() = x.isNotEmpty() || m.isNotEmpty() || a.isNotEmpty() || s.isNotEmpty()
    fun combinations(): Long {
        return x.size.toLong() * m.size.toLong() * a.size.toLong() * s.size.toLong()
    }
}

sealed class Result {
    data object Accepted : Result()
    data object Rejected : Result()
    data class Delegated(val workflow: String) : Result()
}

data class Resolve(val result: Result, val affected: SetItem, val remaining: SetItem)

sealed class Rule {
    abstract fun resolve(item: Item): Result?

    abstract fun resolve(setItem: SetItem): Resolve

    data class LessThan(val threshold: Int, val propertyKey: String, val result: Result) : Rule() {
        override fun resolve(item: Item): Result? {
            return if (item.getProperty(propertyKey) < threshold) result else null
        }

        override fun resolve(setItem: SetItem): Resolve {
            val set = setItem.getProperty(propertyKey)
            val (affected, remaining) = set.partition { it < threshold }
            return Resolve(result, setItem.copyWith(propertyKey, affected.toSet()), setItem.copyWith(propertyKey, remaining.toSet()))
        }
    }

    data class GreaterThan(val threshold: Int, val propertyKey: String, val result: Result) : Rule() {
        override fun resolve(item: Item): Result? {
            return if (item.getProperty(propertyKey) > threshold) result else null
        }

        override fun resolve(setItem: SetItem): Resolve {
            val set = setItem.getProperty(propertyKey)
            val (affected, remaining) = set.partition { it > threshold }
            return Resolve(result, setItem.copyWith(propertyKey, affected.toSet()), setItem.copyWith(propertyKey, remaining.toSet()))
        }
    }

    data class Unconditionally(val result: Result) : Rule() {
        override fun resolve(item: Item): Result {
            return result
        }

        override fun resolve(setItem: SetItem): Resolve {
            return Resolve(result, setItem, SetItem(emptySet(), emptySet(), emptySet(), emptySet()))
        }
    }
}

data class Workflow(val name: String, val rules: List<Rule>)

fun propertyKeyToAccessor(propertyKey: String): (item: Item) -> Int {
    return when (propertyKey) {
        "x" -> { item -> item.x }
        "m" -> { item -> item.m }
        "a" -> { item -> item.a }
        "s" -> { item -> item.s }
        else -> throw IllegalArgumentException("Unknown property key: $propertyKey")
    }
}

fun resultStringToResult(resultString: String): Result {
    return when (resultString) {
        "A" -> Result.Accepted
        "R" -> Result.Rejected
        else -> Result.Delegated(resultString)
    }
}

fun parseInput(input: List<String>): Pair<Map<String, Workflow>, List<Item>> {
    val (workflowRows, itemRows) = input.split {
        it.isEmpty()
    }
    val workflows = workflowRows.map { row ->
        val (name, ruleString) = row.trim('}').split("{")
        val rules = ruleString.split(",").map { rule ->
            if (":" in rule) {
                val (condition, resultString) = rule.split(":")
                val (property, threshold) = condition.split("[<>]".toRegex())
                val result = resultStringToResult(resultString)
                if ("<" in condition) {
                    Rule.LessThan(threshold.toInt(), property, result)
                } else {
                    Rule.GreaterThan(threshold.toInt(), property, result)
                }
            } else {
                Rule.Unconditionally(resultStringToResult(rule))
            }
        }
        Workflow(name, rules)
    }
    val items = itemRows.map { row ->
        val (x, m, a, s) = row.trim('{', '}').split(",").map { it.split("=").last().toInt() }
        Item(x, m, a, s)
    }
    return Pair(workflows.associateBy { it.name }, items)
}


fun part1(input: List<String>): Int {
    val (workflows, items) = parseInput(input)
    val inWorkflow = workflows["in"]!!
    val acceptedItems = items.filter { item ->
        var currentWorkflow: Workflow? = inWorkflow
        var accepted = false
        while (currentWorkflow != null) {
            val result = currentWorkflow.rules.firstNotNullOf { rule -> rule.resolve(item) }
            when (result) {
                Result.Accepted -> {
                    accepted = true
                    break
                }

                Result.Rejected -> {
                    accepted = false
                    break
                }

                is Result.Delegated -> currentWorkflow = workflows[result.workflow]
            }
        }
        accepted
    }
    return acceptedItems.sumOf {
        it.x + it.m + it.a + it.s
    }
}

fun part2(input: List<String>): Long {
    val (workflows, _) = parseInput(input)
    var openList = listOf<Pair<Workflow, SetItem>>(
        workflows["in"]!! to SetItem(
            x=(1..4000).toSet(),
            m=(1..4000).toSet(),
            a=(1..4000).toSet(),
            s=(1..4000).toSet(),
        )
    )
    val resolvedList = mutableListOf<SetItem>()
    while (openList.isNotEmpty()) {
        openList = openList.flatMap { (workflow, item) ->
            val next = mutableListOf<Pair<Workflow, SetItem>>()
            workflow.rules.fold(item) { it, rule ->
                val resolve = rule.resolve(it)
                if (resolve.affected.isNotEmpty()) {
                    when (resolve.result) {
                        is Result.Accepted -> {
                            resolvedList.add(resolve.affected)
                        }
                        is Result.Delegated -> {
                            next.add(workflows[resolve.result.workflow]!! to resolve.affected)
                        }
                        else -> {}
                    }
                }
                resolve.remaining
            }
            next
        }
    }
    return resolvedList.sumOf {
        it.combinations()
    }
}

fun main() = AoCTask("day19").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 19114)
    check(part2(testInput), 167409079868000)

    println(part1(input))
    println(part2(input))
}
