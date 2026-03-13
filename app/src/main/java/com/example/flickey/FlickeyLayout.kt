package com.example.flickey

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout

class FlickeyLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var keyboardState = KeyboardState()
    private var actionListener: FlickeyActionListener? = null
    private val autocorrectManager = AutocorrectManager(context)
    private var currentWord = StringBuilder()

    // UI Components
    private val btnLayerToggle: Button
    private val btnLayerCustom: Button
    private val btnLayerSpecial: Button
    private val btnCursorLeft: Button
    private val btnCursorRight: Button
    
    private val knobConsonant: FlickKnob
    private val knobVowel: FlickKnob
    private val knobPunctuation: FlickKnob
    private val keyboardRoot: LinearLayout
    
    private val btnSuggestion1: Button
    private val btnSuggestion2: Button
    private val btnSuggestion3: Button

    init {
        LayoutInflater.from(context).inflate(R.layout.layout_flickey, this, true)
        
        keyboardRoot = findViewById(R.id.keyboard_root)
        btnLayerToggle = findViewById(R.id.btn_layer_toggle)
        btnLayerCustom = findViewById(R.id.btn_layer_custom)
        btnLayerSpecial = findViewById(R.id.btn_layer_special)
        btnCursorLeft = findViewById(R.id.btn_cursor_left)
        btnCursorRight = findViewById(R.id.btn_cursor_right)
        
        knobConsonant = findViewById(R.id.knob_consonant)
        knobVowel = findViewById(R.id.knob_vowel)
        knobPunctuation = findViewById(R.id.knob_punctuation)
        
        btnSuggestion1 = findViewById(R.id.btn_suggestion_1)
        btnSuggestion2 = findViewById(R.id.btn_suggestion_2)
        btnSuggestion3 = findViewById(R.id.btn_suggestion_3)

        setupListeners()
        updateUI()
    }

    fun setActionListener(listener: FlickeyActionListener) {
        this.actionListener = listener
    }

    private fun setupListeners() {
        // State Change Listener
        keyboardState.onStateChanged = {
            updateUI()
        }

        // Buttons
        btnLayerToggle.setOnClickListener {
            keyboardState.toggleLayer()
        }
        btnLayerSpecial.setOnClickListener {
            keyboardState.setSpecialLayer()
        }
        
        // Cursor
        btnCursorLeft.setOnClickListener { actionListener?.onCursorLeft() }
        btnCursorRight.setOnClickListener { actionListener?.onCursorRight() }

        // Suggestions
        val onSuggestionClick: (String) -> Unit = { suggestion ->
            if (suggestion.isNotEmpty() && currentWord.isNotEmpty()) {
                // Delete current word
                for (i in 0 until currentWord.length) {
                    actionListener?.onDelete()
                }
                // Input suggested word with a space
                applyInput(suggestion + " ")
            }
        }
        btnSuggestion1.setOnClickListener { onSuggestionClick(btnSuggestion1.text.toString()) }
        btnSuggestion2.setOnClickListener { onSuggestionClick(btnSuggestion2.text.toString()) }
        btnSuggestion3.setOnClickListener { onSuggestionClick(btnSuggestion3.text.toString()) }

        // Knobs - Flicks
        knobConsonant.onFlick = { text -> applyInput(text) }
        knobVowel.onFlick = { text -> applyInput(text) }
        knobPunctuation.onFlick = { text -> handlePunctuation(text) }

        // Knobs - Taps
        knobConsonant.onTap = {
            keyboardState.toggleConsonantPage()
        }
        knobVowel.onTap = {
            keyboardState.toggleVowelPage()
        }
        knobPunctuation.onTap = {
            // User requested: One tap on punctuation = Backspace or Delete
            applyDelete()
        }
    }

    private fun applyInput(text: String?) {
        if (text == null) return
        actionListener?.onTextInput(text)
        
        // Update current word buffer (basic logic: assumes simple alphabetical appending)
        // Reset if space or punctuation
        if (text == " " || text == "\n" || !text.all { it.isLetter() }) {
            currentWord.clear()
        } else {
            currentWord.append(text)
        }
        updateSuggestions()
    }

    private fun applyDelete() {
        actionListener?.onDelete()
        if (currentWord.isNotEmpty()) {
            currentWord.deleteCharAt(currentWord.length - 1)
        }
        updateSuggestions()
    }

    private fun updateSuggestions() {
        val word = currentWord.toString().trim()
        if (word.isEmpty() || keyboardState.currentLayer != Layer.EN) {
            btnSuggestion1.text = ""
            btnSuggestion2.text = ""
            btnSuggestion3.text = ""
            return
        }

        val suggestions = autocorrectManager.getSuggestions(word)
        btnSuggestion1.text = suggestions.getOrNull(0) ?: ""
        btnSuggestion2.text = suggestions.getOrNull(1) ?: ""
        btnSuggestion3.text = suggestions.getOrNull(2) ?: ""
    }

    private fun handlePunctuation(text: String) {
        when (text) {
            "SPACE" -> applyInput(" ")
            "ENTER" -> { actionListener?.onEnter(); currentWord.clear(); updateSuggestions() }
            "BACKSPACE" -> applyDelete()
            "SHIFT" -> actionListener?.onShift()
            else -> applyInput(text)
        }
    }

    private fun updateUI() {
        // Update Knobs
        knobConsonant.setConfig(keyboardState.getConsonantConfig())
        knobVowel.setConfig(keyboardState.getVowelConfig())
        knobPunctuation.setConfig(KeyMappings.PUNCTUATION)

        // Update Colors based on Layer
        val bgColor = when (keyboardState.currentLayer) {
            Layer.KR -> Color.parseColor("#E0F7FA") // Light Cyan
            Layer.EN -> Color.parseColor("#F3E5F5") // Light Purple
            Layer.SP -> Color.parseColor("#FFF3E0") // Light Orange
        }
        keyboardRoot.setBackgroundColor(bgColor)

        // Update Button Texts
        btnLayerToggle.text = if (keyboardState.currentLayer == Layer.KR) "한/영 (KR)" else "한/영 (EN)"
    }
}
