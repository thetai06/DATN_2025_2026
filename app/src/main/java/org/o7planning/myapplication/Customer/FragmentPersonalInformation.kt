package org.o7planning.myapplication.Customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.o7planning.myapplication.data.dataUser
import org.o7planning.myapplication.databinding.FragmentPersonalInformationBinding

class FragmentPersonalInformation : Fragment() {
    private lateinit var binding: FragmentPersonalInformationBinding
    private lateinit var dbRefUser: DatabaseReference
    private lateinit var mAuth: FirebaseAuth
    private var userId: String? = null
    private var userValueEventListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentPersonalInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbRefUser = FirebaseDatabase.getInstance().getReference("dataUser")

        mAuth = FirebaseAuth.getInstance()
        userId = mAuth.currentUser?.uid

        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        attachUserListener()
    }

    override fun onStop() {
        super.onStop()
        detachUserListener()
    }

    private fun attachUserListener() {
        if (userId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Lỗi: Người dùng không xác định", Toast.LENGTH_SHORT).show()
            return
        }

        if (userValueEventListener == null) {
            userValueEventListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userData = snapshot.getValue(dataUser::class.java)
                        userData?.let { user ->
                            updateUI(user)
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Lỗi: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
            dbRefUser.child(userId!!).addValueEventListener(userValueEventListener!!)
        }
    }

    private fun detachUserListener() {
        userValueEventListener?.let { listener ->
            if (!userId.isNullOrEmpty()) {
                dbRefUser.child(userId!!).removeEventListener(listener)
            }
        }
        userValueEventListener = null
    }

    private fun updateUI(user: dataUser) {
        binding.apply {
            if (edtName.text.toString() != user.name) {
                edtName.setText(user.name)
            }
            if (tvEmail.text.toString() != user.email) {
                tvEmail.setText(user.email)
            }
            if (edtPhoneNumber.text.toString() != user.phoneNumber) {
                edtPhoneNumber.setText(user.phoneNumber)
            }
            if (edtDateOfBirth.text.toString() != user.dateBirth) {
                edtDateOfBirth.setText(user.dateBirth)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().popBackStack()
        }
        binding.btnUpdate.setOnClickListener {
            updatePersonalInformation()
        }
    }

    private fun updatePersonalInformation() {
        if (userId.isNullOrEmpty()) return

        binding.apply {
            val name = edtName.text.toString().trim()
            val phoneNumber = edtPhoneNumber.text.toString().trim()
            val dateOfBirth = edtDateOfBirth.text.toString().trim()

            val updateMap = mapOf<String, Any>(
                "name" to name,
                "phoneNumber" to phoneNumber,
                "dateBirth" to dateOfBirth
            )

            dbRefUser.child(userId!!).updateChildren(updateMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Lỗi cập nhật!", Toast.LENGTH_SHORT).show()
                }
        }
    }

}