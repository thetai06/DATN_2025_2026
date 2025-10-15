package org.o7planning.myapplication.data

class dataOverviewOwner(
    var storeId:String?=null,
    var ownerId:String? = null,
    var totalBooking: Int? = null, //tổng đặt bàn
    var confirm: Int? = null, //tổng chấp nhận
    var pendingBookings: Int? = null, //chờ xử lý
    var profit: Double? = null, //Doanh thu
    var tableActive: Int? = null,// bàn hoạt động
    var tableEmpty: Int? = null, // bàn trống
    var maintenance: Int? = null, // bàn bảo trì
)