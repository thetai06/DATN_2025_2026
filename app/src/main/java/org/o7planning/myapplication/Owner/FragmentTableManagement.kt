package org.o7planning.myapplication.Owner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.FragmentTablemanagementBinding

class FragmentTableManagement : Fragment(), setOnclickTableManagement {

    private lateinit var binding: FragmentTablemanagementBinding
    private lateinit var list: ArrayList<dataTableManagement>
    private lateinit var dbRefTableManagement: DatabaseReference
    private lateinit var tableManagementAdapter: RvTableManagement
    private lateinit var mAuth: FirebaseAuth
    private var ownerId:String ? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentTablemanagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRefTableManagement = FirebaseDatabase.getInstance().getReference("dataBookTable")
        list = arrayListOf()

        mAuth = FirebaseAuth.getInstance()
        ownerId = mAuth.currentUser?.uid

        boxTableManagement()
        dataBoxTableManagement()
    }

    private fun dataBoxTableManagement() {
        dbRefTableManagement.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                list.clear()
                if (snapshot.exists()){
                    for (listSnap in snapshot.children){
                        val listData = listSnap.getValue(dataTableManagement::class.java)
                        if (listData != null && listData.storeOwnerId == ownerId && (listData.btnStatus == "Bắt đầu" || listData.btnStatus == "Hoàn thành")){
                            listData.let { list.add(it) }
                        }
                    }
                }
                tableManagementAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }

        })
    }

    private fun boxTableManagement() {
        tableManagementAdapter = RvTableManagement(list,this)
        binding.rvTableManagement.adapter = tableManagementAdapter
        binding.rvTableManagement.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.VERTICAL,
            false
        )
    }

    override fun onClickComplete(data: dataTableManagement) {
        val id = data.id.toString()
        val dbRefToUpdate = dbRefTableManagement.child(id)
        val dataUpdate = hashMapOf<String, Any>(
            "btnStatus" to "Hoàn thành",
            "status" to "Đang chơi"
        )
        dbRefToUpdate.updateChildren(dataUpdate)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Hoàn thành đơn", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Không thể hoàn thành: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onClickEndComplete(data: dataTableManagement) {
        val id = data.id.toString()
        val dbRefToUpdate = dbRefTableManagement.child(id)
        val dataUpdate = hashMapOf<String, Any>(
            "btnStatus" to "End",
            "status" to "Đã hoàn thành"
        )
        dbRefToUpdate.updateChildren(dataUpdate)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Hoàn thành đơn", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Không thể hoàn thành: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

