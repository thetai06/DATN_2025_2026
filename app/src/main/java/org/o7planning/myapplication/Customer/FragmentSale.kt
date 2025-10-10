package org.o7planning.myapplication.Customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataVoucher
import org.o7planning.myapplication.databinding.FragmentSaleBinding

class FragmentSale: Fragment(), onClickVoucherListenner {
    private lateinit var binding: FragmentSaleBinding
    private lateinit var dbRefSale: DatabaseReference
    private lateinit var voucherValueEventListener: ValueEventListener
    private lateinit var listVoucher: ArrayList<dataVoucher>
    private lateinit var voucherAdapter: RvVoucher

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSaleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        listVoucher = arrayListOf()
        dbRefSale = FirebaseDatabase.getInstance().getReference("dataVoucher")


        boxSpecialOffer()
        boxVoucher()
    }

    private fun boxVoucher() {
        voucherAdapter = RvVoucher(listVoucher,this)
        binding.rvVoucher.adapter = voucherAdapter
        binding.rvVoucher.layoutManager= GridLayoutManager(
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
                voucherAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
            }
        }
        dbRefSale.addValueEventListener(voucherValueEventListener)
    }

    private fun boxSpecialOffer() {
        val list = mutableListOf<OutDataSpecialOffer>()
        list.add(
            OutDataSpecialOffer(
                R.drawable.icons8goal48,
                "Ưu đãi đặt biệt",
                "Giảm tới 50% cho lần đầu đặt đầu tiên"
            )
        )
        binding.rvSpecialOffer.adapter = RvSpecialOffer(list)
        binding.rvSpecialOffer.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.HORIZONTAL,
            false
        )
    }

    override fun onClickVoucher(voucher: String) {
        val action = FragmentSaleDirections.actionFragmentSaleToFragmentBooktable(voucher)
        findNavController().navigate(action)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dbRefSale.removeEventListener(voucherValueEventListener)
    }

}