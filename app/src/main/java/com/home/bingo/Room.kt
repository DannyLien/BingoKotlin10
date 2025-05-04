package com.home.bingo

class Room(
    var id: String,
    var title: String,
    var status: Int,
    var init: Member?,
    var join: Member?
) {
    companion object {
        val STATUS_INIT = 0
        val STATUS_CREATOR = 1
        val STATUS_JOINT = 2
        val STATUS_CREATED_TURN = 3
        val STATUS_JOINTED_TURN = 4
        val STATUS_CREATED_BINGO = 5
        val STATUS_JOINTED_BINGO = 6
    }

    constructor() : this("", "Welcome", 0, null, null)
    constructor(title: String, init: Member?) : this("", title, 0, init, null)
}













