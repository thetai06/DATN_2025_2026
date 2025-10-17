package org.o7planning.myapplication.Customer
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

// Dùng để gửi dữ liệu (số tiền, mã đơn hàng) lên server
data class PaymentRequest(val amount: Double, val orderId: String)

// Dùng để nhận URL thanh toán trả về
data class PaymentResponse(val paymentUrl: String)

// Định nghĩa API endpoint
interface ApiService {
    @POST("/create_payment_url")
    fun createPaymentUrl(@Body request: PaymentRequest): Call<PaymentResponse>
}