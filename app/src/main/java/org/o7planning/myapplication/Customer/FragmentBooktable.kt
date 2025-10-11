package org.o7planning.myapplication.Customer

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.Spinner
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
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.databinding.FragmentBooktableBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FragmentBooktable : Fragment(), onOrderClickListener {
    private lateinit var dbRefBooktable: DatabaseReference
    private lateinit var dbRefStore: DatabaseReference
    private lateinit var dbRefOverview: DatabaseReference
    private lateinit var listStore: ArrayList<dataStore>
    private lateinit var storeValueEventListener: ValueEventListener
    private lateinit var listBooking: ArrayList<dataTableManagement>
    private lateinit var storeAdapter: RvClbBia
    private lateinit var mAuth: FirebaseAuth
    private lateinit var dbRefUser: DatabaseReference

    private var storeId:String? = null
    private var storeOwnerId: String? = null
    private var userId: String? = null
    private var userName: String? = null
    private var nameCLB: String? = null
    private var dataGame: String? = null
    private var dataDate: String? = null
    private var dataStartTime: String? = null
    private var dataEndTime: String? = null
    private var dataPeople: String? = null
    private var dataLocation: String? = null
    private var dataVoucher: String? = null

    private var phoneNumberUser: String? = null
    private var emailUser: String? = null

    private var totalTables: String? = null
    private var openingHour: String? = null
    private var closingHour: String? = null
    private var totalPrice: Double? = 0.0

    private lateinit var binding: FragmentBooktableBinding
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBooktableBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbRefStore = FirebaseDatabase.getInstance().getReference("dataStore")
        listStore = arrayListOf()

        dbRefBooktable = FirebaseDatabase.getInstance().getReference("dataBookTable")
        listBooking = arrayListOf()

        dbRefOverview = FirebaseDatabase.getInstance().getReference("dataOverview")

        mAuth = FirebaseAuth.getInstance()
        userId = mAuth.currentUser?.uid
        dbRefUser = FirebaseDatabase.getInstance().getReference("dataUser").child(userId!!)
        binding.btnConfirmBooking.isEnabled = false

        loadUserinformation()

        boxSelectGameType()
        boxSelectDate()
        boxSelectPeopel()
        boxSelectTime()
        boxSelectOutstandingCLB()
        boxVoucher()

        binding.btnConfirmBooking.setOnClickListener {
            if (dataStartTime != null && dataEndTime != null && dataDate != null && dataPeople != null && dataLocation != null && validateExistingTimeSelection()) {
                addDataOrder()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Vui lòng kiểm tra lại thông tin đặt bàn!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        updatePrice()
    }

    private fun loadUserinformation() {
        dbRefUser.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userName = snapshot.child("name").getValue(String::class.java)
                phoneNumberUser = snapshot.child("phoneNumber").getValue(String::class.java)
                emailUser = snapshot.child("email").getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun addDataOrder() {
        binding.apply {
            val name = userName.toString()
            val phoneNumber = phoneNumberUser.toString()
            val email = emailUser.toString()
            val startTime = dataStartTime.toString()
            val endTime = dataEndTime.toString()
            val dateTime = dataDate.toString()
            val person = dataPeople.toString()
            val address = dataLocation.toString()
            val money = PrepareTheBill.text.toString()
            val status = "Chờ xử lý"
            val idBooking = dbRefBooktable.push().key.toString()
            val dataOder = dataTableManagement(
                idBooking,
                userId,
                storeOwnerId,
                storeId,
                phoneNumber,
                email,
                name,
                startTime,
                endTime,
                dateTime,
                person,
                money,
                status,
                address
            )
            dbRefBooktable.child(idBooking).setValue(dataOder)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Thanh cong", Toast.LENGTH_SHORT).show()
                    dialogPaymentMethod()
                    if (dataDate != null && dataLocation != null) {
                        updateDailyStatistics(dataDate!!, dataLocation!!)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "khong thanh cong", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun dialogPaymentMethod() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogview = layoutInflater.inflate(R.layout.dialog_payment_method,null)
        builder.setView(dialogview)
        builder.setCancelable(false)
        val alertDialog: AlertDialog = builder.create()

        val price = dialogview.findViewById<TextView>(R.id.tvPrice)
        val rbWallet = dialogview.findViewById<RadioButton>(R.id.rbWallet)
        val rbCash = dialogview.findViewById<RadioButton>(R.id.rbCash)
        val btnCancel = dialogview.findViewById<Button>(R.id.btnCancel)
        val btnPay = dialogview.findViewById<Button>(R.id.btnPay)

        price.text = totalPrice.toString()

        btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }
        btnPay.setOnClickListener {
            alertDialog.dismiss()
            Toast.makeText(requireContext(),"Thanh toán thành công!", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.fragment_home)
        }
        alertDialog.show()
    }

    private fun calculateTablePrice(
        dataGame: String?,
        dataDate: String?,
        dataStartTime: String,
        dataEndTime: String,
        dataPeople: String?,
        dataLocation: String?,
        dataVoucher: String?
    ): Double {
        val basePricePerPerson = 60.0
        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startDate = dateFormat.parse(dataStartTime)
        val endDate = dateFormat.parse(dataEndTime)
        val dataGame = dataGame?.toLowerCase()

        // Tính toán độ dài giữa hai thời điểm (tính bằng giờ)
        val durationInMillis = endDate.time - startDate.time
        val totalHours =
            durationInMillis / (1000 * 60 * 60).toDouble() // Chuyển đổi từ mili giây sang giờ

        // Tính số người
        val numPeople = dataPeople?.toIntOrNull() ?: 1

        // Tính tổng tiền
        var totalPrice = basePricePerPerson * numPeople * totalHours

        if (dataLocation == "Hà Nam") {
            totalPrice *= 0.5
        }

        if (dataGame == "cutthroat") {
            totalPrice *= 0.2
        }
        if (!dataVoucher.isNullOrEmpty()) {
            totalPrice *= 0.9
        }

        return totalPrice
    }


    private fun boxVoucher() {
        val voucher = arguments?.getString("voucher")
        dataVoucher = voucher
        binding.edtDiscountCode.setText(dataVoucher)
        updatePrice()
    }


    private fun boxSelectTime() {
        binding.ibLogoSelectTimeStart.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                TimePickerDialog.OnTimeSetListener { timePicker, hourOfDay, minute ->
                    val selectedStartTime = String.format("%02d:%02d", hourOfDay, minute)
                    dataStartTime = selectedStartTime
                    binding.textViewTimeStart.text = dataStartTime
                    val newHour = (hourOfDay + 1) % 24
                    val formattedEndTime = String.format("%02d:%02d", newHour, minute)
                    dataEndTime = formattedEndTime
                    binding.textViewTimeEnd.text = dataEndTime
                    binding.txtTime.text = "Thời gian: $dataStartTime - $dataEndTime"
                    updatePrice()
                },
                12,
                0,
                true
            ).show()
        }
        binding.ibLogoSelectTimeEnd.setOnClickListener {
            TimePickerDialog(
                requireContext(),
                TimePickerDialog.OnTimeSetListener { timePicker, hourOfDay, minute ->
                    val selectedEndTime = String.format("%02d:%02d", hourOfDay, minute)
                    if (dataStartTime != null && dataDate != null) {
                        val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                        val startDate = dateFormat.parse(dataStartTime)
                        val endDate = dateFormat.parse(selectedEndTime)
                        val durationInMillis = endDate.time - startDate.time
                        if (durationInMillis >= 3600000) {
                            dataEndTime = selectedEndTime
                            binding.textViewTimeEnd.text = dataEndTime
                            binding.txtTime.text = "Thời gian: $dataStartTime - $dataEndTime"
                            updatePrice()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Thời gian kết thúc phải lớn hơn thời gian bắt đầu ít nhất 1 giờ",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Vui lòng chọn thời gian bắt đầu và ngày",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                1,
                0,
                true
            ).show()
        }

    }


    private fun boxSelectDate() {
        binding.ibLogoSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                requireContext(),{ datePicker, year, month, dayOfMonth ->
                    val selectDate = "$dayOfMonth / ${month + 1} / $year"
                    binding.tvDate.text = selectDate
                    selectDate(selectDate)
                    updatePrice()
                },
                currentYear,
                currentMonth,
                currentDay
            )
            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            datePickerDialog.show()
        }
    }

    private fun selectDate(date: String) {
        dataDate = date
        binding.txtDateTime.text = "Ngày: " + dataDate
    }

    private fun boxSelectCLB() {
        storeAdapter = RvClbBia(listStore, this)
        binding.rvClbBia.adapter = storeAdapter.apply {
            onClickItem = { item, pos ->
                dialogViewStore(item)
            }
        }
        binding.rvClbBia.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.VERTICAL,
            false
        )

        storeValueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listStore.clear()
                if (snapshot.exists()) {
                    for (storeSnap in snapshot.children) {
                        val stortData = storeSnap.getValue(dataStore::class.java)
                        stortData?.let { listStore.add(it) }
                    }
                }
                storeAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        dbRefStore.addValueEventListener(storeValueEventListener)
    }

    private fun dialogViewStore(item: dataStore){
        val builder = AlertDialog.Builder(requireContext())
        val dialogView = layoutInflater.inflate(R.layout.dialog_view_store,null)
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

    override fun onOrderClick(id: String, ownerId: String, name: String, location: String) {
        storeId = id
        storeOwnerId = ownerId
        nameCLB = name
        binding.txtSelectClb.text = "Quán: $name \n Cơ sở: $location"
        dataLocation = location
        loadTableCountForSelectedStore(location)
        updatePrice()
    }

    private fun loadTableCountForSelectedStore(location: String) {
        dbRefStore.orderByChild("address").equalTo(location)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val storeData =
                            snapshot.children.firstOrNull()?.getValue(dataStore::class.java)
                        totalTables = storeData?.tableNumber
                        openingHour = storeData?.openingHour
                        closingHour = storeData?.closingHour
                        updatePrice()
                    } else {
                        totalTables = "0"
                        updatePrice()
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                }

            })
    }

    private fun isTimeWithInOperatingHour(selectedTime: String): Boolean{
        if (openingHour == null || closingHour == null){
            return true
        }
        return try {
            val timeFormat = android.icu.text.SimpleDateFormat("HH:mm", Locale.getDefault())
            val selectedDate = timeFormat.parse(selectedTime)
            val openingDate = timeFormat.parse(openingHour)
            val closingDate = timeFormat.parse(closingHour)

            !selectedDate.before(openingDate) && !selectedDate.after(closingDate)
        }catch (e: Exception) {
            true
        }
    }

    private fun validateExistingTimeSelection(): Boolean {
        if (openingHour == null || closingHour == null) {
            return true
        }

        if (dataStartTime != null && !isTimeWithInOperatingHour(dataStartTime!!)) {
            Toast.makeText(
                requireContext(),
                "Giờ bắt đầu ($dataStartTime) nằm ngoài giờ hoạt động ($openingHour - $closingHour)",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }

        if (dataEndTime != null && !isTimeWithInOperatingHour(dataEndTime!!)) {
            Toast.makeText(
                requireContext(),
                "Giờ kết thúc ($dataEndTime) nằm ngoài giờ hoạt động ($openingHour - $closingHour)",
                Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    private fun boxSelectOutstandingCLB() {
        val name = arguments?.getString("name")
        val location = arguments?.getString("location")
        storeOwnerId = arguments?.getString("id")
        binding.txtSelectClb.text = "Quán: $name \n Cơ sở: $location"
        dataLocation = location
        nameCLB = name

        if (name != null) {
            binding.boxClbBia.visibility = View.GONE
        }
        boxSelectCLB()
        updatePrice()
    }

    private fun boxSelectPeopel() {
        val tables = listOf("1 người", "2 người", "3 người", "4 người")
        val spinnerTime: Spinner = binding.spinnerPeopel
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tables)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTime.adapter = adapter
        spinnerTime.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                p0: AdapterView<*>,
                p1: View?,
                p2: Int,
                p3: Long
            ) {
                val selectItemPeople = p0.getItemAtPosition(p2)?.toString() ?: "Không có lựa chọn"
                dataPeople = selectItemPeople
                binding.txtManyPeoPle.text = "Số lượng: $dataPeople người"
                updatePrice()
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {
            }

        })
    }


    private fun boxSelectGameType() {
        val list = mutableListOf<OutDataBookTable>()
        list.add(OutDataBookTable("8-ball"))
        list.add(OutDataBookTable("9-ball"))
        list.add(OutDataBookTable("Straight Pool"))
        list.add(OutDataBookTable("Carom"))
        list.add(OutDataBookTable("Cutthroat"))
        binding.rvOrderGame.adapter = RvBooktable(list).apply {
            onItemClick = { item, pos ->
                setGameType(item.name)
            }
        }
        binding.rvOrderGame.layoutManager = GridLayoutManager(
            requireContext(),
            2,
            GridLayoutManager.VERTICAL,
            false
        )
    }

    private fun updatePrice() {
        if (dataStartTime != null && dataEndTime != null && dataDate != null
            && dataPeople != null && dataLocation != null
        ) {

            totalPrice = calculateTablePrice(
                dataGame,
                dataDate,
                dataStartTime!!,
                dataEndTime!!,
                dataPeople,
                dataLocation,
                dataVoucher
            )
            binding.PrepareTheBill.text = "Tổng tiền: ${String.format("%.0f VND", totalPrice)}"
            checkAndCountBookings()
            binding.btnConfirmBooking.isEnabled = true
        } else {
            binding.PrepareTheBill.text = "Vui lòng điền đủ thông tin để tính giá"
            binding.btnConfirmBooking.isEnabled = false
        }
    }

    private fun setGameType(typeGame: String) {
        dataGame = typeGame
        binding.txtGameType.text = "Loại Game: $dataGame"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRefStore.removeEventListener(storeValueEventListener)
    }

    private fun checkAndCountBookings() {
        val busyStatuses = listOf("Chờ xử lý", "Đã xác nhận", "Đang chơi")
        val busyBookings = mutableListOf<String>()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        dbRefBooktable.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val startTime = timeFormat.parse(dataStartTime ?: return)
                val endTime = timeFormat.parse(dataEndTime ?: return)

                listBooking.clear()
                if (snapshot.exists()) {
                    for (bookingSnap in snapshot.children) {
                        val bookingData = bookingSnap.getValue(dataTableManagement::class.java)
                        if (bookingData != null && bookingData.id != null && bookingData.status in busyStatuses) {
                            if (bookingData.dateTime == dataDate && bookingData.addressClb == dataLocation) {
                                try {
                                    val orderStart = timeFormat.parse(bookingData.startTime)
                                    val orderEnd = timeFormat.parse(bookingData.endTime)

                                    val isOverLapping =
                                        (orderStart.before(endTime) && startTime.before(orderEnd))
                                    if (isOverLapping) {
                                        busyBookings.add(bookingData.id!!)
                                    }
                                } catch (e: Exception) {
                                    Log.e(
                                        "BookingCheck",
                                        "Lỗi phân tích cú pháp thời gian: ${e.message}"
                                    )
                                    continue
                                }
                                listBooking.add(bookingData)
                            }
                        }
                    }
                }
                val reservedCount = busyBookings.size
                val availableCount = totalTables?.toDouble()?.minus(reservedCount.toDouble())

                val numPeople = dataPeople?.toIntOrNull() ?: 1
                if (availableCount != null) {
                    binding.btnConfirmBooking.isEnabled = (availableCount >= numPeople)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    requireContext(),
                    "Lỗi tải dữ liệu: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }

        })
    }

    private fun updateDailyStatistics(date: String, location: String) {
        dbRefBooktable.orderByChild("addressClb").equalTo(location)
            .addValueEventListener(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Log.d("DailyStats", "Không có đơn đặt bàn nào cho địa điểm: $location")
                    return
                }

                var confirmedCount = 0      // Đơn đã xác nhận
                var processingCount = 0     // Đơn chờ xử lý
                var activeTableCount = 0    // Bàn đang hoạt động (xác nhận + đang chơi)
                var totalRevenue = 0.0      // Tổng doanh thu
                var totalBookings = 0       // Tổng số đơn (trừ đơn bị từ chối)

                for (bookingSnap in snapshot.children) {
                    val booking = bookingSnap.getValue(dataTableManagement::class.java)
                    if (booking != null && booking.dateTime == date) {
                        if (booking.status != "Đã từ chối") {
                            totalBookings++
                        }
                        when (booking.status) {
                            "Chờ xử lý" -> {
                                processingCount++
                            }
                            "Đã xác nhận" -> {
                                confirmedCount++
                                activeTableCount++
                            }
                            "Đang chơi" -> {
                                activeTableCount++
                            }
                            "Đã hoàn thành" -> {
                                val moneyString = booking.money?.replace(Regex("[^\\d.]"), "")
                                totalRevenue += moneyString?.toDoubleOrNull() ?: 0.0
                            }
                        }
                    }
                }

                val totalAvailableTables = totalTables?.toIntOrNull() ?: 0
                val tableEmpty = totalAvailableTables - activeTableCount
                val statisticsStatus = activeTableCount + tableEmpty
                val maintenance = 0

                val finalOverviewDataMap = mapOf<String, Any>(
                    "name" to (nameCLB ?: ""),
                    "storeId" to (storeId ?: ""),
                    "ownerId" to (storeOwnerId ?: ""),
                    "location" to (dataLocation ?: ""),
                    "confirm" to confirmedCount,
                    "statisticsStatus" to statisticsStatus,
                    "tableActive" to activeTableCount,
                    "tableEmpty" to tableEmpty,
                    "maintenance" to maintenance,

                    "sumTable" to totalBookings,
                    "profit" to totalRevenue,
                    "processing" to processingCount
                )

                if (storeId != null) {
                    dbRefOverview.child(storeId.toString()).updateChildren(finalOverviewDataMap)
                        .addOnSuccessListener {
                            Log.i("DailyStats", "Đã cập nhật TOÀN BỘ thống kê cho ngày $date tại $location thành công!")
                        }
                        .addOnFailureListener { e ->
                            Log.e("DailyStats", "Lỗi cập nhật thống kê tổng hợp: ${e.message}")
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DailyStats", "Lỗi tải dữ liệu đặt bàn: ${error.message}")
            }
        })
    }


}


