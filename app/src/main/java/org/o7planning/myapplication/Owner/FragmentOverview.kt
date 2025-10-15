package org.o7planning.myapplication.Owner

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
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
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.data.dataStoreDisplayInfo
import org.o7planning.myapplication.data.dataOverviewOwner
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.data.dataVoucher
import org.o7planning.myapplication.databinding.FragmentOverviewBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FragmentOverview : Fragment(), onVoucherRealtimeClick {

    private lateinit var binding: FragmentOverviewBinding
    private lateinit var mAuth: FirebaseAuth

    private lateinit var dbRefVoucher: DatabaseReference
    private lateinit var dbRefOverview: DatabaseReference
    private lateinit var dbRefStore: DatabaseReference
    private lateinit var dbRefBooktable: DatabaseReference

    private lateinit var voucherAdapter: RvVoucherOverView
    private lateinit var overviewAdapter: RvOverview

    private lateinit var listVoucher: ArrayList<dataVoucher>
    private lateinit var listOverviewDisplay: ArrayList<dataStoreDisplayInfo>
    private var ownerStores: List<dataStore> = listOf()
    private var allOverviews: List<dataOverviewOwner> = listOf()

    private lateinit var voucherListener: ValueEventListener
    private lateinit var storesListener: ValueEventListener
    private lateinit var overviewsListener: ValueEventListener

    private val statisticsListeners = mutableMapOf<String, ValueEventListener>()

    private var ownerId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentOverviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeFirebase()
        initializeListsAndAdapters()
        setupRecyclerViews()
        setUpAddVoucher()

        attachDataListeners()
    }

    private fun initializeFirebase() {
        mAuth = FirebaseAuth.getInstance()
        ownerId = mAuth.currentUser?.uid

        dbRefVoucher = FirebaseDatabase.getInstance().getReference("dataVoucher")
        dbRefOverview = FirebaseDatabase.getInstance().getReference("dataOverview")
        dbRefStore = FirebaseDatabase.getInstance().getReference("dataStore")
        dbRefBooktable = FirebaseDatabase.getInstance().getReference("dataBookTable")
    }

    private fun initializeListsAndAdapters() {
        listVoucher = arrayListOf()
        listOverviewDisplay = arrayListOf()

        voucherAdapter = RvVoucherOverView(listVoucher, this)
        overviewAdapter = RvOverview(listOverviewDisplay)
    }

    private fun setupRecyclerViews() {
        binding.rvVoucherOverview.apply {
            adapter = voucherAdapter
            layoutManager = GridLayoutManager(
                requireContext(),
                1,
                GridLayoutManager.VERTICAL,
                false)
        }
        binding.rvOverview.apply {
            adapter = overviewAdapter
            layoutManager = GridLayoutManager(
                requireContext(),
                1,
                GridLayoutManager.HORIZONTAL,
                false)
        }
    }

    private fun setupStatisticsListener(store: dataStore) {
        val currentStoreId = store.storeId ?: return
        val totalTables = store.tableNumber?.toIntOrNull() ?: 0

        val calendar = Calendar.getInstance()
        val todayDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalBookingsForDay = 0
                var processingCount = 0
                var confirmedCount = 0
                var playingCount = 0
                var completedCount = 0
                var refusedCount = 0
                var totalRevenue = 0.0

                if (snapshot.exists()) {
                    for (bookingSnap in snapshot.children) {
                        val booking = bookingSnap.getValue(dataTableManagement::class.java)

                        if (booking != null && booking.dateTime == todayDate) {
                            totalBookingsForDay++
                            when (booking.status) {
                                "Chờ xử lý" -> processingCount++
                                "Đã xác nhận" -> confirmedCount++
                                "Đang chơi" -> {
                                    confirmedCount++
                                    playingCount++
                                }
                                "Đã hoàn thành" -> {
                                    confirmedCount++
                                    completedCount++
                                    val moneyString = booking.money?.replace(Regex("[^\\d.]"), "")
                                    totalRevenue += moneyString?.toDoubleOrNull() ?: 0.0
                                }
                                "Đã từ chối" -> refusedCount++
                            }
                        }
                    }
                }

                val tableEmpty = totalTables - playingCount

                val overviewDataMap = mapOf<String, Any>(
                    "storeId" to currentStoreId,
                    "ownerId" to (ownerId ?: ""),
                    "profit" to totalRevenue,
                    "tableEmpty" to tableEmpty,
                    "pendingBookings" to processingCount,
                    "confirm" to confirmedCount,
                    "tableActive" to playingCount,
                    "totalBooking" to totalBookingsForDay,
                    "maintenance" to refusedCount
                )

                dbRefOverview.child(currentStoreId).updateChildren(overviewDataMap)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("DailyStats", "Lỗi tải dữ liệu đặt bàn cho quán $currentStoreId: ${error.message}")
            }
        }
        statisticsListeners[currentStoreId] = listener
        dbRefBooktable.orderByChild("storeId").equalTo(currentStoreId).addValueEventListener(listener)
    }

    private fun attachDataListeners() {
        voucherListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listVoucher.clear()
                for (voucherSnap in snapshot.children) {
                    voucherSnap.getValue(dataVoucher::class.java)?.let { listVoucher.add(it) }
                }
                voucherAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRefVoucher.addValueEventListener(voucherListener)

        storesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                removeStatisticsListeners()
                ownerStores = snapshot.children.mapNotNull { it.getValue(dataStore::class.java) }
                ownerStores.forEach { store ->
                    setupStatisticsListener(store)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRefStore.orderByChild("ownerId").equalTo(ownerId).addValueEventListener(storesListener)

        overviewsListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allOverviews = snapshot.children.mapNotNull { it.getValue(dataOverviewOwner::class.java) }
                combineAndRefreshUI()
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        dbRefOverview.orderByChild("ownerId").equalTo(ownerId).addValueEventListener(overviewsListener)
    }

    private fun combineAndRefreshUI() {
        val combinedList = ArrayList<dataStoreDisplayInfo>()

        for (store in ownerStores) {
            val overview = allOverviews.find { it.storeId == store.storeId }
            val totalTables = store.tableNumber?.toIntOrNull() ?: 0

            val displayInfo = dataStoreDisplayInfo(
                storeId = store.storeId, ownerId = store.ownerId, name = store.name, address = store.address,
                tableNumber = store.tableNumber, profit = overview?.profit ?: 0.0,
                tableEmpty = overview?.tableEmpty ?: totalTables,
                pendingBookings = overview?.pendingBookings ?: 0, confirm = overview?.confirm ?: 0,
                tableActive = overview?.tableActive ?: 0, totalBooking = overview?.totalBooking ?: 0,
                maintenance = overview?.maintenance ?: 0
            )
            combinedList.add(displayInfo)
        }

        listOverviewDisplay.clear()
        listOverviewDisplay.addAll(combinedList)
        overviewAdapter.notifyDataSetChanged()
    }

    private fun setUpAddVoucher() {
        binding.btnAddVoucher.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            val dialogView = layoutInflater.inflate(R.layout.dialog_add_voucher, null)
            builder.setView(dialogView)
            val alertDialog = builder.create()

            val edtDes = dialogView.findViewById<EditText>(R.id.edtDes)
            val tvVoucherTimeStart = dialogView.findViewById<TextView>(R.id.edtVoucherTimeStart)
            val tvVoucherTimeEnd = dialogView.findViewById<TextView>(R.id.edtVoucherTimeEnd)
            val edtVoucherCode = dialogView.findViewById<EditText>(R.id.edtVoucherCode)
            val edtVoucherValue = dialogView.findViewById<EditText>(R.id.edtVoucherValue)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

            val startDateCalendar = Calendar.getInstance()
            val endDateCalendar = Calendar.getInstance()

            tvVoucherTimeStart.text = "Chọn ngày"
            tvVoucherTimeEnd.text = "Chọn ngày"

            tvVoucherTimeStart.setOnClickListener {
                showDatePickerDialog(startDateCalendar, tvVoucherTimeStart)
            }
            tvVoucherTimeEnd.setOnClickListener {
                showDatePickerDialog(endDateCalendar, tvVoucherTimeEnd)
            }

            btnConfirm.setOnClickListener {
                val des = edtDes.text.toString().trim()
                val voucherTimeStart = tvVoucherTimeStart.text.toString().trim()
                val voucherTimeEnd = tvVoucherTimeEnd.text.toString().trim()
                val voucherCode = edtVoucherCode.text.toString().trim()
                val voucherValue = edtVoucherValue.text.toString().trim()

                if (voucherTimeStart == "Chọn ngày" || voucherTimeEnd == "Chọn ngày") {
                    Toast.makeText(requireContext(), "Vui lòng chọn ngày bắt đầu và kết thúc!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (endDateCalendar.before(startDateCalendar)) {
                    Toast.makeText(requireContext(), "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val id = dbRefVoucher.push().key ?: return@setOnClickListener

                val newVoucher = dataVoucher(id, des, voucherTimeStart, voucherTimeEnd, voucherCode, voucherValue)
                dbRefVoucher.child(id).setValue(newVoucher).addOnSuccessListener {
                    Toast.makeText(requireContext(), "Thêm voucher thành công!", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }
            }
            alertDialog.show()
        }
    }

    override fun editVoucher(dataVoucher: dataVoucher) {
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_voucher, null)
        builder.setView(dialogView)
        val alertDialog = builder.create()

        val edtDes = dialogView.findViewById<EditText>(R.id.edtDes)
        val tvVoucherTimeStart = dialogView.findViewById<TextView>(R.id.edtVoucherTimeStart)
        val tvVoucherTimeEnd = dialogView.findViewById<TextView>(R.id.edtVoucherTimeEnd)
        val edtVoucherCode = dialogView.findViewById<EditText>(R.id.edtVoucherCode)
        val edtVoucherValue = dialogView.findViewById<EditText>(R.id.edtVoucherValue)
        val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

        val startDateCalendar = Calendar.getInstance()
        val endDateCalendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        edtDes.setText(dataVoucher.des)
        edtVoucherCode.setText(dataVoucher.voucherCode)
        edtVoucherValue.setText(dataVoucher.voucherValue)

        try {
            if (dataVoucher.voucherTimeStart?.isNotEmpty() == true) {
                startDateCalendar.time = dateFormat.parse(dataVoucher.voucherTimeStart!!)!!
                tvVoucherTimeStart.text = dataVoucher.voucherTimeStart
            } else {
                tvVoucherTimeStart.text = "Chọn ngày"
            }
            if (dataVoucher.voucherTimeEnd?.isNotEmpty() == true) {
                endDateCalendar.time = dateFormat.parse(dataVoucher.voucherTimeEnd!!)!!
                tvVoucherTimeEnd.text = dataVoucher.voucherTimeEnd
            } else {
                tvVoucherTimeEnd.text = "Chọn ngày"
            }
        } catch (e: Exception) {
            tvVoucherTimeStart.text = "Chọn ngày"
            tvVoucherTimeEnd.text = "Chọn ngày"
        }

        tvVoucherTimeStart.setOnClickListener {
            showDatePickerDialog(startDateCalendar, tvVoucherTimeStart)
        }
        tvVoucherTimeEnd.setOnClickListener {
            showDatePickerDialog(endDateCalendar, tvVoucherTimeEnd)
        }

        btnConfirm.setOnClickListener {
            val des = edtDes.text.toString().trim()
            val voucherTimeStart = tvVoucherTimeStart.text.toString().trim()
            val voucherTimeEnd = tvVoucherTimeEnd.text.toString().trim()
            val voucherCode = edtVoucherCode.text.toString().trim()
            val voucherValue = edtVoucherValue.text.toString().trim()
            val id = dataVoucher.id.toString()

            if (voucherTimeStart == "Chọn ngày" || voucherTimeEnd == "Chọn ngày") {
                Toast.makeText(requireContext(), "Vui lòng chọn ngày bắt đầu và kết thúc!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (endDateCalendar.before(startDateCalendar)) {
                Toast.makeText(requireContext(), "Ngày kết thúc phải sau hoặc bằng ngày bắt đầu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dataVoucherUpdate = dataVoucher(id, des, voucherTimeStart, voucherTimeEnd, voucherCode, voucherValue)
            dbRefVoucher.child(id).setValue(dataVoucherUpdate)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Chỉnh sửa thành công!", Toast.LENGTH_SHORT).show()
                    alertDialog.dismiss()
                }
        }
        alertDialog.show()
    }

    override fun refuseRealtime(id: String, voucher: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Xoá Voucher $voucher")
        builder.setMessage("Bạn có chắc chắn muốn xoá voucher $voucher không?")
        builder.setPositiveButton("Đồng ý") { dialog, _ ->
            dbRefVoucher.child(id).removeValue()
            Toast.makeText(requireContext(), "Bạn đã xoá voucher $voucher", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("Không") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun showDatePickerDialog(calendar: Calendar, targetTextView: TextView) {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            targetTextView.text = dateFormat.format(calendar.time)
        }

        val dialog = DatePickerDialog(
            requireContext(), dateSetListener,
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)
        )
        dialog.datePicker.minDate = System.currentTimeMillis() - 1000
        dialog.show()
    }

    private fun removeStatisticsListeners() {
        statisticsListeners.forEach { (storeId, listener) ->
            dbRefBooktable.orderByChild("storeId").equalTo(storeId).removeEventListener(listener)
        }
        statisticsListeners.clear()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (this::voucherListener.isInitialized) {
            dbRefVoucher.removeEventListener(voucherListener)
        }
        if (this::storesListener.isInitialized && ownerId != null) {
            dbRefStore.orderByChild("ownerId").equalTo(ownerId).removeEventListener(storesListener)
        }
        if (this::overviewsListener.isInitialized && ownerId != null) {
            dbRefOverview.orderByChild("ownerId").equalTo(ownerId).removeEventListener(overviewsListener)
        }
        removeStatisticsListeners()
    }
}