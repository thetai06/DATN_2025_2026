package org.o7planning.myapplication.Customer

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import org.o7planning.myapplication.data.dataStort
import org.o7planning.myapplication.databinding.FragmentBooktableBinding
import java.text.SimpleDateFormat
import java.util.Locale

class FragmentBooktable : Fragment(), onOrderClickListener {
    private lateinit var dbRefBooktable: DatabaseReference
    private lateinit var dbRefStore: DatabaseReference
    private lateinit var listStore: ArrayList<dataStort>
    private lateinit var storeValueEventListener: ValueEventListener
    private lateinit var storeAdapter: RvClbBia
    private lateinit var mAuth: FirebaseAuth
    private var userId:String? = null
    private lateinit var dbRefUser: DatabaseReference
    private var userName: String? = null

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
        mAuth = FirebaseAuth.getInstance()
        userId = mAuth.currentUser?.uid
        dbRefUser = FirebaseDatabase.getInstance().getReference("dataUser").child(userId!!)
        loadUserName()

        boxSelectGameType()
        boxSelectDate()
        boxSelectPeopel()
        boxSelectTime()
        boxSelectOutstandingCLB()
        boxVoucher()
        binding.btnConfirmBooking.setOnClickListener {
            someOtherFunction()
        }
    }

    private fun loadUserName() {
        dbRefUser.addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                userName = snapshot.child("name").getValue(String::class.java)
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun addDataOrder() {
        binding.apply {
            val name = userName.toString()
            val time = txtTime.text.toString()
            val person = dataPeople.toString()
            val table = "1"
            val Game = dataGame.toString()
            val money = PrepareTheBill.text.toString()
            val status = "Chờ xử lý"
            val idBooking = dbRefBooktable.push().key.toString()
            val dataOder = dataTableManagement(userId,idBooking, name, time, person, money, status)
            dbRefBooktable.child(idBooking).setValue(dataOder)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Thanh cong", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "khong thanh cong", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private var dataGame: String? = "8-ball"
    private var dataDate: String? = "01 / 01 / 2025"
    private var dataStartTime: String? = "01:00"
    private var dataEndTime: String? = null
    private var dataPeople: String? = null
    private var dataLocation: String? = null
    private var dataVoucher: String? = null


    private fun someOtherFunction() {
        if (dataStartTime != null && dataEndTime != null
            && dataDate != null && dataPeople != null
            && dataLocation != null
        ) {
            val totalPrice = calculateTablePrice(
                dataGame,
                dataDate,
                dataStartTime!!,
                dataEndTime!!,
                dataPeople,
                dataLocation,
                dataVoucher
            )
            binding.PrepareTheBill.text = "Tổng tiền: $totalPrice"
            addDataOrder()
        } else {
            Toast.makeText(
                requireContext(),
                "Không bỏ sót thông tin $dataPeople $dataLocation",
                Toast.LENGTH_SHORT
            ).show()
        }
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
        val basePricePerPerson = 100.0 // Giá cơ bản trên mỗi người
        // Tính toán thời gian giữa hai thời điểm
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
            DatePickerDialog(
                requireContext(),
                DatePickerDialog.OnDateSetListener { datePicker, i, i2, i3 ->
                    val selectDate = "$i3 / ${i2 + 1} / $i"
                    binding.tvDate.text = selectDate
                    selectDate(selectDate)
                },
                6,
                10,
                2003
            ).show()
        }
    }

    private fun selectDate(date: String) {
        dataDate = date
        binding.txtDateTime.text = "Ngày: " + dataDate
    }

    private fun boxSelectCLB() {
        storeAdapter = RvClbBia(listStore,this)
        binding.rvClbBia.adapter = storeAdapter.apply {
            onClickItem = { item, pos ->
                Toast.makeText(
                    requireContext(),
                    item.name + ", " + item.address,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        binding.rvClbBia.layoutManager = GridLayoutManager(
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
                        val stortData = storeSnap.getValue(dataStort::class.java)
                        stortData?.let { listStore.add(it) }
                    }
                }
                storeAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        }
        dbRefStore.addValueEventListener(storeValueEventListener)
    }

    override fun onOrderClick(name: String, location: String) {
        binding.txtSelectClb.text = "Quán: $name, Cơ sở: $location"
        dataLocation = location
    }

    private fun boxSelectOutstandingCLB() {
        val name = arguments?.getString("name")
        val location = arguments?.getString("location")
        binding.txtSelectClb.text = "Quán: $name, Cơ sở: $location"
        if (name != null) {
            binding.boxClbBia.visibility = View.GONE
        }
        boxSelectCLB()
    }

    private fun boxSelectPeopel() {
        val tables = listOf("1", "2", "3", "4")
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

    private fun setGameType(typeGame: String) {
        dataGame = typeGame
        binding.txtGameType.text = "Loại Game: $dataGame"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRefStore.removeEventListener(storeValueEventListener)
    }

}


