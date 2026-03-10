package com.example.blackjack

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import android.widget.FrameLayout
import androidx.core.view.isVisible
import com.example.blackjack.databinding.ActivityMainBinding
import com.example.blackjack.databinding.ItemCardBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val game = BlackjackGame()
    private var isDealing = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        startNewGame()
    }

    private fun setupListeners() {
        binding.btnHit.setOnClickListener { hitPlayer() }
        binding.btnCheck.setOnClickListener { check() }
        binding.btnNewGame.setOnClickListener { startNewGame() }
    }

    private fun startNewGame() {
        game.playerHand.clear()
        game.dealerHand.clear()
        binding.playerCardContainer.removeAllViews()
        binding.dealerCardContainer.removeAllViews()
        
        binding.tvGameStatus.text = "Your turn - HIT or CHECK"
        binding.tvDealerScore.visibility = View.INVISIBLE
        binding.tvPlayerScore.text = "0"
        
        binding.btnHit.isEnabled = false
        binding.btnCheck.isEnabled = false
        binding.btnNewGame.isVisible = false
        
        game.resetDeck()
        dealInitialCards()
    }

    private fun dealInitialCards() {
        isDealing = true
        dealToPlayer {
            dealToDealer(isHidden = false) {
                dealToPlayer {
                    dealToDealer(isHidden = true) {
                        isDealing = false
                        checkInitialNaturals()
                    }
                }
            }
        }
    }

    private fun checkInitialNaturals() {
        val playerNatural = game.isNatural(game.playerHand)
        val dealerNatural = game.isNatural(game.dealerHand)

        updateScores()

        if (playerNatural || dealerNatural) {
            revealDealerCard()
            if (playerNatural && dealerNatural) {
                endRound("Blackjack\nTie", "tie")
            } else if (playerNatural) {
                endRound("Blackjack\nYou Win", "win")
            } else {
                endRound("Blackjack\nDealer Wins", "loss")
            }
        } else {
            val pScore = game.calculateScore(game.playerHand)
            binding.btnHit.isEnabled = pScore < 18
            binding.btnCheck.isEnabled = true
        }
    }

    private fun dealToPlayer(onComplete: (() -> Unit)? = null) {
        val card = game.drawCard()
        game.playerHand.add(card)
        addCardToUi(card, binding.playerCardContainer, false, onComplete)
        updateScores()
    }

    private fun dealToDealer(isHidden: Boolean, onComplete: (() -> Unit)? = null) {
        val card = game.drawCard()
        game.dealerHand.add(card)
        addCardToUi(card, binding.dealerCardContainer, isHidden, onComplete)
        if (!isHidden) updateScores()
    }

    private fun addCardToUi(card: Card, container: FrameLayout, isHidden: Boolean, onComplete: (() -> Unit)?) {
        val cardBinding = ItemCardBinding.inflate(LayoutInflater.from(this), container, false)
        val cardView = cardBinding.root
        
        cardBinding.tvRankTop.text = card.rank.display
        cardBinding.tvRankBottom.text = card.rank.display
        cardBinding.tvSuitTop.text = card.suit.symbol
        cardBinding.tvSuitBottom.text = card.suit.symbol
        cardBinding.tvSuitCenter.text = card.suit.symbol

        val color = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color.parseColor("#FF0000") else Color.parseColor("#000000")
        cardBinding.tvRankTop.setTextColor(color)
        cardBinding.tvRankBottom.setTextColor(color)
        cardBinding.tvSuitTop.setTextColor(color)
        cardBinding.tvSuitBottom.setTextColor(color)
        cardBinding.tvSuitCenter.setTextColor(color)

        cardBinding.cardBack.isVisible = isHidden
        if (isHidden) {
            // Removed hardcoded background color to show the drawable pattern
            val text = cardBinding.tvSuitCenter
            text.text = "?"
            text.setTextColor(Color.parseColor("#4A627A"))
            text.textSize = 32f
        }

        container.addView(cardView)
        
        val count = container.childCount
        val offset = 72f * (count - 1)
        cardView.translationX = 1000f
        cardView.translationY = -1000f

        ObjectAnimator.ofFloat(cardView, "translationX", offset).apply {
            duration = 400
            start()
        }
        ObjectAnimator.ofFloat(cardView, "translationY", 0f).apply {
            duration = 400
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    onComplete?.invoke()
                }
            })
            start()
        }
    }

    private fun updateScores() {
        val pScore = game.calculateScore(game.playerHand)
        binding.tvPlayerScore.text = pScore.toString()
        
        if (binding.btnNewGame.isVisible) {
             binding.tvDealerScore.visibility = View.VISIBLE
             binding.tvDealerScore.text = game.calculateScore(game.dealerHand).toString()
        } else {
             if (game.dealerHand.isNotEmpty()) {
                 binding.tvDealerScore.visibility = View.VISIBLE
                 binding.tvDealerScore.text = "?"
             }
        }
        
        binding.btnHit.isEnabled = pScore < 18 && !isDealing && !binding.btnNewGame.isVisible
        binding.btnCheck.isEnabled = !isDealing && !binding.btnNewGame.isVisible
    }

    private fun hitPlayer() {
        isDealing = true
        binding.btnHit.isEnabled = false
        binding.btnCheck.isEnabled = false
        dealToPlayer {
            isDealing = false
            if (game.isFiveCardCharlie(game.playerHand)) {
                endRound("5-Card Charlie\nYou Win", "win")
            } else if (game.isBust(game.playerHand)) {
                endRound("Bust\nDealer Wins", "loss")
            } else {
                updateScores()
            }
        }
    }

    private fun check() {
        binding.btnHit.isEnabled = false
        binding.btnCheck.isEnabled = false
        revealDealerCard()
        
        Handler(Looper.getMainLooper()).postDelayed({
            dealerTurn()
        }, 600)
    }

    private fun revealDealerCard() {
        val dealerContainer = binding.dealerCardContainer
        if (dealerContainer.childCount >= 2) {
            val secondCardView = dealerContainer.getChildAt(1)
            secondCardView.findViewById<View>(R.id.card_back).isVisible = false
            val card = game.dealerHand[1]
            val tvSuitCenter = secondCardView.findViewById<android.widget.TextView>(R.id.tv_suit_center)
            tvSuitCenter.text = card.suit.symbol
            tvSuitCenter.textSize = 40f
            val color = if (card.suit == Suit.HEARTS || card.suit == Suit.DIAMONDS) Color.parseColor("#FF0000") else Color.parseColor("#000000")
            tvSuitCenter.setTextColor(color)
        }
        updateScores()
    }

    private fun dealerTurn() {
        val dScore = game.calculateScore(game.dealerHand)
        if (dScore < 17 && !game.isBust(game.dealerHand)) {
            dealToDealer(false) {
                Handler(Looper.getMainLooper()).postDelayed({
                    dealerTurn()
                }, 600)
            }
        } else {
            determineWinner()
        }
    }

    private fun determineWinner() {
        val pScore = game.calculateScore(game.playerHand)
        val dScore = game.calculateScore(game.dealerHand)

        when {
            game.isBust(game.dealerHand) -> endRound("Dealer Busts\nYou Win", "win")
            pScore > dScore -> endRound("You Win", "win")
            dScore > pScore -> endRound("Dealer Wins", "loss")
            else -> endRound("Tie", "tie")
        }
    }

    private fun endRound(message: String, result: String) {
        binding.tvGameStatus.text = message
        binding.btnHit.isEnabled = false
        binding.btnCheck.isEnabled = false
        binding.btnNewGame.isVisible = true
        
        when (result) {
            "win" -> game.wins++
            "loss" -> game.losses++
            "tie" -> game.ties++
        }
        
        val w = game.wins
        val l = game.losses
        val t = game.ties
        binding.tvWins.text = "W:$w"
        binding.tvLosses.text = "L:$l"
        binding.tvTies.text = "T:$t"
        updateScores()
    }
}
