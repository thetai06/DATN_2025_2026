package org.o7planning.myapplication.owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataStoreDisplayInfo
import org.o7planning.myapplication.databinding.ItemOverviewBinding

class RvOverview(val list: List<dataStoreDisplayInfo>) :
    RecyclerView.Adapter<RvOverview.viewHolderItem>() {
    inner class viewHolderItem(val binding: ItemOverviewBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding =
            ItemOverviewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        holder.binding.apply {
            val data = list[position]
            imgHeader.setImageResource(R.drawable.quan_bi_a2)
            txtTitleShop.text = data.name
            txtLocationOverview.text = data.address
            txtSumTable.text = "Tổng đặt bàn: ${data.totalBooking.toString()}"
            txtConfirm.text = "Đã xác nhận: ${data.confirm.toString()}"
            txtProfit.text = "Doanh thu: ${data.profit.toString()}"
            txtProcessing.text = "Chờ xử lý: ${data.pendingBookings.toString()}"
            txtStatisticsStatus.text = "Tổng bàn: ${data.tableNumber}"
            txtTableActive.text = "Bàn đang hoạt động: ${data.tableActive.toString()}"
            txtTableEmpty.text = "Bàn trống: ${data.tableEmpty.toString()}"
            maintenance.text = "Bàn Bảo trì: ${data.maintenance.toString()}"
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}