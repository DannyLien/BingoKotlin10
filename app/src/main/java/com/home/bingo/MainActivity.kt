package com.home.bingo

import android.content.Intent
import android.inputmethodservice.Keyboard.Row
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.IntList
import androidx.constraintlayout.widget.Group
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.snapshots
import com.google.firebase.database.values
import com.home.bingo.databinding.ActivityMainBinding
import com.home.bingo.databinding.RowRoomViewBinding
import kotlinx.coroutines.flow.count
import java.util.Arrays
import java.util.Objects

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, ValueEventListener,
    View.OnClickListener {
    private lateinit var binding: ActivityMainBinding
    private val TAG: String? = MainActivity::class.java.simpleName
    private lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var adapter: FirebaseRecyclerAdapter<Room, RoomHolder>
    private lateinit var avatar: ImageView
    private lateinit var groupAvatars: Group
    private lateinit var recy: RecyclerView
    private lateinit var tvNickname: TextView
    private lateinit var member: Member

    private val requestRC = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (it.resultCode == RESULT_OK) {
        }
    }

    val avatarIds = intArrayOf(
        R.drawable.avatar_0,
        R.drawable.avatar_1,
        R.drawable.avatar_2,
        R.drawable.avatar_3,
        R.drawable.avatar_4,
        R.drawable.avatar_5,
        R.drawable.avatar_6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        auth = FirebaseAuth.getInstance()
        findViews()
    }

    private fun findViews() {
        tvNickname = binding.tvNickname
        avatar = binding.avatar
        recy = binding.recyclerRoom
        groupAvatars = binding.groupAvatars
        groupAvatars.visibility = View.GONE

        binding.avatar0.setOnClickListener(this)
        binding.avatar1.setOnClickListener(this)
        binding.avatar2.setOnClickListener(this)
        binding.avatar3.setOnClickListener(this)
        binding.avatar4.setOnClickListener(this)
        binding.avatar5.setOnClickListener(this)
        binding.avatar6.setOnClickListener(this)

        recy.setHasFixedSize(true)
        recy.layoutManager = GridLayoutManager(this, 1)
        val query = FirebaseDatabase.getInstance().getReference("rooms")
            .limitToLast(30)
        val options = FirebaseRecyclerOptions.Builder<Room>()
            .setQuery(query, Room::class.java).build()
        adapter = object : FirebaseRecyclerAdapter<Room, RoomHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHolder {
                val v = RowRoomViewBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                return RoomHolder(v)
            }

            override fun onBindViewHolder(holder: RoomHolder, position: Int, model: Room) {
                holder.roomTitle.setText(model.title)
                holder.roomAvatar.setImageResource(avatarIds[model.init!!.avatarId])
                holder.itemView.setOnClickListener {
                    Intent(this@MainActivity, BingoActivity::class.java)
                        .apply {
                            putExtra("ROOM_ID", model.id)
                            putExtra("IS_CREATOR", false)
                        }.also {
                            startActivity(it)
                        }
                }
            }
        }
        recy.adapter = adapter

    }

    class RoomHolder(var view: RowRoomViewBinding) : RecyclerView.ViewHolder(view.root) {
        val roomTitle = view.roomTitle
        val roomAvatar = view.roomAvatar
    }

    fun setNickname(view: View) {
        user?.also {
            showNickDialog(it.uid, tvNickname.text.toString())
        }
    }

    fun setAvatar(view: View) {
        groupAvatars.visibility = if (groupAvatars.visibility == View.VISIBLE)
            View.GONE
        else
            View.VISIBLE
    }

    fun setFab(view: View) {
        Log.d(TAG, "setFab: bingo- setFab- ")
        var roomText = EditText(this)
        roomText.setText(" Welcome ")
        AlertDialog.Builder(this)
            .setTitle(" Room Info ")
            .setMessage(" Enter Room title ")
            .setView(roomText)
            .setPositiveButton("OK") { ok, which ->
                val room = Room(roomText.text.toString(), member)
                FirebaseDatabase.getInstance().getReference("rooms")
                    .push()
                    .setValue(room, object : DatabaseReference.CompletionListener {
                        override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
                            val roomId = ref.key
                            FirebaseDatabase.getInstance().getReference("rooms")
                                .child(roomId.toString()).child("id")
                                .setValue(roomId.toString())
                            Intent(this@MainActivity, BingoActivity::class.java)
                                .apply {
                                    putExtra("ROOM_ID", roomId)
                                    putExtra("IS_CREATOR", true)
                                }.also {
                                    startActivity(it)
                                }
                        }
                    })
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(this)
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(this)
        adapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_signout -> {
                auth.signOut()
                true
            }

            R.id.action_exit -> {
                finish()
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(fireAuth: FirebaseAuth) {
        user = fireAuth.currentUser
        user?.also {
            Log.d(TAG, "onAuthStateChanged: bingo- ")
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).child("displayName")
                .setValue(it.displayName)
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid).child("uid")
                .setValue(it.uid)
            FirebaseDatabase.getInstance().getReference("users")
                .child(it.uid)
                .addValueEventListener(this)
        } ?: signUp()
    }

    private fun signUp() {
        Log.d(TAG, "signUp: bingo- ")
        val signIN = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(
                Arrays.asList(
                    EmailBuilder().build(),
                    GoogleBuilder().build()
                )
            )
            .setIsSmartLockEnabled(false)
            .build()
        requestRC.launch(signIN)
    }

    override fun onDataChange(snapshot: DataSnapshot) {
        member = snapshot.getValue(Member::class.java)!!
//        Log.d(TAG, "onDataChange: bingo- member- ${member.uid} , ${member.displayName}")
        member.nickname?.let {
            tvNickname.setText(member.nickname)
        } ?: showNickDialog(user!!.uid, user?.displayName!!)
        avatar.setImageResource(avatarIds[member.avatarId])
    }

    override fun onCancelled(error: DatabaseError) {

    }

    private fun showNickDialog(uid: String, name: String) {
        val nickText = EditText(this)
        nickText.setText(name)
        AlertDialog.Builder(this)
            .setTitle(" Nick Info ")
            .setMessage(" Enter Nick name ")
            .setView(nickText)
            .setPositiveButton("OK") { ok, which ->
                FirebaseDatabase.getInstance().getReference("users")
                    .child(uid).child("nickname")
                    .setValue(nickText.text.toString())
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onClick(v: View?) {
        val itemId = v?.id
        val selectId = when (itemId) {
            R.id.avatar_0 -> 0
            R.id.avatar_1 -> 1
            R.id.avatar_2 -> 2
            R.id.avatar_3 -> 3
            R.id.avatar_4 -> 4
            R.id.avatar_5 -> 5
            R.id.avatar_6 -> 6
            else -> 0
        }
        groupAvatars.visibility = View.GONE
        FirebaseDatabase.getInstance().getReference("users")
            .child(user!!.uid).child("avatarId")
            .setValue(selectId)
    }

}




















