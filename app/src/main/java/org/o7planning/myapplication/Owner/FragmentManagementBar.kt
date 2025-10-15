package org.o7planning.myapplication.Owner

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import org.o7planning.myapplication.data.dataOverviewOwner
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.databinding.FragmentManagementBarBinding

class FragmentManagementBar : Fragment(), setOnclickManagemenrBar {

    private lateinit var binding: FragmentManagementBarBinding
    private lateinit var dbRefManagementStore: DatabaseReference
    private lateinit var storeValueEventListener: ValueEventListener
    private lateinit var listStore: ArrayList<dataStore>
    private lateinit var stortAdapter: RvManagementBar
    private lateinit var mAuth: FirebaseAuth
    private lateinit var dbRefOverview: DatabaseReference
    private var ownerId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentManagementBarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRefManagementStore = FirebaseDatabase.getInstance().getReference("dataStore")
        listStore = arrayListOf()
        dbRefOverview = FirebaseDatabase.getInstance().getReference("dataOverview")
        mAuth = FirebaseAuth.getInstance()
        ownerId = mAuth.currentUser?.uid

        setupManagementBar()
        addStore()
        logOut()
    }

    private fun logOut() {
        binding.btnLogOut.setOnClickListener {
            mAuth.signOut()
            (activity as MainActivity).hideBottomNavigation()
            val navOptions = androidx.navigation.navOptions {
                popUpTo(R.id.nav_graph){
                    inclusive = true
                }
            }
            findNavController().navigate(R.id.fragment_splash,null, navOptions)
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
            val edtOpeningHour = dialogView.findViewById<EditText>(R.id.edtOpeningHour)
            val edtClosingHour = dialogView.findViewById<EditText>(R.id.edtClosingHour)
            val edtNumberTable = dialogView.findViewById<EditText>(R.id.edtNumberTable)
            val edtPriceTable = dialogView.findViewById<EditText>(R.id.edtPriceTable)
            val edtDes = dialogView.findViewById<EditText>(R.id.edtDes)
            val btnAddStore = dialogView.findViewById<Button>(R.id.btnConfirmAddStore)

            btnAddStore.setOnClickListener {
                val name = edtName.text.toString().trim()
                val location = edtLocation.text.toString().trim()
                val phoneNumber = edtNumberPhone.text.toString().trim()
                val email = edtEmail.text.toString().trim()
                val openingHour = edtOpeningHour.text.toString().trim()
                val closingHour = edtClosingHour.text.toString().trim()
                val tableNumber = edtNumberTable.text.toString().trim()
                val priceTable = edtPriceTable.text.toString().trim().toInt()
                val des = edtDes.text.toString().trim()
                if (location.isEmpty() && phoneNumber.isEmpty() && email.isEmpty() && openingHour.isEmpty() && closingHour.isEmpty() && tableNumber.isEmpty()) {
                    Toast.makeText(requireContext(), "Vui lòng điền đủ thông tin bắt buộc!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val storeId = dbRefManagementStore.push().key!!

                val dataStore = dataStore(
                    storeId,
                    ownerId,
                    name,
                    R.drawable.quan_bi_a1,
                    location,
                    phoneNumber,
                    email,
                    tableNumber,
                    des,
                    openingHour,
                    closingHour,
                    priceTable)
                val dataOverview = dataOverviewOwner(
                    storeId,
                    ownerId,
                    totalBooking = 0,
                    confirm = 0,
                    pendingBookings = 0,
                    profit = 0.0,
                    tableActive = 0,
                    tableEmpty = 0,
                    maintenance = 0,
                )

                dbRefManagementStore.child(storeId).setValue(dataStore)
                dbRefOverview.child(storeId).setValue(dataOverview)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),"Thêm cửa hàng thành công!", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),"thêm không thành công!", Toast.LENGTH_SHORT).show()
                    }
            }
            alertDialog.show()
        }
    }

    private fun setupManagementBar() {
        stortAdapter = RvManagementBar(listStore,this)
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
                        val storeData = storeSnap.getValue(dataStore::class.java)
                        if (storeData?.ownerId == ownerId){
                            storeData?.let { listStore.add(it) }
                        }
                    }
                }
                stortAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }

        }
        dbRefManagementStore.addValueEventListener(storeValueEventListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRefManagementStore.removeEventListener(storeValueEventListener)
    }

    override fun onClickEditBar(data: dataStore) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_store,null)
        builder.setView(dialogView)
        val alerDialog = builder.create()
        val title = dialogView.findViewById<TextView>(R.id.title)
        val edtName = dialogView.findViewById<EditText>(R.id.edtName)
        val edtLocation = dialogView.findViewById<EditText>(R.id.edtLocation)
        val edtNumberPhone = dialogView.findViewById<EditText>(R.id.edtNumberPhone)
        val edtEmail = dialogView.findViewById<EditText>(R.id.edtEmail)
        val edtOpeningHour = dialogView.findViewById<EditText>(R.id.edtOpeningHour)
        val edtClosingHour = dialogView.findViewById<EditText>(R.id.edtClosingHour)
        val edtNumberTable = dialogView.findViewById<EditText>(R.id.edtNumberTable)
        val edtDes = dialogView.findViewById<EditText>(R.id.edtDes)
        val btnAddStore = dialogView.findViewById<Button>(R.id.btnConfirmAddStore)

        title.setText("Chỉnh sửa thông tin cửa hàng")
        edtName.setText(data.name)
        edtLocation.setText(data.address)
        edtNumberPhone.setText(data.phone)
        edtEmail.setText(data.email)
        edtOpeningHour.setText(data.openingHour)
        edtClosingHour.setText(data.closingHour)
        edtNumberTable.setText(data.tableNumber)
        edtDes.setText(data.des)
        btnAddStore.setOnClickListener {
            val name = edtName.text.toString().trim()
            val location = edtLocation.text.toString().trim()
            val phoneNumber = edtNumberPhone.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val openingHour = edtOpeningHour.text.toString().trim()
            val closingHour = edtClosingHour.text.toString().trim()
            val tableNumber = edtNumberTable.text.toString().trim()
            val des = edtDes.text.toString().trim()
            updateStoreData(data.storeId, name, location, phoneNumber, email, tableNumber, des,openingHour,closingHour, alerDialog)
        }
        alerDialog.show()
    }

    private fun updateStoreData(storeId: String?, name: String, location: String, phoneNumber:String, email: String, tableNumber:String, des:String, openingHour: String, closingHour: String, alerDialog: AlertDialog){
        val update = hashMapOf<String, Any>(
            "name" to name,
            "address" to location,
            "phone" to phoneNumber,
            "email" to email,
            "tableNumber" to tableNumber,
            "des" to des,
            "openingHour" to openingHour,
            "closingHour" to closingHour

        )
        dbRefManagementStore.child(storeId.toString()).updateChildren(update)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                alerDialog.dismiss()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Lỗi cập nhật: ${it.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onClickDeleteBar(data: dataStore) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Xoá Thông Tin Cơ Sở ${data.address}")
        builder.setMessage("Bạn có chắc chắn muốn xoá cơ sở ${data.address} khỏi chuỗi cửa hàng của mình không ?")
        builder.setPositiveButton("Đồng ý"){ dialog, wich ->
            Toast.makeText(requireContext(),"Bạn đã huỷ đặt bàn :( \n Hẹn Bạn Lần sau!", Toast.LENGTH_SHORT).show()
            deleteOders(data)
            dialog.dismiss()
        }
        builder.setNegativeButton("Không"){ dialog, wich ->
            dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun deleteOders(data: dataStore) {
        val id = data.storeId.toString()
        dbRefManagementStore.child(id).removeValue()
        dbRefOverview.child(id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(),"Đã xoá CLB tại chi nhánh ${data.address}!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),"Xoá không thành công!", Toast.LENGTH_SHORT).show()
            }

    }


}