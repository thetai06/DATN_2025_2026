package org.o7planning.myapplication.Owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.ItemOderRealTimeBinding

interface OnOrderConfirmListener {
    fun onConfirmClick(data: dataTableManagement)
    fun onRefuseRealtimeClick(id: String)
}

class RvOrderRealTime(
    val list: List<dataTableManagement>,
    private val listener: OnOrderConfirmListener
) : RecyclerView.Adapter<RvOrderRealTime.viewHolderItem>() {


    inner class viewHolderItem(val binding: ItemOderRealTimeBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RvOrderRealTime.viewHolderItem {
        val binding =
            ItemOderRealTimeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(holder: RvOrderRealTime.viewHolderItem, position: Int) {
        holder.binding.apply {
            nameRealtime.text = list[position].name
            timeRealtime.text = list[position].time
            personRealtime.text = "${list[position].person} Người"
            statusRealtime.text = list[position].status
            manyRealtime.text = list[position].money
            btnConfirmRealtime.setOnClickListener {
                listener.onConfirmClick(list[position])
            }
            btnRefuseRealtime.setOnClickListener {
                listener.onRefuseRealtimeClick(list[position].id.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}
