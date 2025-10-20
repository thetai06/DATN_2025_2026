package org.o7planning.myapplication.Admin // Đổi thành 'admin' nếu cần

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.o7planning.myapplication.admin.RegistrationViewModel
import org.o7planning.myapplication.databinding.FragmentRVStepTwoBinding
import androidx.activity.result.PickVisualMediaRequest
// Xóa import trùng lặp 'androidx.fragment.app.activityViewModels'

class RvStepTwoFragment : Fragment() {

    private var _binding: FragmentRVStepTwoBinding? = null
    private val binding get() = _binding!!
    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    // --- SỬA LỖI: Dùng PickVisualMedia cho TẤT CẢ ---

    // 1. Trình khởi chạy cho Giấy phép kinh doanh
    private val licenseLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("ImagePicker", "Đã chọn ảnh Giấy phép: $uri")
            // GỌI HÀM SETTER CỦA VIEWMODEL
            registrationViewModel.setBusinessLicenseUri(uri)

            // Cập nhật UI
            binding.ivSelectedLicense.setImageURI(uri)
            binding.ivSelectedLicense.visibility = View.VISIBLE
            binding.tvFileName.text = "Đã chọn: GiayPhepKinhDoanh.jpg"
            binding.btnUploadLicense.text = "Thay đổi"
        } else {
            Log.d("ImagePicker", "Không chọn ảnh Giấy phép")
        }
    }

    // 2. Trình khởi chạy cho MẶT TRƯỚC CCCD
    private val idFrontLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("ImagePicker", "Đã chọn ảnh Mặt trước CCCD: $uri")
            // GỌI HÀM SETTER CỦA VIEWMODEL
            registrationViewModel.setNationalIdFrontUri(uri)

            // Cập nhật UI
            binding.ivSelectedIdFront.setImageURI(uri)
            binding.ivSelectedIdFront.visibility = View.VISIBLE
            updateCccdUi()
        } else {
            Log.d("ImagePicker", "Không chọn ảnh Mặt trước CCCD")
        }
    }

    // 3. Trình khởi chạy cho MẶT SAU CCCD
    private val idBackLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            Log.d("ImagePicker", "Đã chọn ảnh Mặt sau CCCD: $uri")
            // GỌI HÀM SETTER CỦA VIEWMODEL
            registrationViewModel.setNationalIdBackUri(uri)

            // Cập nhật UI
            binding.ivSelectedIdBack.setImageURI(uri)
            binding.ivSelectedIdBack.visibility = View.VISIBLE
            updateCccdUi()
        } else {
            Log.d("ImagePicker", "Không chọn ảnh Mặt sau CCCD")
        }
    }

    // --- (Xóa các launcher 'GetContent' cũ ở đây) ---

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRVStepTwoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
        // Xóa sự kiện click trùng lặp ở đây
    }

    private fun setupListeners() {
        // Sự kiện nhấn nút tải lên Giấy phép kinh doanh
        binding.btnUploadLicense.setOnClickListener {
            // Chỉ cần gọi launcher mới
            licenseLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // Sự kiện nhấn nút tải lên CCCD
        binding.btnUploadCCCD.setOnClickListener {
            showCccdSelectionDialog()
        }

        // --- SỬA LỖI: GỌI HÀM SETTER CỦA VIEWMODEL ---
        binding.edtBankAccountNumber.doAfterTextChanged { text ->
            registrationViewModel.setBankAccountNumber(text.toString().trim())
        }

        binding.edtNameBank.doAfterTextChanged { text ->
            registrationViewModel.setBankName(text.toString().trim())
        }

        binding.edtDateBank.doAfterTextChanged { text ->
            // Giả sử đây là tên chủ thẻ
            registrationViewModel.setBankHolderName(text.toString().trim())
        }
    }

    // Hiển thị dialog để người dùng chọn tải mặt trước hay mặt sau
    private fun showCccdSelectionDialog() {
        val options = arrayOf("Tải lên mặt trước", "Tải lên mặt sau")
        AlertDialog.Builder(requireContext())
            .setTitle("Chọn ảnh CCCD/CMND")
            .setItems(options) { dialog, which ->

                val request = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                when (which) {
                    // SỬA LỖI: Gọi launcher mới
                    0 -> idFrontLauncher.launch(request)
                    1 -> idBackLauncher.launch(request)
                }
                dialog.dismiss()
            }
            .show()
    }

    // Cập nhật giao diện sau khi chọn ảnh CCCD (Hàm này đã ổn)
    private fun updateCccdUi() {
        val hasFront = registrationViewModel.nationalIdFrontUri.value != null
        val hasBack = registrationViewModel.nationalIdBackUri.value != null

        if (hasFront || hasBack) {
            binding.groupPlaceholderCccd.visibility = View.GONE
        } else {
            binding.groupPlaceholderCccd.visibility = View.VISIBLE
        }

        binding.tvCCCDFileName.text = when {
            hasFront && hasBack -> "Đã chọn mặt trước và mặt sau"
            hasFront -> "Đã chọn mặt trước (Vui lòng chọn thêm mặt sau)"
            hasBack -> "Đã chọn mặt sau (Vui lòng chọn thêm mặt trước)"
            else -> "Hai mặt trước và sau"
        }
        binding.btnUploadCCCD.text = "Thay đổi ảnh"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Tránh memory leak
    }
}