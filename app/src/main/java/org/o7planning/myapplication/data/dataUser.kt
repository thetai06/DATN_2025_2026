package org.o7planning.myapplication.data

class dataUser(
    var id:String? = null,
    var phoneNumber:String? = null,
    var name:String? = null,
    var email:String? = null,
    var dateBirth:String? = null,
    val role:String? = null
) {
    constructor():this(null,null,null)
}