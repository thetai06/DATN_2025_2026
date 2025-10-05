package org.o7planning.myapplication.Admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.FragmentSendCodeBinding

class FragmentSendCode : Fragment() {

    private lateinit var mAuth: FirebaseAuth

    private lateinit var binding: FragmentSendCodeBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSendCodeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        binding.btnSendCode.setOnClickListener {
            val email = binding.edtEmailSendCode.text.toString().trim()

            if (email.isEmpty()){
                Toast.makeText(requireContext(),"Vui lòng nhập Email đã đăng ký.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendPasswordReset(email)
        }
    }

    private fun sendPasswordReset(email: String) {
        binding.btnSendCode.isEnabled = false
        mAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.btnSendCode.isEnabled = true

                if (task.isSuccessful){
                    Toast.makeText(requireContext(), "Liên kết lấy lại mật khẩu đã được gửi đến $email. Vui lòng kiểm tra hộp thư.", Toast.LENGTH_LONG).show()
                    findNavController().navigate(R.id.fragment_login)
                }else{
                    val errorMessage = task.exception?.message
                    Toast.makeText(requireContext(), "Gửi yêu cầu thất bại. Vui lòng kiểm tra Email hoặc kết nối mạng.", Toast.LENGTH_LONG).show()
                    Log.e("ResetPassword", "Error: $errorMessage")
                }
            }
    }
}