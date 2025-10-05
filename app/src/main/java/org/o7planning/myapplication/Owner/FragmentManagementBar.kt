package org.o7planning.myapplication.Owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.o7planning.myapplication.Admin.MainActivity
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataStort
import org.o7planning.myapplication.databinding.FragmentManagementBarBinding

class FragmentManagementBar : Fragment() {

    private lateinit var binding: FragmentManagementBarBinding
    private lateinit var dbRefManagementStore: DatabaseReference
    private lateinit var storeValueEventListener: ValueEventListener
    private lateinit var listStore: ArrayList<dataStort>
    private lateinit var stortAdapter: RvManagementBar
    private lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManagementBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listStore = arrayListOf()
        dbRefManagementStore = FirebaseDatabase.getInstance().getReference("dataStore")
        mAuth = FirebaseAuth.getInstance()

        setupManagementBar()
        addStore()
        logOut()
    }

    private fun logOut() {
        binding.btnLogOut.setOnClickListener {
            val navOptions = androidx.navigation.navOptions {
                popUpTo(R.id.nav_graph){
                    inclusive = true
                }
            }
            mAuth.signOut()
            (activity as MainActivity).hideBottomNavigation()
            findNavController().navigate(R.id.fragment_splash,null,navOptions)
        }
    }

    private fun addStore() {
        binding.btnAddStore.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_store,null)
            builder.setView(dialogView)
            val alertDialog = builder.create()
            val edtName = dialogView.findViewById<EditText>(R.id.edtName)
            val edtLocation = dialogView.findViewById<EditText>(R.id.edtLocation)
            val edtNumberPhone = dialogView.findViewById<EditText>(R.id.edtNumberPhone)
            val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
            val edtOperationTime = dialogView.findViewById<EditText>(R.id.edtOperationTime)
            val edtNumberTable = dialogView.findViewById<EditText>(R.id.edtNumberTable)
            val edtDes = dialogView.findViewById<EditText>(R.id.edtDes)
            val btnAddStore = dialogView.findViewById<Button>(R.id.btnConfirmAddStore)

            btnAddStore.setOnClickListener {
                val name = edtName.text.toString().trim()
                val location = edtLocation.text.toString().trim()
                val numberPhone = edtNumberPhone.text.toString().trim()
                val email = edtEmail.text.toString().trim()
                val operationTime = edtOperationTime.text.toString().trim()
                val numberTable = edtNumberTable.text.toString().trim()
                val des = edtDes.text.toString().trim()
                if (location.isEmpty() && numberPhone.isEmpty() && email.isEmpty() && operationTime.isEmpty() && numberTable.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng điền đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val id = dbRefManagementStore.push().key!!

                val dataStore = dataStort(id,name,R.drawable.quan_bi_a1, location, numberPhone, email, operationTime, numberTable, des )

                dbRefManagementStore.child(id).setValue(dataStore)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),"Thêm cửa hàng thành công!", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),"thêm không thanành công!", Toast.LENGTH_SHORT).show()
                    }
            }
            alertDialog.show()
        }
    }

    private fun setupManagementBar() {
        stortAdapter = RvManagementBar(listStore)
        binding.rvBilliardsBarManagement.adapter= stortAdapter
        binding.rvBilliardsBarManagement.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.VERTICAL,
            false
        )

        storeValueEventListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listStore.clear()
                if (snapshot.exists()){
                    for (storeSnap in snapshot.children){
                        val storeData = storeSnap.getValue(dataStort::class.java)
                        storeData?.let { listStore.add(it) }
                    }
                }
                stortAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }
        dbRefManagementStore.addValueEventListener(storeValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRefManagementStore.removeEventListener(storeValueEventListener)
    }

}