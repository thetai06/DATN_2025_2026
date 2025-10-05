package org.o7planning.myapplication.Owner

import android.app.AlertDialog
import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.privacysandbox.ads.adservices.customaudience.CustomAudience
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.recaptcha.internal.zzrq
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.serialization.descriptors.listSerialDescriptor
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataOverviewOwner
import org.o7planning.myapplication.data.dataVoucher
import org.o7planning.myapplication.databinding.FragmentOverviewBinding
import java.nio.file.attribute.AclEntry
import java.util.Collections.list

class FragmentOverview : Fragment(), onVoucherRealtimeClick {

    private lateinit var binding: FragmentOverviewBinding
    private lateinit var dbRefVoucher: DatabaseReference
    private lateinit var voucherAdaper: RvVoucherOverView
    private lateinit var voucherValueEventListener: ValueEventListener
    private lateinit var listVoucher: ArrayList<dataVoucher>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override  fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbRefVoucher = FirebaseDatabase.getInstance().getReference("dataVoucher")
        listVoucher = arrayListOf()

        setUpAddVoucher()
        setUpVoucherOverview()
        displayInformation()
    }

    private fun setUpVoucherOverview() {
        voucherAdaper = RvVoucherOverView(listVoucher, this)
        binding.rvVoucherOverview.adapter = voucherAdaper
        binding.rvVoucherOverview.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.VERTICAL,
            false
            )
        voucherValueEventListener = object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                listVoucher.clear()
                if (snapshot.exists()){
                    for (voucherSnap in snapshot.children){
                        val voucherData = voucherSnap.getValue(dataVoucher::class.java)
                        voucherData?.let { listVoucher.add(it) }
                    }
                }
                voucherAdaper.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        dbRefVoucher.addValueEventListener(voucherValueEventListener)


    }


    private fun setUpAddVoucher() {
        binding.btnAddVoucher.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_voucher,null)
            builder.setView(dialogView)
            val alertDialog = builder.create()
            val edtDes = dialogView.findViewById<EditText>(R.id.edtDes)
            val edtVoucherToClb = dialogView.findViewById<EditText>(R.id.edtVoucherToClb)
            val edtVoucherTiem = dialogView.findViewById<EditText>(R.id.edtVoucherTime)
            val edtVoucherCode = dialogView.findViewById<EditText>(R.id.edtVoucherCode)
            val edtVoucherValue = dialogView.findViewById<EditText>(R.id.edtVoucherValue)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
            btnConfirm.setOnClickListener {
                val des = edtDes.text.toString().trim()
                val voucherToClb = edtVoucherToClb.text.toString().trim()
                val voucherTime = edtVoucherTiem.text.toString().trim()
                val voucherCode = edtVoucherCode.text.toString().trim()
                val voucherValue = edtVoucherValue.text.toString().trim()
                val id = dbRefVoucher.push().key!!
                val dataVoucher = dataVoucher(id,des,voucherToClb,voucherTime,voucherCode,voucherValue)

                dbRefVoucher.child(id).setValue(dataVoucher)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(),"Them thanh cong!", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
            }
            alertDialog.show()
        }

    }

    private fun displayInformation() {
        val list = mutableListOf<dataOverviewOwner>()
        list.add((dataOverviewOwner(R.drawable.quan_bi_a1,"Bi-a Ông Thần Tài","Hà Nội","Đánh giá: 4 sao",
            "Tổng đặt bàn 20", "Đã xác nhận: 8", "Chờ xử lý: 2","Doanh thu 1.000.000",
            "Tổng bàn: 20", "Đang hoạt động: 5","Bàn trống: 2" ,"Bảo trì: 1")))
        list.add((dataOverviewOwner(R.drawable.quan_bi_a2,"Bi-a Ông Thần Tài","Nga Sơn","Đánh giá: 5 sao",
            "Tổng đặt bàn 30", "Đã xác nhận: 18", "Chờ xử lý: 12","Doanh thu 10.000.000",
            "Tổng bàn: 30", "Đang hoạt động: 15","Bàn trống: 12" ,"Bảo trì: 2")))
        binding.rvOverview.adapter = RvOverview(list)
        binding.rvOverview.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.HORIZONTAL,
            false
        )
    }

    override fun editVoucher(dataVoucher: dataVoucher) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_voucher,null)
        builder.setView(dialogView)
        val alertDialog = builder.create()
        val edtDes = dialogView.findViewById<EditText>(R.id.edtDes)
        val edtVoucherToClb = dialogView.findViewById<EditText>(R.id.edtVoucherToClb)
        val edtVoucherTiem = dialogView.findViewById<EditText>(R.id.edtVoucherTime)
        val edtVoucherCode = dialogView.findViewById<EditText>(R.id.edtVoucherCode)
        val edtVoucherValue = dialogView.findViewById<EditText>(R.id.edtVoucherValue)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        edtDes.setText(dataVoucher.des)
        edtVoucherToClb.setText(dataVoucher.voucherToClb)
        edtVoucherTiem.setText(dataVoucher.voucherTime)
        edtVoucherCode.setText(dataVoucher.voucherCode)
        edtVoucherValue.setText(dataVoucher.voucherValue)

        btnConfirm.setOnClickListener {
            val des = edtDes.text.toString().trim()
            val voucherToClb = edtVoucherToClb.text.toString().trim()
            val voucherTime = edtVoucherTiem.text.toString().trim()
            val voucherCode = edtVoucherCode.text.toString().trim()
            val voucherValue = edtVoucherValue.text.toString().trim()
            val id = dataVoucher.id.toString()
            val dataVoucherUpdate = dataVoucher(id, des, voucherToClb, voucherTime, voucherCode, voucherValue)
            dbRefVoucher.child(id).setValue(dataVoucherUpdate)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(),"Chinh sua thanh cong!", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }
        }
        alertDialog.show()
    }

    override fun refuseRealtime(id: String) {
        dbRefVoucher.child(id).removeValue()
        Toast.makeText(requireContext(),"Xoa thanh cong!", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRefVoucher.removeEventListener(voucherValueEventListener)
    }
}