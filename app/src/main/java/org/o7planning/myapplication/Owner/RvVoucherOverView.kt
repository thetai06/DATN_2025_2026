package org.o7planning.myapplication.Owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.data.dataVoucher
import org.o7planning.myapplication.databinding.ItemRvVoucheroverviewBinding

interface onVoucherRealtimeClick{
    fun editVoucher(dataVoucher: dataVoucher)
    fun refuseRealtime(id:String)
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
            titleVoucher.text = list[position].des
            clbVoucher.text = list[position].voucherToClb
            dateVoucher.text = list[position].voucherTime
            voucherCode.text = list[position].voucherCode
            btnEditVoucher.setOnClickListener {
                listenner.editVoucher(list[position])
            }
            btnRefuseRealtime.setOnClickListener {
                listenner.refuseRealtime(list[position].id.toString())
            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }




}