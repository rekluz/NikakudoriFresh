/*
 * Copyright (c) 2026 Rekluz Games. All rights reserved.
 */

package com.rekluzgames.nikakudorimahjong.presentation.quote

object QuoteProvider {

    private val masterList = listOf(
        "Fall seven times, stand up eight.",
        "Even a dust heap yields a mountain.",
        "The bamboo that bends is stronger than the oak that resists.",
        "Beginnings are easy; continuing is hard.",
        "Wisdom and virtue are like the two wheels of a cart.",
        "One kind word can warm three winter months.",
        "The water that has flowed past will not turn the wheel again.",
        "A single arrow is easily broken, but not ten in a bundle.",
        "He who climbs the mountain must start at the bottom.",
        "Silence is a flower.",
        "Studying the old is the key to understanding the new.",
        "A journey of a thousand miles begins with a single step.",
        "The reverse side also has a reverse side.",
        "Vision without action is a daydream.",
        "The frog in the well knows nothing of the great ocean.",
        "Unless you enter the tiger's den, you cannot take the cubs.",
        "Ten men, ten colors.",
        "Experience the world and you will gain wisdom.",
        "Even a stone can become warm if sat on for three years.",
        "Gold is tested by fire; man by his words.",
        "Better to be the beak of a rooster than the tail of an ox.",
        "A bird does not foul its own nest.",
        "Don't give a gold coin to a cat.",
        "Waking up is the first step to progress.",
        "Persistence is the shortcut to success.",
        "The sun does not know who is right or wrong.",
        "If you do not enter the fire, you cannot find the gold.",
        "Every day is a good day.",
        "Be like the flower that gives its fragrance to the hand that crushes it.",
        "The willow does not break under the weight of the snow.",
        "Time flies like an arrow.",
        "It takes three years even to heat a stone.",
        "Even dust, accumulated, becomes a mountain.",
        "You don't feed a fish you've already caught.",
        "A kite does not give birth to a falcon.",
        "Start with politeness, end with politeness.",
        "Exhaust all human affairs and wait on heaven's decree."
    )

    fun getShuffledQuotes(): MutableList<String> = masterList.shuffled().toMutableList()
}