package org.o7planning.myapplication.Owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.data.dataVoucher
import org.o7planning.myapplication.databinding.ItemRvVoucheroverviewBinding

interface onVoucherRealtimeClick{
    fun editVoucher(dataVoucher: dataVoucher)
    fun refuseRealtime(id:String, voucher:String)
}

class RvVoucherOverView(
    val list: List<dataVoucher>,
    private val listenner: onVoucherRealtimeClick
) : RecyclerView.Adapter<RvVoucherOverView.viewHolderItem>(){

    inner class viewHolderItem(val binding: ItemRvVoucheroverviewBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemRvVoucheroverviewBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {

        holder.binding.apply {
            val data = list[position]
            titleVoucher.text = data.des
            dateVoucher.text = data.voucherTimeStart +" - "+ data.voucherTimeEnd
            voucherCode.text = data.voucherCode
            btnEditVoucher.setOnClickListener {
                listenner.editVoucher(data)
            }
            btnRefuseRealtime.setOnClickListener {
                listenner.refuseRealtime(data.id.toString(), data.voucherCode.toString())
            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }




}