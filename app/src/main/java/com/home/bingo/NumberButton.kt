package com.home.bingo

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatButton

class NumberButton @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    def: Int? = 0
) : AppCompatButton(context, attributeSet, def!!) {
    var number: Int = 0
    var pos: Int = 0
    var picked: Boolean = false
}