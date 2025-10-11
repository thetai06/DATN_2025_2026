package org.o7planning.myapplication.Customer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
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
import org.o7planning.myapplication.data.dataOverviewOwner
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.FragmentHomeBinding


class FragmentHome : Fragment(), onClickOrderOutStandingListenner {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var dbRefOutstanding: DatabaseReference
    private lateinit var listOutstanding: ArrayList<dataStore> // Danh sách để hiển thị
    private lateinit var listOutstandingSearch: ArrayList<dataStore> // Danh sách chứa toàn bộ dữ liệu gốc
    private lateinit var outstandingAdapter: RvOutstanding
    private lateinit var dbRefTheBooking: DatabaseReference
    private lateinit var dbRefOverview: DatabaseReference
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
        listOutstandingSearch = arrayListOf()

        dbRefOverview = FirebaseDatabase.getInstance().getReference("dataOverview")

        dbRefTheBooking = FirebaseDatabase.getInstance().getReference("dataBookTable")
        listBooking = arrayListOf()

        mAuth = FirebaseAuth.getInstance()

        itemLayoutTournament()
        itemLayoutTheBooking()
        itemLayoutOutstanding()
        dataTheBooking()
        dataOutStanding()

        setupSearchView()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText.orEmpty())
                return true
            }
        })
    }

    private fun filterList(query: String) {
        listOutstanding.clear()

        if (query.isEmpty()) {
            listOutstanding.addAll(listOutstandingSearch.take(10))
        } else {
            val filteredResults = listOutstandingSearch.filter { store ->
                val nameMatch = store.name?.contains(query, ignoreCase = true) == true
                val locationMatch = store.address?.contains(query, ignoreCase = true) == true
                nameMatch || locationMatch
            }
            listOutstanding.addAll(filteredResults)
        }
        outstandingAdapter.notifyDataSetChanged()
    }

    private fun dataOutStanding() {
        dbRefOutstanding.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOutstandingSearch.clear()
                if (snapshot.exists()) {
                    for (outstandingSnap in snapshot.children) {
                        val outstandingData = outstandingSnap.getValue(dataStore::class.java)
                        outstandingData?.let { listOutstandingSearch.add(it) }
                    }
                }
                filterList(binding.searchView.query.toString())
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
        val update = mapOf<String, Any>("status" to "Đã huỷ")
        dbRefTheBooking.child(id).updateChildren(update)
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
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy người dùng", Toast.LENGTH_SHORT).show()
            return
        }
        dbRefTheBooking.orderByChild("userId").equalTo(userId)
            .addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listBooking.clear()
                if (snapshot.exists()) {
                    for (bookingSnap in snapshot.children) {
                        val bookingData = bookingSnap.getValue(dataTableManagement::class.java)
                        if (bookingData?.status == "Chờ xử lý" || bookingData?.status == "Đã xác nhận" || bookingData?.status == "Đang chơi"){
                            bookingData.let { listBooking.add(it) }
                        }
                    }
                }
                listBooking.sortBy { data: dataTableManagement ->
                    when (data.status) {
                        "Chờ xử lý" -> 0
                        "Đã xác nhận" -> 1
                        else -> 2
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

    fun handleClickOutstanding(item: dataStore, position: Int) {
        when (position) {
            else -> {
                val builder = AlertDialog.Builder(requireContext())
                val dialogView = layoutInflater.inflate(R.layout.dialog_view_store, null)
                builder.setView(dialogView)
                builder.setCancelable(false)
                val alertDialog: AlertDialog = builder.create()

                val nameBar = dialogView.findViewById<TextView>(R.id.nameBar)
                val locationBar = dialogView.findViewById<TextView>(R.id.locationBar)
                val phoneBar = dialogView.findViewById<TextView>(R.id.phoneBar)
                val emailBar = dialogView.findViewById<TextView>(R.id.emailBar)
                val timeBar = dialogView.findViewById<TextView>(R.id.timeBar)
                val tableBar = dialogView.findViewById<TextView>(R.id.tableBar)
                val desBar = dialogView.findViewById<TextView>(R.id.desBar)
                val btnExit = dialogView.findViewById<ImageButton>(R.id.btnExit)
                btnExit.setOnClickListener {
                    alertDialog.dismiss()
                }
                dbRefOverview.child(item.storeId.toString()).addValueEventListener(object : ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()){
                            val overviewData = snapshot.getValue(dataOverviewOwner::class.java)
                            nameBar.text = overviewData?.name.toString()
                            locationBar.text = overviewData?.location.toString()
                            phoneBar.text = "Số điệt thoại: ${item.phone}"
                            emailBar.text = "Email: ${item.email}"
                            timeBar.text = "Thời gian hoạt động: ${overviewData?.openingHour.toString()} - ${overviewData?.closingHour.toString()}"
                            tableBar.text = "Số lượng bàn: ${overviewData?.sumTable.toString()}"
                            desBar.text = item.des

                            alertDialog.show()
                        } else {
                            Toast.makeText(requireContext(), "Không tìm thấy dữ liệu chi tiết cho cửa hàng này.", Toast.LENGTH_LONG).show()
                        }
                        }
                    override fun onCancelled(error: DatabaseError) {
                    }
                })
            }
        }
    }

    override fun onClickOderOutStanding(name: String, location: String) {
        val action = FragmentHomeDirections.actionFragmentHomeToFragmentBooktable(name, location)
        findNavController().navigate(action)
    }


}