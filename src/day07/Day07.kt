package day07

import AoCTask

// https://adventofcode.com/2023/day/7

const val JOKER = 'J'

/**
 * A list of all possible card values in a standard playing deck.
 */
val CARDS = listOf('2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A')

/**
 * Represents a playing card.
 *
 * @property value The value of the card.
 * @property jIsJoker Indicates if a value of J is a joker or a normal jack.
 * @property strength The strength of the card.
 */
data class Card(val value: Char, val jIsJoker: Boolean = false) {

    val strength: Int = if (jIsJoker && value == JOKER) -1 else CARDS.indexOf(value)
}

/**
 * Enum class representing different types of Poker hands.
 *
 * The hand types are:
 * - HIGH_CARD: A hand with no matching cards.
 * - ONE_PAIR: A hand with two cards of the same value.
 * - TWO_PAIR: A hand with two pairs of cards of the same value.
 * - THREE_OF_KIND: A hand with three cards of the same value.
 * - FULL_HOUSE: A hand with three of a kind and a pair.
 * - FOUR_OF_A_KIND: A hand with four cards of the same value.
 * - FIVE_OF_A_KIND: A hand with five cards of the same value.
 *
 * @see Hand
 */
enum class HandType {
    HIGH_CARD,
    ONE_PAIR,
    TWO_PAIR,
    THREE_OF_KIND,
    FULL_HOUSE,
    FOUR_OF_A_KIND,
    FIVE_OF_A_KIND
}

/**
 * Represents the count of a specific card in a hand.
 *
 * @property card The card.
 * @property count The count of the card.
 */
data class CardCount(val card: Card, val count: Int)

/**
 * Represents a hand of cards with a bid and an optional joker configuration.
 *
 * @property cards The list of cards in the hand.
 * @property bid The bid associated with the hand.
 * @property jIsJoker Indicates whether the Joker card is treated as a joker or a regular card.
 */
data class Hand(val cards: List<Card>, val bid: Int, val jIsJoker: Boolean = false): Comparable<Hand> {
    companion object {

        /**
         * Converts a string representation of a hand into a Hand object.
         *
         * @param s The string representation of the hand $cards $bid where $card=[2-9TJQKA]{5} and $bid=\d+.
         * @param jIsJoker Indicates whether the Joker card is treated as a joker or a regular card.
         * @return The Hand object representing the parsed hand.
         */
        fun fromString(s: String, jIsJoker: Boolean = false): Hand {
            val (cards, bid) = s.split(" ")
            return Hand(cards.map { Card(it, jIsJoker) }, bid.toInt(), jIsJoker)
        }
    }

    val type: HandType by lazy {
        if (jIsJoker) jokerType else actualType
    }

    val cardCounts = cards.groupBy { it }.toList().map { (card, cards) -> CardCount(card, cards.size) }

    val actualType: HandType by lazy {
        when {
            cardCounts.any { it.count == 5 } -> HandType.FIVE_OF_A_KIND
            cardCounts.any { it.count == 4 } -> HandType.FOUR_OF_A_KIND
            cardCounts.any { it.count == 3 } && cardCounts.any { it.count == 2 } -> HandType.FULL_HOUSE
            cardCounts.any { it.count == 3 } -> HandType.THREE_OF_KIND
            cardCounts.count { it.count == 2 } == 2 -> HandType.TWO_PAIR
            cardCounts.any { it.count == 2 } -> HandType.ONE_PAIR
            else -> HandType.HIGH_CARD
        }
    }

    fun containsAJoker(): Boolean = cards.any { it.jIsJoker }

    /**
     * Lazy-initialized variable representing the type of hand with the presence of a joker.
     * If the hand does not contain a joker, returns the actual hand type.
     * Otherwise, calculates the potential highest hand type considering the joker count and card counts.
     *
     * @return The type of hand with the presence of a joker.
     */
    val jokerType: HandType by lazy {
        if (!containsAJoker()) {
            actualType
        } else {
            val jokerCount = cards.count { it.value == JOKER }
            val cardCountsWithoutJoker = cardCounts.filter { it.card.value != JOKER }
            val highestCardCount = cardCountsWithoutJoker.maxOfOrNull { it.count } ?: 0
            val potentialHighestCount = highestCardCount + jokerCount
            when {
                potentialHighestCount == 5 -> HandType.FIVE_OF_A_KIND
                potentialHighestCount == 4 -> HandType.FOUR_OF_A_KIND
                cardCountsWithoutJoker.count { it.count == 2 } == 2 -> HandType.FULL_HOUSE
                cardCountsWithoutJoker.any { it.count == 2 } || cardCountsWithoutJoker.count { it.count == 1 } == 3 -> HandType.THREE_OF_KIND
                else -> HandType.ONE_PAIR
            }
        }
    }

    val strength: Int by lazy {
        type.ordinal
    }

    /**
     * Compares this hand with another hand based on their strength.
     *
     * @param other The other hand to compare with.
     * @return An integer representing the comparison result:
     *      - If this hand is stronger, returns a positive number.
     *      - If this hand is weaker, returns a negative number.
     *      - If this hand is equal in strength to the other hand, returns 0.
     */
    override operator fun compareTo(other: Hand): Int {
        val strengthComparison = strength.compareTo(other.strength)
        return if (strengthComparison == 0) {
            compareFirstDifferingCard(other)
        } else {
            strengthComparison
        }
    }

    /**
     * Compares the first differing card between this hand and another hand.
     *
     * @param other The other hand to compare with.
     * @return An integer representing the comparison result:
     *         - If the hands have a differing card, returns the result of comparing their strengths.
     *         - If there is no differing card, returns 0.
     */
    fun compareFirstDifferingCard(other: Hand): Int {
        val firstDifferingCard = cards.zip(other.cards).firstOrNull { it.first.value != it.second.value } ?: return 0
        return firstDifferingCard.first.strength.compareTo(firstDifferingCard.second.strength)
    }

    override fun toString(): String {
        val hand = cards.joinToString("") { it.value.toString() }
        return "$hand $bid ($actualType $jokerType $strength)"
    }
}

/**
 * Parses a list of strings representing hands of cards and converts them into a list of Hand objects.
 *
 * @param input The list of strings representing hands of cards.
 * @param jIsJoker Indicates whether the Joker card is treated as a joker or a regular card.
 *
 * @return The list of Hand objects parsed from the input strings.
 */
fun parseHands(input: List<String>, jIsJoker: Boolean): List<Hand> = input.map { Hand.fromString(it, jIsJoker) }

/**
 * Calculates the score of each hand in the given list based on their bids and their ranking.
 * The score of a hand is calculated as the product of its bid and its position in the sorted list of hands.
 *
 * @param hands The list of hands to score.
 *
 * @return The total score of all the hands.
 */
fun scoreHands(hands: List<Hand>): Int {
    val rankedHands = hands.sorted()
    return rankedHands.mapIndexed { index, hand ->
        hand.bid * (index + 1)
    }.sum()
}

fun part1(input: List<String>): Int {
    return scoreHands(parseHands(input, false))
}

fun part2(input: List<String>): Int {
    return scoreHands(parseHands(input, true))
}

fun main() = AoCTask("day07").run {
    // test if implementation meets criteria from the description, like:
    check(part1(testInput), 6440)
    check(part2(testInput), 5905)

    println(part1(input))
    println(part2(input))
}
