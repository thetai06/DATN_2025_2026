package org.o7planning.myapplication.owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.FragmentMakeorderBinding

class FragmentMakeorder : Fragment(), OnOrderConfirmListener {

    private lateinit var binding: FragmentMakeorderBinding
    private lateinit var dbRefMakeorder: DatabaseReference
    private lateinit var listOderRealTiem: ArrayList<dataTableManagement>
    private lateinit var oderRealTime: ValueEventListener
    private lateinit var mAuth: FirebaseAuth
    private var ownerId:String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMakeorderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAuth = FirebaseAuth.getInstance()
        ownerId = mAuth.currentUser?.uid

        listOderRealTiem = arrayListOf()

        dbRefMakeorder = FirebaseDatabase.getInstance().getReference("dataBookTable")

        getOrderRealTime()
        boxOrderRealTime()
    }

    private fun getOrderRealTime() {
        oderRealTime = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listOderRealTiem.clear()
                for (orderSnapshort in snapshot.children){
                    val order = orderSnapshort.getValue(dataTableManagement::class.java)
                    if (order != null && order.btnStatus == "" && order.storeOwnerId == ownerId){
                        order?.let { listOderRealTiem.add(it) }
                    }
                }
                binding.rvOrderRealTime.adapter?.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
            }
        }
        dbRefMakeorder.addValueEventListener(oderRealTime)
    }


    private fun boxOrderRealTime() {
        binding.rvOrderRealTime.adapter = RvOrderRealTime(listOderRealTiem, this)
        binding.rvOrderRealTime.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.VERTICAL,
            false
        )
    }

    override fun onConfirmClick(data: dataTableManagement) {
        val id = data.id.toString()
        val dbRefToUpdate = dbRefMakeorder.child(id)
        val dataUpdate = hashMapOf<String, Any>(
            "btnStatus" to "Bắt đầu",
            "status" to "Đã xác nhận"
        )
        dbRefToUpdate.updateChildren(dataUpdate)
            .addOnSuccessListener {
                Toast.makeText(requireContext(),"Đã xác nhận",Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),"Xác nhận ${it.message} thất bại",Toast.LENGTH_SHORT).show()
            }
    }

    override fun onRefuseRealtimeClick(id: String) {
        dbRefMakeorder.child(id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(),"Tu choi thanh cong",Toast.LENGTH_SHORT).show()
                binding.rvOrderRealTime.adapter?.notifyDataSetChanged()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),"Tu choi khong thanh cong",Toast.LENGTH_SHORT).show()
            }
    }
}