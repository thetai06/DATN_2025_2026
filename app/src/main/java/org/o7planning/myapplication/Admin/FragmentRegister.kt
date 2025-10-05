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
import com.google.firebase.FirebaseException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataUser
import org.o7planning.myapplication.databinding.FragmentRegisterBinding
import java.util.concurrent.TimeUnit

class FragmentRegister : Fragment() {
    private lateinit var binding: FragmentRegisterBinding
    private lateinit var dbRefUser: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private var verificationId: String? = null
    private lateinit var callback: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    private  var inputName: String? = null
    private var inputEmail:String? = null
    private var inputPhoneNumber: String?= null
    private val role = "user"


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRefUser = FirebaseDatabase.getInstance().getReference("dataUser")

        mAuth = FirebaseAuth.getInstance()
        setUpCallBack()

        binding.btnSignup.setOnClickListener {
            val name = binding.edtNameSignup.text.toString()
            val input = binding.edtEmailSignup.text.toString()
            val password = binding.edtPasswordSignup.text.toString()
            val passwordRestype = binding.edtRestypePassword.text.toString()

            if (validateInput(name, input, password, passwordRestype)) {
                inputName = name
                inputEmail = null
                inputPhoneNumber = null

                if (isEmail(input)) {
                    inputEmail = input
                    registerUser(input, password)
                } else if (isPhoneNumber(input)) {
                    inputPhoneNumber = input
                    sendVerificationCode(input)
                    val builder = AlertDialog.Builder(requireContext())
                    val dialogView = layoutInflater.inflate(R.layout.dialog_send_code, null)
                    builder.setView(dialogView)
                    val dialog = builder.create()

                    val btnVerifyOtp = dialogView.findViewById<Button>(R.id.btn_Ok_Password_SendCode)
                    btnVerifyOtp.setOnClickListener {
                        val otpCode = dialogView.findViewById<EditText>(R.id.edt_SendCode).text.toString()
                                .trim()
                        verifyCode(otpCode,dialog)
                    }
                    dialog.show()
                }
            }

        }
    }

    private fun verifyCode(otpCode: String, dialog: AlertDialog) {
        if (verificationId == null) {
            Toast.makeText(requireContext(), "Vui lòng gửi lại mã xác minh.", Toast.LENGTH_SHORT).show()
            return
        }

        if (otpCode.isEmpty() || otpCode.length != 6) {
            Toast.makeText(requireContext(), "Mã OTP không hợp lệ.", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo thông tin xác thực (Credential) từ ID và Mã OTP
        val credential = PhoneAuthProvider.getCredential(verificationId!!, otpCode)

        // Hoàn tất quá trình đăng nhập/đăng ký
        signInWithPhone(credential){ success ->
            if (success){
                dialog.dismiss()
            }
        }
    }

    private fun signInWithPhone(credential: PhoneAuthCredential, onComplete:((Boolean)-> Unit)? = null) {
        mAuth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("PhoneAuth", "Đăng nhập thành công!")
                    Toast.makeText(requireContext(), "Đăng nhập bằng SĐT thành công!", Toast.LENGTH_LONG).show()
                    saveAllUserData()
                    onComplete?.invoke(true)
                } else {
                    val errorMessage = task.exception?.message
                    Log.e("PhoneAuth", "Xác thực thất bại: $errorMessage", task.exception)
                    Toast.makeText(requireContext(), "Xác thực thất bại. Vui lòng kiểm tra mã OTP.", Toast.LENGTH_LONG).show()
                    onComplete?.invoke(false)
                }
            }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        if (phoneNumber.isEmpty() || phoneNumber.length < 9) {
            Toast.makeText(requireContext(), "Số điện thoại không hợp lệ.", Toast.LENGTH_SHORT).show()
            return
        }

        val fullPhoneNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+84$phoneNumber"

        PhoneAuthProvider.getInstance().verifyPhoneNumber(
            fullPhoneNumber,      // Số điện thoại
            60,                   // Thời gian chờ (giây)
            TimeUnit.SECONDS,
            requireActivity(),
            callback            // Đối tượng xử lý sự kiện
        )
        Toast.makeText(requireContext(), "Đang gửi mã xác minh...", Toast.LENGTH_SHORT).show()
    }

    private fun setUpCallBack() {
        callback = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(p0: PhoneAuthCredential) {
                Log.d("PhoneAuth", "Tu dong xac minh hoan tat")
                signInWithPhone(p0)
            }

            override fun onVerificationFailed(p0: FirebaseException) {
                Log.e("PhoneAuth", "Xac minh that bai: ${p0.message}")
                Toast.makeText(requireContext(), "Xác minh thất bại: ${p0.message}", Toast.LENGTH_LONG).show()
            }

            override fun onCodeSent(p0: String, p1: PhoneAuthProvider.ForceResendingToken) {
                Log.d("PhoneAuth","Ma da gui. ID: $p0")
                this@FragmentRegister.verificationId = p0
                Toast.makeText(requireContext(),"Mã OTP đã được gửi đến số điện thoại.", Toast.LENGTH_LONG).show()
            }

        }
    }

    private fun registerUser(email: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    Log.d("Register","Thanh Cong")
                    Toast.makeText(requireContext(),"Luu dataUser thanh cong!", Toast.LENGTH_SHORT).show()
                    saveAllUserData()
                }
            }

    }

    private fun saveAllUserData() {
        val userId = mAuth.currentUser?.uid?: return
        val name = inputName?:""
        val email = inputEmail
        val phone = inputPhoneNumber
        saveUserRole(userId,name,role, phone,email)

        val dbUser = dataUser(userId, phone, name, email, role)
        dbRefUser.child(userId).setValue(dbUser)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Lưu dữ liệu người dùng thành công!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi lưu Realtime DB.", Toast.LENGTH_SHORT).show()
            }
        findNavController().navigate(R.id.fragment_login)
    }

    private fun saveUserRole(userId: String, name: String, role: String, numberPhone: String?, email: String?) {
        val db = FirebaseFirestore.getInstance()
        val userMap = hashMapOf(
            "name" to name,
            "role" to role,
            "numberPhone" to numberPhone,
            "email" to email,
            "createdAt" to Timestamp.now()
        )

        db.collection("users").document(userId)
            .set(userMap)
            .addOnSuccessListener {
                Log.d("Firestore", "Đã lưu vai trò thành công.")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Lỗi khi lưu vai trò", e)
            }
    }

    private fun isEmail(input: String): Boolean {
        return return Patterns.EMAIL_ADDRESS.matcher(input).matches()
    }

    private fun isPhoneNumber(input: String): Boolean {
        val normalizedInput = input.replace("[^0-9+]".toRegex(), "")
        return Patterns.PHONE.matcher(normalizedInput).matches()
    }


    private fun validateInput(
        name: String,
        email: String,
        password: String,
        passwordRestype: String
    ): Boolean {
        return when {
            name.isEmpty() -> {
                Toast.makeText(requireContext(), "Vui lòng nhập tên!", Toast.LENGTH_SHORT).show()
                false
            }

            email.isEmpty() -> {
                Toast.makeText(requireContext(), "Vui lòng nhập email!", Toast.LENGTH_SHORT).show()
                false
            }

            password.isEmpty() -> {
                Toast.makeText(requireContext(), "Vui lòng nhập password!", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            passwordRestype.isEmpty() -> {
                Toast.makeText(requireContext(), "Vui lòng xác nhận mật khẩu!", Toast.LENGTH_SHORT)
                    .show()
                false
            }

            password != passwordRestype -> {
                Toast.makeText(requireContext(), "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show()
                false
            }

            else -> true
        }
    }
}