package com.example.flickey

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.inputmethod.SurroundingText
import android.widget.Button
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class FlickeyLayout(context: Context, attrs: AttributeSet?) : LinearLayout(context, attrs) {

    private var keyboardState = KeyboardState()
    private var actionListener: FlickeyActionListener? = null
    private val autocorrectManager = AutocorrectManager(context)
    private val NextWordManager = NextWordManager(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var predictJob: Job? = null

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

    var surroundingCursorTextProvider: ((Int, Int) -> SurroundingText?)? = null
    var beforeCursorTextProvider: ((Int) -> String)? = null

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

    private fun getSurroundingCursorText(n: Int, m: Int): SurroundingText? {
        return surroundingCursorTextProvider?.invoke(n, m)
    }

    private fun getBeforeCursorText(n: Int): String? {
        return beforeCursorTextProvider?.invoke(n)
    }
    private fun isWordChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || c == '\''
    }
    private fun getCurrentWord(): String {
        val surrounding = getSurroundingCursorText(20, 20) ?: return ""

        val text = surrounding.text.toString()
        if (text.isEmpty()) return ""

        // selectionStart == selectionEnd 이면 보통 커서 위치
        val cursor = surrounding.selectionStart.coerceIn(0, text.length)

        var start = cursor
        while (start > 0 && isWordChar(text[start - 1])) {
            start--
        }

        var end = cursor
        while (end < text.length && isWordChar(text[end])) {
            end++
        }

        return text.substring(start, end)
    }

    private fun getLeftWords(leftwords: Int = 3, lookback: Int = 50): List<String> {
        val text = getBeforeCursorText(lookback) ?: return emptyList()

        val result = mutableListOf<String>()
        var i = text.length - 1

        while (i >= 0 && result.size < leftwords) {
            // 1) 뒤쪽 공백/구두점 스킵
            while (i >= 0 && !isWordChar(text[i])) {
                i--
            }

            if (i < 0) break

            // 2) 단어 끝
            val end = i + 1

            // 3) 단어 시작까지 이동
            while (i >= 0 && isWordChar(text[i])) {
                i--
            }

            val start = i + 1
            val word = text.substring(start, end)

            if (word.isNotBlank()) {
                result.add(word)
            }
        }

        return result.reversed()
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
            if (suggestion.isNotEmpty()) {
                // Delete current word
                actionListener?.onDeleteCurrentWord()
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
        // results in bug when moving cursors so removed
        updateSuggestions()
    }

    private fun applyDelete() {
        actionListener?.onDelete()
        updateSuggestions()
    }

    private fun updateSuggestions() {
        fun clearSuggestions(){
            btnSuggestion1.text = ""
            btnSuggestion2.text = ""
            btnSuggestion3.text = ""
        }
        if (keyboardState.currentLayer != Layer.EN){
            clearSuggestions()
            return
        }
        val word = getCurrentWord()

        if (word.isEmpty()){
            val leftwords = getLeftWords()
            if (leftwords.isEmpty()){
                clearSuggestions()
                return
            }
            predictJob?.cancel()
            predictJob = scope.launch {
                val suggestions = NextWordManager.getSuggestions(getBeforeCursorText(200)?:"")
                btnSuggestion1.text = suggestions.getOrNull(0) ?: ""
                btnSuggestion2.text = suggestions.getOrNull(1) ?: ""
                btnSuggestion3.text = suggestions.getOrNull(2) ?: ""
            }
        } else {
            val suggestions = autocorrectManager.getSuggestions(word)
            btnSuggestion1.text = suggestions.getOrNull(0) ?: ""
            btnSuggestion2.text = suggestions.getOrNull(1) ?: ""
            btnSuggestion3.text = suggestions.getOrNull(2) ?: ""
        }
    }

    private fun handlePunctuation(text: String) {
        when (text) {
            "SPACE" -> applyInput(" ")
            "ENTER" -> { actionListener?.onEnter(); updateSuggestions() }
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
