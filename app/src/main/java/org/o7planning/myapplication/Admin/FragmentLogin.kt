package org.o7planning.myapplication.Admin

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.FragmentLoginBinding
import java.util.concurrent.TimeUnit

class FragmentLogin : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private var verificationId: String? = null
    private lateinit var callback: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()

        setUpCallBack() // Thiết lập callback cho xác thực SĐT

        setupLoginButton()
        goToSignUp()
        goToSendCode()
    }


    private fun setupLoginButton() {
        binding.btnLogin.setOnClickListener {
            val input = binding.edtInput.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (input.isEmpty() || (isEmail(input) && password.isEmpty())) {
                Toast.makeText(
                    requireContext(),
                    "Vui lòng nhập đầy đủ thông tin.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (isEmail(input)) {
                loginUserWithEmail(input, password)
            } else if (isPhoneNumber(input)) {
                sendVerificationCode(input)
                showOtpdialog()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Email hoặc Số điện thoại không hợp lệ.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showOtpdialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_send_code, null)
        builder.setView(dialogView)
        val dialog = builder.create()

        val btnVerifyOtp = dialogView.findViewById<Button>(R.id.btn_Ok_Password_SendCode)
        btnVerifyOtp.setOnClickListener {
            val otpCode =
                dialogView.findViewById<EditText>(R.id.edt_SendCode).text.toString()
            verifyCode(otpCode, dialog)
        }
        dialog.show()
    }

    private fun loginUserWithEmail(email: String, password: String) {
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    val errorMessage = task.exception?.message
                    Toast.makeText(
                        requireContext(),
                        "Đăng nhập thất bại: ${errorMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun setUpCallBack() {
        callback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("PhoneAuth", "Tự động xác minh hoàn tất")
                signInWithPhone(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("PhoneAuth", "Xác minh thất bại: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Xác minh SĐT thất bại: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("PhoneAuth", "Mã đã gửi. ID: $verificationId")
                this@FragmentLogin.verificationId = verificationId
                Toast.makeText(
                    requireContext(),
                    "Mã OTP đã được gửi đến số điện thoại.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val fullPhoneNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+84$phoneNumber"

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            fullPhoneNumber,
            60,
            TimeUnit.SECONDS,
            requireActivity(),
            callback
        )
        Toast.makeText(requireContext(), "Đang gửi mã xác minh...", Toast.LENGTH_SHORT).show()
    }

    private fun verifyCode(otpCode: String, dialog: AlertDialog) {
        if (verificationId == null) {
            Toast.makeText(requireContext(), "Vui lòng gửi lại mã xác minh.", Toast.LENGTH_SHORT)
                .show()
            return
        }

        if (otpCode.isEmpty() || otpCode.length != 6) {
            Toast.makeText(requireContext(), "Mã OTP không hợp lệ.", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)

        signInWithPhone(credential) { success ->
            if (success) {
                dialog.dismiss()
            }
        }
    }

    private fun signInWithPhone(
        credential: PhoneAuthCredential,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    onLoginSuccess()
                    onComplete?.invoke(true)
                } else {
                    val errorMessage = task.exception?.message
                    Log.e("PhoneAuth", "Xác thực thất bại: $errorMessage", task.exception)
                    Toast.makeText(
                        requireContext(),
                        "Xác thực thất bại. Vui lòng kiểm tra mã OTP hoặc SĐT chưa đăng ký.",
                        Toast.LENGTH_LONG
                    ).show()
                    onComplete?.invoke(false)
                }
            }
    }

    private fun onLoginSuccess() {
        Toast.makeText(requireContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
        val currentUser = mAuth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Lỗi không tìm thấy người dùng!", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val navOptions = navOptions {
            popUpTo(R.id.fragment_login) {
                inclusive = true
            }
        }
        (activity as MainActivity).navigateBasedOnRole(currentUser.uid, navOptions)
    }

    private fun isEmail(input: String): Boolean {
        return input.contains("@") && Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }

    private fun isPhoneNumber(input: String): Boolean {
        val normalizedInput = input.replace("[\\s()-]".toRegex(), "")
        return normalizedInput.matches(Regex("^[+]?[0-9]{7,15}$"))
    }

    private fun goToSignUp() {
        binding.btnToSignUp.setOnClickListener {
            findNavController().navigate(R.id.fragment_signup)
        }
    }

    private fun goToSendCode() {
        binding.btnForgotPassword.setOnClickListener {
            findNavController().navigate(R.id.fragment_send_code)
        }
    }
}