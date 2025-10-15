package org.o7planning.myapplication.Customer

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataOverviewOwner
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.data.dataStoreDisplayInfo // Đảm bảo bạn import đúng class này
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.FragmentHomeBinding


class FragmentHome : Fragment(), onClickOrderOutStandingListenner {

    private lateinit var binding: FragmentHomeBinding
    private lateinit var mAuth: FirebaseAuth

    private lateinit var dbRefStores: DatabaseReference
    private lateinit var dbRefOverviews: DatabaseReference
    private lateinit var dbRefTheBooking: DatabaseReference
    private lateinit var bookingQuery: Query

    private lateinit var outstandingAdapter: RvOutstanding
    private lateinit var bookingAdapter: RvTheBooking

    private lateinit var listOutstanding: ArrayList<dataStoreDisplayInfo>
    private lateinit var listOutstandingSearch: ArrayList<dataStoreDisplayInfo>
    private lateinit var listBooking: ArrayList<dataTableManagement>

    private var allStores: List<dataStore> = listOf()
    private var allOverviews: List<dataOverviewOwner> = listOf()

    private lateinit var storesListener: ValueEventListener
    private lateinit var overviewsListener: ValueEventListener
    private lateinit var bookingListener: ValueEventListener

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFirebase()
        initializeListsAndAdapters()
        setupRecyclerViews()
        attachDataListeners()
        setupSearchView()
    }

    private fun initializeFirebase() {
        mAuth = FirebaseAuth.getInstance()
        dbRefStores = FirebaseDatabase.getInstance().getReference("dataStore")
        dbRefOverviews = FirebaseDatabase.getInstance().getReference("dataOverview")
        dbRefTheBooking = FirebaseDatabase.getInstance().getReference("dataBookTable")
    }

    private fun initializeListsAndAdapters() {
        listOutstanding = arrayListOf()
        listOutstandingSearch = arrayListOf()
        outstandingAdapter = RvOutstanding(listOutstanding, this)
        outstandingAdapter.onClickItem = { item, _ ->
            handleClickOutstanding(item)
        }

        listBooking = arrayListOf()
        bookingAdapter = RvTheBooking(listBooking)
        bookingAdapter.onClickCancelOrder = { item, _ ->
            if (item.status == "Chờ xử lý") {
                dialogDeleteOrder(item)
            }
        }
    }

    private fun setupRecyclerViews() {
        binding.rvOutstanding.apply {
            adapter = outstandingAdapter
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
        }
        binding.rvTheBooking.apply {
            adapter = bookingAdapter
            layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.VERTICAL, false)
        }
        val tournamentList = mutableListOf<OutDataTournament>()
        tournamentList.add(OutDataTournament(R.drawable.icons8battle80, "Giai dau 1", "Tham gia ngay"))
        tournamentList.add(OutDataTournament(R.drawable.icons8ratings80, "Thong ke", "xem ket qua"))
        binding.rvTournament.adapter = RvTournamet(tournamentList).apply {
            onClickItem = { item, pos -> handleClickTournament(item, pos) }
        }
        binding.rvTournament.layoutManager =
            GridLayoutManager(requireContext(), 1, GridLayoutManager.HORIZONTAL, false)
    }

    private fun attachDataListeners() {
        storesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allStores = snapshot.children.mapNotNull { it.getValue(dataStore::class.java) }
                combineAndRefreshUI()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Lỗi tải danh sách cửa hàng", Toast.LENGTH_SHORT).show()
            }
        }
        dbRefStores.addValueEventListener(storesListener)

        overviewsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allOverviews = snapshot.children.mapNotNull { it.getValue(dataOverviewOwner::class.java) }
                combineAndRefreshUI()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Lỗi tải dữ liệu tổng quan", Toast.LENGTH_SHORT).show()
            }
        }
        dbRefOverviews.addValueEventListener(overviewsListener)

        dataTheBooking()
    }

    private fun combineAndRefreshUI() {
        val combinedList = ArrayList<dataStoreDisplayInfo>()

        for (store in allStores) {
            val overview = allOverviews.find { it.storeId == store.storeId }
            val totalTables = store.tableNumber?.toIntOrNull() ?: 0

            val displayInfo = dataStoreDisplayInfo(
                storeId = store.storeId,
                ownerId = store.ownerId,
                name = store.name,
                address = store.address,
                tableNumber = store.tableNumber,
                phone = store.phone,
                email = store.email,
                des = store.des,
                openingHour = store.openingHour,
                closingHour = store.closingHour,
                profit = overview?.profit ?: 0.0,
                tableEmpty = overview?.tableEmpty ?: totalTables,
                pendingBookings = overview?.pendingBookings ?: 0,
                confirm = overview?.confirm ?: 0,
                tableActive = overview?.tableActive ?: 0,
                totalBooking = overview?.totalBooking ?: 0,
                maintenance = overview?.maintenance ?: 0
            )
            combinedList.add(displayInfo)
        }

        listOutstandingSearch.clear()
        listOutstandingSearch.addAll(combinedList)

        filterList(binding.searchView.query.toString())
    }

    private fun filterList(query: String) {
        listOutstanding.clear()
        val results = if (query.isEmpty()) {
            listOutstandingSearch.take(10)
        } else {
            listOutstandingSearch.filter { displayInfo ->
                val nameMatch = displayInfo.name?.contains(query, ignoreCase = true) == true
                val locationMatch = displayInfo.address?.contains(query, ignoreCase = true) == true
                nameMatch || locationMatch
            }
        }
        listOutstanding.addAll(results)
        outstandingAdapter.notifyDataSetChanged()
    }

    fun handleClickOutstanding(item: dataStoreDisplayInfo) {
        val storeId = item.storeId
        if (storeId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID cửa hàng.", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_view_store, null)
        val alertDialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val nameBar = dialogView.findViewById<TextView>(R.id.nameBar)
        val locationBar = dialogView.findViewById<TextView>(R.id.locationBar)
        val phoneBar = dialogView.findViewById<TextView>(R.id.phoneBar)
        val emailBar = dialogView.findViewById<TextView>(R.id.emailBar)
        val timeBar = dialogView.findViewById<TextView>(R.id.timeBar)
        val tableBar = dialogView.findViewById<TextView>(R.id.tableBar)
        val desBar = dialogView.findViewById<TextView>(R.id.desBar)
        val btnExit = dialogView.findViewById<ImageButton>(R.id.btnExit)

        lateinit var dialogOverviewListener: ValueEventListener

        val overviewRef = dbRefOverviews.child(storeId)

        dialogOverviewListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val overviewData = snapshot.getValue(dataOverviewOwner::class.java)

                nameBar.text = item.name
                locationBar.text = item.address
                phoneBar.text = "Số điện thoại: ${item.phone}"
                emailBar.text = "Email: ${item.email}"
                timeBar.text = "Thời gian hoạt động: ${item.openingHour} - ${item.closingHour}"
                desBar.text = item.des

                val tableEmpty = overviewData?.tableEmpty ?: item.tableNumber?.toIntOrNull() ?: 0
                tableBar.text = "Bàn trống: ${tableEmpty} / ${item.tableNumber}"
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Lỗi tải dữ liệu chi tiết.", Toast.LENGTH_SHORT).show()
            }
        }

        overviewRef.addValueEventListener(dialogOverviewListener)

        alertDialog.setOnDismissListener {
            overviewRef.removeEventListener(dialogOverviewListener)
        }

        btnExit.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText.orEmpty())
                return true
            }
        })
    }

    private fun dataTheBooking() {
        val userId = mAuth.currentUser?.uid
        if (userId == null) {
            listBooking.clear()
            bookingAdapter.notifyDataSetChanged()
            return
        }
        bookingQuery = dbRefTheBooking.orderByChild("userId").equalTo(userId)
        bookingListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listBooking.clear()
                if (snapshot.exists()) {
                    val tempBookingList = mutableListOf<dataTableManagement>()
                    for (bookingSnap in snapshot.children) {
                        val bookingData = bookingSnap.getValue(dataTableManagement::class.java)
                        if (bookingData != null && bookingData.status in listOf("Chờ xử lý", "Đã xác nhận", "Đang chơi")) {
                            tempBookingList.add(bookingData)
                        }
                    }
                    tempBookingList.sortBy { data ->
                        when (data.status) {
                            "Chờ xử lý" -> 0
                            "Đã xác nhận" -> 1
                            else -> 2
                        }
                    }
                    listBooking.addAll(tempBookingList)
                }
                bookingAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Lỗi đọc dữ liệu đặt bàn: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
        bookingQuery.addValueEventListener(bookingListener)
    }

    private fun dialogDeleteOrder(item: dataTableManagement) {
        AlertDialog.Builder(requireContext())
            .setTitle("Huỷ Đặt Bàn")
            .setMessage("Bạn có chắc chắn muốn huỷ lịch đặt bàn này không?")
            .setPositiveButton("Đồng ý") { dialog, _ ->
                deleteOrder(item)
                dialog.dismiss()
            }
            .setNegativeButton("Không") { dialog, _ -> dialog.dismiss() }
            .create()
            .show()
    }

    private fun deleteOrder(item: dataTableManagement) {
        val id = item.id
        if (id.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Lỗi: Không tìm thấy ID đơn hàng.", Toast.LENGTH_SHORT).show()
            return
        }
        val update = mapOf<String, Any>("status" to "Đã huỷ")
        dbRefTheBooking.child(id).updateChildren(update)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Bạn đã huỷ đặt bàn thành công!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Lỗi hủy đơn hàng: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    fun handleClickTournament(item: OutDataTournament, position: Int) {
        when (position) {
            0 -> {
                val dialogView = layoutInflater.inflate(R.layout.dialog_tour_nament, null)
                AlertDialog.Builder(requireContext()).setView(dialogView).show()
            }
            else -> Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onClickOderOutStanding(
        name: String, location: String, storeId: String, ownerId: String
    ) {
        val action = FragmentHomeDirections.actionFragmentHomeToFragmentBooktable(name, location, storeId, ownerId)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::storesListener.isInitialized) {
            dbRefStores.removeEventListener(storesListener)
        }
        if (this::overviewsListener.isInitialized) {
            dbRefOverviews.removeEventListener(overviewsListener)
        }
        if (this::bookingListener.isInitialized && this::bookingQuery.isInitialized) {
            bookingQuery.removeEventListener(bookingListener)
        }
    }
}