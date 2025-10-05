package org.o7planning.myapplication.data

class dataUser(
    var id:String? = null,
    var numberPhone:String? = null,
    var name:String? = null,
    var email:String? = null,
    val role:String? = null
) {
    constructor():this(null,null,null)
}