package org.o7planning.myapplication.admin

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.journeyapps.barcodescanner.BarcodeEncoder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import com.google.firebase.firestore.EventListener
import okhttp3.OkHttpClient
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.FragmentUpgradeBinding
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class FragmentUpgrade : Fragment(R.layout.fragment_upgrade) {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: PagerAdapterUpgrade
    private lateinit var dbRefUser: DatabaseReference
    private lateinit var dbRefStoreMain: DatabaseReference // Thêm Ref cho dataStoreMain
    private lateinit var dbFirestore: FirebaseFirestore    // Thêm Ref cho Firestore (để lắng nghe)

    // Thêm biến để giữ listener
    private var roleListenerRegistration: ListenerRegistration? = null

    private var _binding: FragmentUpgradeBinding? = null

    private val registrationViewModel: RegistrationViewModel by activityViewModels()
    private lateinit var binding: FragmentUpgradeBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUpgradeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = binding.viewPager
        adapter = PagerAdapterUpgrade(requireActivity())
        viewPager.adapter = adapter

        // Khởi tạo các Ref
        dbRefUser = FirebaseDatabase.getInstance().getReference("dataUser")
        dbRefStoreMain = FirebaseDatabase.getInstance().getReference("dataStoreMain")
        dbFirestore = FirebaseFirestore.getInstance()

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateUiForStep(position)
            }
        })

        updateUiForStep(0)

        // Bỏ biến 'currenStep' không cần thiết

        binding.btnNext.setOnClickListener {
            val currentItem = viewPager.currentItem
            val isCurrentStepValid = when (currentItem) {
                0 -> registrationViewModel.isStepOneValid()
                1 -> registrationViewModel.isStepTwoValid()
                2 -> registrationViewModel.isStepThreeValid()
                else -> false
            }

            if (!isCurrentStepValid) {
                Toast.makeText(requireContext(), "Vui lòng điền đầy đủ các trường bắt buộc.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentItem == adapter.itemCount - 1) {
                // SỬA LẠI: Gọi hàm xử lý thanh toán
                handleUpgradeSubmission()
            } else {
                val nextItem = currentItem + 1
                viewPager.currentItem = nextItem
                updateUiForStep(nextItem)
            }
        }

        binding.btnBack.setOnClickListener {
            val backItem = viewPager.currentItem - 1
            if (backItem == -1){
                findNavController().navigate(R.id.fragment_profile)
                (activity as? MainActivity)?.setupBottomNavigation(false)
            } else {
                viewPager.currentItem = backItem
                // 'onPageSelected' sẽ tự động gọi 'updateUiForStep'
            }
        }
    }

    // Xóa hàm 'updateStepBackground' (đã gộp)

    // Hàm gộp để cập nhật UI, tránh lặp code
    private fun updateUiForStep(step: Int) {
        val step1Background = if(step == 0) R.drawable.bg_upgrade_orgrien else R.drawable.bg_upgrade_white
        val step2Background = if(step == 1) R.drawable.bg_upgrade_orgrien else R.drawable.bg_upgrade_white
        val step3Background = if (step == 2) R.drawable.bg_upgrade_orgrien else R.drawable.bg_upgrade_white

        binding.ivStep1.setBackgroundResource(step1Background)
        binding.ivStep2.setBackgroundResource(step2Background)
        binding.ivStep3.setBackgroundResource(step3Background)

        if (step == adapter.itemCount - 1) {
            binding.btnNext.text = "Thanh toán & Hoàn tất" // Đổi tên nút
        } else {
            binding.btnNext.text = "Tiếp theo"
        }
    }

    // HÀM MỚI: Xử lý khi nhấn nút "Hoàn tất"
    private fun handleUpgradeSubmission() {
        if (registrationViewModel.checkboxBank.value == true) {
            // Nếu người dùng chọn VNPAY
            executeVnPayUpgrade()
        } else if (registrationViewModel.checkboxWallet.value == true) {
            // Nếu người dùng chọn ví MoMo (hoặc khác)
            Toast.makeText(requireContext(), "Phương thức này chưa được hỗ trợ", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Vui lòng chọn phương thức thanh toán", Toast.LENGTH_SHORT).show()
        }
    }

    // SỬA LẠI: Đổi tên hàm và logic
    private fun executeVnPayUpgrade() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Toast.makeText(requireContext(), "Lỗi: Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.fragment_login)
            return
        }

        val licenseUri = registrationViewModel.businessLicenseUri.value
        val idFrontUri = registrationViewModel.nationalIdFrontUri.value
        val idBackUri = registrationViewModel.nationalIdBackUri.value

        if (licenseUri == null || idFrontUri == null || idBackUri == null) {
            Toast.makeText(requireContext(), "Lỗi: Thiếu ảnh. Vui lòng quay lại.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnNext.isEnabled = false
        binding.btnBack.isEnabled = false
        Toast.makeText(requireContext(), "Đang xử lý ảnh, vui lòng chờ...", Toast.LENGTH_LONG).show()

        val licenseBase64 = uriToResizedBase64(licenseUri)
        val idFrontBase64 = uriToResizedBase64(idFrontUri)
        val idBackBase64 = uriToResizedBase64(idBackUri)

        if (licenseBase64 == null || idFrontBase64 == null || idBackBase64 == null) {
            Toast.makeText(requireContext(), "Lỗi xử lý ảnh. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
            binding.btnNext.isEnabled = true
            binding.btnBack.isEnabled = true
            return
        }

        Log.d("Base64", "Đã xử lý 3 ảnh thành công.")

        // SỬA LẠI: Dùng push() key để tạo storeId
        val newStoreId = dbRefStoreMain.push().key
        if (newStoreId == null) {
            Toast.makeText(requireContext(), "Lỗi tạo ID cửa hàng.", Toast.LENGTH_SHORT).show()
            binding.btnNext.isEnabled = true
            binding.btnBack.isEnabled = true
            return
        }

        val storeMainData = hashMapOf(
            "ownerId" to userId,
            "storeName" to registrationViewModel.storeName.value,
            "phoneNumber" to registrationViewModel.phoneNumber.value,
            "address" to registrationViewModel.address.value,
            "email" to registrationViewModel.email.value,
            "description" to registrationViewModel.description.value,
            "bankAccountNumber" to registrationViewModel.bankAccountNumber.value,
            "bankName" to registrationViewModel.edtNameBank.value,
            "bankDate" to registrationViewModel.edtDateBank.value,
            "businessLicenseBase64" to licenseBase64,
            "nationalIdFrontBase64" to idFrontBase64,
            "nationalIdBackBase64" to idBackBase64,
            "createdAt" to System.currentTimeMillis(),
            "paymentStatus" to "Chờ thanh toán VNPay" // Thêm trạng thái
        )

        // 4. Lưu vào Realtime Database
        dbRefStoreMain.child(newStoreId).setValue(storeMainData)
            .addOnSuccessListener {
                Log.d("Firebase", "Lưu thông tin cửa hàng thành công, đang gọi API thanh toán...")
                // 5. Nếu lưu DB thành công -> GỌI API THANH TOÁN
                // (Bạn cần sửa lại số tiền)
                callPaymentApi(newStoreId, userId, 5000000.0)
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Lỗi lưu thông tin cửa hàng", e)
                Toast.makeText(requireContext(), "Tạo cửa hàng thất bại (lỗi DB).", Toast.LENGTH_SHORT).show()
                binding.btnNext.isEnabled = true
                binding.btnBack.isEnabled = true
            }
    }

    // HÀM MỚI: Gọi API
    private fun callPaymentApi(storeId: String, userId: String, amount: Double) {
        Toast.makeText(requireContext(), "Đang tạo mã QR thanh toán...", Toast.LENGTH_SHORT).show()

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api-datn-2025.onrender.com") // URL server của bạn
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        val request = PaymentRequestUpgrade(
            amount = amount,
            userId = userId,
            storeId = storeId
        )

        Log.d("VNPay_API", "Đang gửi yêu cầu nâng cấp: $request")

        apiService.createUpgradePaymentUrl(request).enqueue(object : Callback<PaymentResponse> {
            override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val paymentUrl = response.body()!!.paymentUrl
                    Log.d("VNPay_API", "Nhận được URL nâng cấp: $paymentUrl")

                    // Hiển thị QR (truyền userId để lắng nghe role)
                    showQrCodeDialog(paymentUrl, userId, amount)

                    // Kích hoạt lại nút Back, nhưng nút Next vẫn vô hiệu
                    binding.btnBack.isEnabled = true
                } else {
                    Log.e("VNPay_API", "Lỗi server: ${response.code()}")
                    Toast.makeText(requireContext(), "Server báo lỗi. Vui lòng thử lại.", Toast.LENGTH_SHORT).show()
                    binding.btnNext.isEnabled = true
                    binding.btnBack.isEnabled = true
                }
            }

            override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                Log.e("VNPay_API", "Lỗi kết nối: ${t.message}")
                Toast.makeText(requireContext(), "Lỗi kết nối. Vui lòng kiểm tra Internet.", Toast.LENGTH_SHORT).show()
                binding.btnNext.isEnabled = true
                binding.btnBack.isEnabled = true
            }
        })
    }

    // HÀM MỚI: Hiển thị QR (Copy từ FragmentBooktable và sửa)
    private fun showQrCodeDialog(paymentUrl: String, userIdToListen: String, amount: Double) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_qr_payment, null)
        builder.setView(dialogView)
        builder.setCancelable(false)
        val qrDialog: AlertDialog = builder.create()

        val ivQrCode = dialogView.findViewById<ImageView>(R.id.ivQrCode)
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvQrAmount)
        val btnClose = dialogView.findViewById<Button>(R.id.btnClose)
        val tvStatus = dialogView.findViewById<TextView>(R.id.tvPaymentStatus)

        tvAmount.text = String.format("%.0f VND", amount)

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap: Bitmap = barcodeEncoder.encodeBitmap(paymentUrl, com.google.zxing.BarcodeFormat.QR_CODE, 400, 400)
            ivQrCode.setImageBitmap(bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Lỗi tạo mã QR", Toast.LENGTH_SHORT).show()
        }

        // --- SỬA LẠI: Lắng nghe VAI TRÒ (ROLE) trên FIRESTORE ---
        val userRoleRef = dbFirestore.collection("users").document(userIdToListen)

        roleListenerRegistration = userRoleRef.addSnapshotListener(object :
            EventListener<DocumentSnapshot> {
            override fun onEvent(snapshot: DocumentSnapshot?, e: FirebaseFirestoreException?) {
                if (e != null) {
                    Log.e("PaymentListener", "Lỗi lắng nghe Firestore: ${e.message}")
                    return
                }

                if (snapshot != null && snapshot.exists()) {
                    val role = snapshot.getString("role")

                    if (role == "owner") {
                        // NÂNG CẤP THÀNH CÔNG!
                        tvStatus.text = "Thanh toán thành công!"
                        tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.green))
                        btnClose.text = "Hoàn tất"

                        // Xóa listener
                        roleListenerRegistration?.remove()

                        // Tự động đóng và chuyển trang
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            qrDialog.dismiss()
                            completeUpgradeUI() // Chuyển đến trang chủ owner
                        }, 3000)
                    }
                    // (Nếu không phải 'owner' thì cứ tiếp tục chờ)
                }
            }
        })

        btnClose.setOnClickListener {
            // Hủy listener khi người dùng tự đóng dialog
            roleListenerRegistration?.remove()
            qrDialog.dismiss()

            // Kích hoạt lại các nút để thử lại
            binding.btnNext.isEnabled = true
            binding.btnBack.isEnabled = true
        }

        qrDialog.show()
    }

    // Hàm chuyển đổi Base64 (giữ nguyên)
    private fun uriToResizedBase64(uri: Uri): String? {
        try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (originalBitmap == null) {
                Log.e("ImageConvert", "Không thể decode Bitmap từ Uri")
                return null
            }
            val targetWidth = 1024
            val ratio = originalBitmap.width.toFloat() / originalBitmap.height.toFloat()
            val targetHeight = (targetWidth / ratio).toInt()
            val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, false)
            val byteStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteStream)
            val byteArray = byteStream.toByteArray()
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ImageConvert", "Lỗi chuyển ảnh sang Base64: ${e.message}")
            return null
        }
    }

    // XÓA HÀM NÀY (SERVER ĐÃ LÀM)
    // private fun upgradeUserRole(userId: String) { ... }

    // Hàm hoàn tất (giữ nguyên)
    private fun completeUpgradeUI() {
        findNavController().navigate(R.id.fragment_overview)
        (activity as? MainActivity)?.setupBottomNavigation(true)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Đảm bảo hủy listener khi Fragment bị hủy
        roleListenerRegistration?.remove()
        _binding = null
    }
}