package org.o7planning.myapplication.Customer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataStort
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.FragmentHomeBinding


class FragmentHome : Fragment(), onClickOrderOutStandingListenner {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var dbRefOutstanding: DatabaseReference
    private lateinit var listOutstanding: ArrayList<dataStort>
    private lateinit var outstandingAdapter: RvOutstanding
    private lateinit var dbRefTheBooking: DatabaseReference
    private lateinit var listBooking: ArrayList<dataTableManagement>
    private lateinit var bookingAdapter: RvTheBooking
    private lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbRefOutstanding = FirebaseDatabase.getInstance().getReference("dataStore")
        listOutstanding = arrayListOf()

        dbRefTheBooking = FirebaseDatabase.getInstance().getReference("dataBookTable")
        listBooking = arrayListOf()

        mAuth = FirebaseAuth.getInstance()


        itemLayoutTournament()
        itemLayoutTheBooking()
        itemLayoutOutstanding()
        dataTheBooking()
        dataOutStanding()
    }


    private fun itemLayoutTheBooking() {
        bookingAdapter = RvTheBooking(listBooking)

        bookingAdapter.onClickCancelOrder = {item, position ->
            if (item.status == "Chờ xử lý"){
                dialogDeleteOrder(item)
            }
        }

        binding.rvTheBooking.adapter = bookingAdapter
        binding.rvTheBooking.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.VERTICAL,
            false
        )
    }

    private fun dialogDeleteOrder(item: dataTableManagement) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Huỷ Đặt Bàn")
        builder.setMessage("Bạn có chắc chắn huỷ đặt bàn không!!!")
        builder.setPositiveButton("Đồng ý"){ dialog, wich ->
            Toast.makeText(requireContext(),"Bạn đã huỷ đặt bàn :( \n Hẹn Bạn Lần sau!", Toast.LENGTH_SHORT).show()
            deleteOders(item)
            dialog.dismiss()
        }
        builder.setNegativeButton("Huỷ"){dialog, wich ->
            dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun deleteOders(item: dataTableManagement) {
        val id = item.id.toString()
        if (id.isEmpty()){
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID đơn hàng.", Toast.LENGTH_SHORT).show()
            return
        }
        dbRefTheBooking.child(id).removeValue()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Đơn hàng đã được Hủy thành công!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Lỗi hủy đơn hàng: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun dataTheBooking() {
        val userId = mAuth.currentUser?.uid.toString()
        if (userId.isEmpty()) {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val userBookingsQuery = dbRefTheBooking.orderByChild("userId").equalTo(userId)
        userBookingsQuery.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listBooking.clear()
                if (snapshot.exists()) {
                    for (bookingSnap in snapshot.children) {
                        val bookingData = bookingSnap.getValue(dataTableManagement::class.java)
                        bookingData?.let { listBooking.add(it) }
                    }
                }
                listBooking.sortBy { data: dataTableManagement ->
                    when (data.status) {
                        "Chờ xử lý" -> 0
                        "Đã xác nhận" -> 1
                        "Đang chơi" -> 2
                        else -> 3
                    }
                }
                bookingAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Lỗi đọc dữ liệu: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }

        })

    }

    private fun dataOutStanding() {
        dbRefOutstanding.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOutstanding.clear()
                if (snapshot.exists()) {
                    for (outstandingSnap in snapshot.children) {
                        val outstandingData = outstandingSnap.getValue(dataStort::class.java)
                        outstandingData?.let { listOutstanding.add(it) }
                    }
                }
                outstandingAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu nổi bật: ${error.message}", Toast.LENGTH_SHORT).show()
            }

        })
    }

    private fun itemLayoutOutstanding() {
        outstandingAdapter = RvOutstanding(listOutstanding, this)
        binding.rvOutstanding.adapter = outstandingAdapter.apply {
            onClickItem = { item, pos ->
                handleClickOutstanding(item, pos)
            }
        }
        binding.rvOutstanding.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.VERTICAL,
            false
        )
    }

    fun itemLayoutTournament() {
        val list = mutableListOf<OutDataTournament>()
        list.add(OutDataTournament(R.drawable.icons8battle80, "Giai dau 1", "Tham gia ngay"))
        list.add(OutDataTournament(R.drawable.icons8ratings80, "Thong ke", "xem ket qua"))
        binding.rvTournament.adapter = RvTournamet(list).apply {
            onClickItem = { item, pos ->
                handleClickTournament(item, pos)
            }
        }
        binding.rvTournament.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.HORIZONTAL,
            false
        )
    }

    fun handleClickTournament(item: OutDataTournament, position: Int) {
        when (position) {
            0 -> {
                val build = AlertDialog.Builder(requireContext())
                val dialogView = layoutInflater.inflate(R.layout.dialog_tour_nament, null)
                build.setView(dialogView).show()
            }

            else -> {
                Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun handleClickOutstanding(item: dataStort, position: Int) {
        when (position) {
            else -> {
                Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.fragment_booktable)
            }
        }
    }

    override fun onClickOderOutStanding(name: String, location: String) {

    }


}