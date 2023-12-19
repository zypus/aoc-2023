package day19

import AoCTask
import split

// https://adventofcode.com/2023/day/19

data class Item(val x: Int, val m: Int, val a: Int, val s: Int)

sealed class Result {
    data object Accepted: Result()
    data object Rejected: Result()
    data class Delegated(val workflow: String): Result()
}

sealed class Rule {
    abstract fun resolve(item: Item): Result?

    data class LessThan(val threshold: Int, val propertyAccessor: (item: Item) -> Int, val result: Result): Rule() {
        override fun resolve(item: Item): Result? {
            return if (propertyAccessor(item) < threshold) result else null
        }
    }

    data class GreaterThan(val threshold: Int, val propertyAccessor: (item: Item) -> Int, val result: Result): Rule() {
        override fun resolve(item: Item): Result? {
            return if (propertyAccessor(item) > threshold) result else null
        }
    }

    data class Unconditionally(val result: Result): Rule() {
        override fun resolve(item: Item): Result {
            return result
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
                val accessor = propertyKeyToAccessor(property)
                val result = resultStringToResult(resultString)
                if ("<" in condition) {
                    Rule.LessThan(threshold.toInt(), accessor, result)
                } else {
                    Rule.GreaterThan(threshold.toInt(), accessor, result)
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
        while (currentWorkflow!= null) {
            val result = currentWorkflow.rules.firstNotNullOf { rule -> rule.resolve(item) }
            when(result) {
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

fun part2(input: List<String>): Int {
    return input.size
}

fun main() = AoCTask("day19").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 19114)
    //check(part2(testInput), 1)

    println(part1(input))
    println(part2(input))
}
