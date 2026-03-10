package com.example.blackjack

class BlackjackGame {
    private var deck = mutableListOf<Card>()
    val playerHand = mutableListOf<Card>()
    val dealerHand = mutableListOf<Card>()

    var wins = 0
    var losses = 0
    var ties = 0

    init {
        resetDeck()
    }

    fun resetDeck() {
        deck.clear()
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                deck.add(Card(rank, suit))
            }
        }
        deck.shuffle()
    }

    fun drawCard(): Card {
        if (deck.isEmpty()) resetDeck()
        return deck.removeAt(0)
    }

    fun calculateScore(hand: List<Card>): Int {
        var score = 0
        var aces = 0

        for (card in hand) {
            if (card.rank == Rank.ACE) {
                aces++
                score += 11
            } else {
                score += card.rank.value
            }
        }

        while (score > 21 && aces > 0) {
            score -= 10
            aces--
        }

        return score
    }

    fun isBust(hand: List<Card>): Boolean {
        return calculateScore(hand) > 21
    }

    fun isNatural(hand: List<Card>): Boolean {
        if (hand.size != 2) return false
        return calculateScore(hand) == 21
    }

    fun isFiveCardCharlie(hand: List<Card>): Boolean {
        return hand.size == 5 && !isBust(hand)
    }
}
