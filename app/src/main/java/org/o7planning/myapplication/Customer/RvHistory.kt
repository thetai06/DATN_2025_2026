package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.ItemHistoryBinding

class RvHistory(val list: List<dataTableManagement>) :
    RecyclerView.Adapter<RvHistory.viewHolderItem>() {

    inner class viewHolderItem(val binding: ItemHistoryBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        val data = list[position]
        holder.binding.apply {
            if (data.phoneNumber == "null") {
                nameRealtime.text = "${data.name} - ${data.email}"
            } else {
                nameRealtime.text = "${data.name} - ${data.phoneNumber}"
            }
            timeRealtime.text = "Thời gian: ${data.startTime} - ${data.endTime}"
            address.text = data.addressClb
            personRealtime.text = "${data.person} Người"
            statusRealtime.text = data.status
            manyRealtime.text = data.money
            statusRealtime.setText(data.status)
            if (data.status == "Đã hoàn thành") {
                statusRealtime.setBackgroundResource(R.drawable.bg_btn_status_confirm)
            } else if (data.status == "Đã huỷ") {
                statusRealtime.setBackgroundResource(R.drawable.bg_start_game)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}