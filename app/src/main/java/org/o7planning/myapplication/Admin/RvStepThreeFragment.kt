package org.o7planning.myapplication.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.FragmentRvStepThreeBinding

class RvStepThreeFragment : Fragment() {

    private var _binding: FragmentRvStepThreeBinding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRvStepThreeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        // Sự kiện khi người dùng tick vào checkbox "Chuyển khoản ngân hàng"
        binding.checkboxBank.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxWallet.isChecked = false
            }

            // SỬA LỖI: Gọi hàm setter của ViewModel
            registrationViewModel.setCheckboxBank(isChecked)

            // Nếu người dùng bỏ chọn bank, phải cập nhật cả wallet
            if (!isChecked && !binding.checkboxWallet.isChecked) {
                registrationViewModel.setCheckboxWallet(false)
            }
        }

        // Sự kiện khi người dùng tick vào checkbox "Ví điện tử MoMo"
        binding.checkboxWallet.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.checkboxBank.isChecked = false
            }

            // SỬA LỖI: Gọi hàm setter của ViewModel
            registrationViewModel.setCheckboxWallet(isChecked)

            // Nếu người dùng bỏ chọn wallet, phải cập nhật cả bank
            if (!isChecked && !binding.checkboxBank.isChecked) {
                registrationViewModel.setCheckboxBank(false)
            }
        }
    }

    private fun updatePackageSelectionUI(selectedPackageName: String) {
        if (selectedPackageName == "Go Premium Business") {
            binding.boxPrice.setBackgroundResource(R.drawable.bg_xam)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}