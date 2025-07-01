package com.home.bingo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.common.ChangeEventType
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.home.bingo.databinding.ActivityBingoBinding
import com.home.bingo.databinding.SingleButtonBinding

class BingoActivity : AppCompatActivity() {
    private lateinit var tvInfo: TextView
    private lateinit var recy: RecyclerView
    private lateinit var adapter: FirebaseRecyclerAdapter<Boolean, NumberHolder>
    private var NUMBER_COUNT: Int = 25
    private var creator: Boolean = false
    private var myTurn: Boolean = false
        set(value) {
            field = value
            tvInfo.setText(if (value) "請選號" else "等對手選號")
        }
    private var roomId: String? = null
    private val TAG: String? = BingoActivity::class.java.simpleName
    private lateinit var binding: ActivityBingoBinding
    private lateinit var randomNumbers: MutableList<Int>
    private lateinit var buttons: MutableList<NumberButton>
    val numberMap = mutableMapOf<Int, NumberButton>()

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

        if (creator) {
            for (i in 0 until NUMBER_COUNT) {
                FirebaseDatabase.getInstance().getReference("rooms")
                    .child(roomId.toString()).child("numbers").child((i + 1).toString())
                    .setValue(false)
            }
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId.toString()).child("status")
                .setValue(Room.STATUS_CREATOR)
        } else {
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId.toString()).child("status")
                .setValue(Room.STATUS_JOINT)
        }
        generateRandomNumber()
        findViews()
    }

    private fun generateRandomNumber() {
//        var randomNumbers: MutableList<Int>
        randomNumbers = mutableListOf<Int>()
        for (i in 0 until NUMBER_COUNT) {
            randomNumbers.add(i + 1)
        }
        randomNumbers.shuffle()
//        var buttons: MutableList<NumberButton>
        buttons = mutableListOf<NumberButton>()
        for (i in 0 until NUMBER_COUNT) {
            val button = NumberButton(this)
            button.apply {
                text = randomNumbers.get(i).toString()
                number = randomNumbers.get(i)
                pos = i
            }
            buttons.add(button)
//            val numberMap = mutableMapOf<Int, NumberButton>()
            numberMap.put(button.number, button)
        }
    }

    private fun findViews() {
        tvInfo = binding.tvInfo
        recy = binding.recyclerBingo
        recy.setHasFixedSize(true)
        recy.layoutManager = GridLayoutManager(this, 5)
        val query = FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomId.toString()).child("numbers").orderByKey()
        val options = FirebaseRecyclerOptions.Builder<Boolean>()
            .setQuery(query, Boolean::class.java).build()
        adapter = object : FirebaseRecyclerAdapter<Boolean, NumberHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NumberHolder {
                val v = SingleButtonBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return NumberHolder(v)
            }

            override fun onChildChanged(
                type: ChangeEventType,
                snapshot: DataSnapshot,
                newIndex: Int,
                oldIndex: Int
            ) {
                super.onChildChanged(type, snapshot, newIndex, oldIndex)
                if (type == ChangeEventType.CHANGED) {
                    val snapshotKey = snapshot.key?.toInt()
                    val pos = numberMap.get(snapshotKey)?.pos
                    val holder = recy.findViewHolderForAdapterPosition(pos!!) as? NumberHolder
                    holder!!.buttonView.isEnabled = false
                    numberMap.get(snapshotKey)?.picked = true
                    if (myTurn) {
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId.toString()).child("status")
                            .setValue(if (creator) Room.STATUS_JOINTED_TURN else Room.STATUS_CREATED_TURN)
                    }
                    var bingo = 0
                    var sum = 0
                    val nums = IntArray(NUMBER_COUNT)
                    for (i in 0 until NUMBER_COUNT) {
                        nums[i] = if (buttons.get(i).picked) 1 else 0
                    }
                    for (i in 0 until 5) {
                        sum = 0
                        for (j in 0 until 5) {
                            sum += nums[i * 5 + j]
                        }
                        bingo += if (sum == 5) 1 else 0
                        sum = 0
                        for (j in 0 until 5) {
                            sum += nums[j * 5 + i]
                        }
                        bingo += if (sum == 5) 1 else 0
                    }
                    sum = 0
                    for (i in 0 until 5) {
                        sum += nums[i * 6]
                    }
                    bingo += if (sum == 5) 1 else 0
                    sum = 0
                    for (i in 0 until 5) {
                        sum += nums[20 - i * 4]
                    }
                    bingo += if (sum == 5) 1 else 0
                    if (bingo > 0) {
                        Log.d(TAG, "onChildChanged: bingo- ${bingo}")
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId.toString()).child("status")
                            .setValue(if (creator) Room.STATUS_CREATED_BINGO else Room.STATUS_JOINTED_BINGO)
                        AlertDialog.Builder(this@BingoActivity)
                            .setTitle(" Game Info ")
                            .setMessage(" Bingo You Win ")
                            .setIcon(R.drawable.yahoo)
                            .setPositiveButton("OK") { ok, which ->
                                endGame()
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }

            override fun onBindViewHolder(holder: NumberHolder, position: Int, model: Boolean) {
                holder.buttonView.text = buttons.get(position).number.toString()
                holder.buttonView.isEnabled = !buttons.get(position).picked
                holder.itemView.setOnClickListener {
                    if (myTurn) {
                        val number = buttons.get(position).number
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId.toString()).child("numbers").child(number.toString())
                            .setValue(true)
                    }
                }
            }
        }
        recy.adapter = adapter
    }

    class NumberHolder(var view: SingleButtonBinding) : RecyclerView.ViewHolder(view.root) {
        val buttonView = view.buttonView
    }

    private fun endGame() {
        FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomId.toString()).child("status")
            .removeEventListener(statusListener)
        if (creator) {
            FirebaseDatabase.getInstance().getReference("rooms")
                .child(roomId.toString()).removeValue()
        }
        finish()
    }

    override fun onStart() {
        super.onStart()
        adapter.startListening()
        FirebaseDatabase.getInstance().getReference("rooms")
            .child(roomId.toString()).child("status")
            .addValueEventListener(statusListener)
    }

    override fun onStop() {
        super.onStop()
        adapter.stopListening()
    }

    val statusListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            if (snapshot.getValue() != null) {
                val status = snapshot.value as Long
                when (status.toInt()) {
                    Room.STATUS_INIT -> ""
                    Room.STATUS_CREATOR -> {
                        tvInfo.text = "等對手加入"
                    }

                    Room.STATUS_JOINT -> {
                        tvInfo.text = "對手已加入"
                        FirebaseDatabase.getInstance().getReference("rooms")
                            .child(roomId.toString()).child("status")
                            .setValue(Room.STATUS_CREATED_TURN)
                    }

                    Room.STATUS_CREATED_TURN -> {
                        myTurn = creator
                    }

                    Room.STATUS_JOINTED_TURN -> {
                        myTurn = !creator
                    }

                    Room.STATUS_CREATED_BINGO -> {
                        if (!creator) {
                            AlertDialog.Builder(this@BingoActivity)
                                .setTitle(" Game Info ")
                                .setMessage(" You Loss ")
                                .setIcon(R.drawable.crying)
                                .setPositiveButton("OK") { ok, which ->
                                    endGame()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }

                    Room.STATUS_JOINTED_BINGO -> {
                        if (creator) {
                            AlertDialog.Builder(this@BingoActivity)
                                .setTitle(" Game Info ")
                                .setMessage(" You Loss ")
                                .setIcon(R.drawable.crying)
                                .setPositiveButton("OK") { ok, which ->
                                    endGame()
                                }
                                .setNegativeButton("Cancel", null)
                                .show()
                        }
                    }
                }
            }
        }

        override fun onCancelled(error: DatabaseError) {

        }
    }


}





















