package com.home.bingo

import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.home.bingo.databinding.ActivityBingoBinding

class BingoActivity : AppCompatActivity() {
    private var creator: Boolean = false
    private var roomId: String? = null
    private val TAG: String? = BingoActivity::class.java.simpleName
    private lateinit var binding: ActivityBingoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBingoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        roomId = intent.getStringExtra("ROOM_ID")
        creator = intent.getBooleanExtra("IS_CREATOR", false)
        Log.d(TAG, "onCreate: bingo- intent- ${roomId}  , ${creator}")
    }


}