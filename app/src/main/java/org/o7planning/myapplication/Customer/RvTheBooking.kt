package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataTableManagement
import org.o7planning.myapplication.databinding.ItemTheBookingBinding

class RvTheBooking(val list: ArrayList<dataTableManagement>) : RecyclerView.Adapter<RvTheBooking.viewHolderItem>() {
    inner class viewHolderItem(val binding: ItemTheBookingBinding) : RecyclerView.ViewHolder(binding.root)

    var onClickCancelOrder: ((dataTableManagement, Int) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding =
            ItemTheBookingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        val data = list[position]
        holder.binding.apply {
            nameRealtime.text = data.name
            timeRealtime.text = data.time
            personRealtime.text = "${data.person} Người"
            statusRealtime.text = data.status
            manyRealtime.text = data.money
            statusRealtime.setText(data.status)
            if (data.status == "Đã xác nhận"){
                statusRealtime.setBackgroundResource(R.drawable.bg_btn_status_confirm)
            }else if (data.status =="Đang chơi"){
                statusRealtime.setBackgroundResource(R.drawable.bg_start_game)
            }else if (data.status == "Chờ xử lý"){
                statusRealtime.setBackgroundResource(R.drawable.bg_startic)
            }
        }
        holder.itemView.setOnLongClickListener {
            onClickCancelOrder?.invoke(data,position)
            return@setOnLongClickListener true
        }


    }

    override fun getItemCount(): Int {
        return list.size
    }


}