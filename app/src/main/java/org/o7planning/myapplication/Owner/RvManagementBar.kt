package org.o7planning.myapplication.Owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.o7planning.myapplication.data.dataStort
import org.o7planning.myapplication.databinding.ItemBilliardsBarManagementBinding



class RvManagementBar(val list: List<dataStort>): RecyclerView.Adapter<RvManagementBar.viewHolderItem>() {

    inner class viewHolderItem(val binding: ItemBilliardsBarManagementBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemBilliardsBarManagementBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        val store = list[position]

        holder.binding.apply {
            Glide.with(holder.itemView.context)
                .load(store.imageURL)
                .placeholder(org.o7planning.myapplication.R.drawable.quan_bi_a1)
                .error(org.o7planning.myapplication.R.drawable.quan_bi_a2)
                .into(ivManagement)
            locationBar.text = "Cơ sở: ${store.address}"
            phoneBar.text = "Số Điện Thoại: ${store.phone}"
            emailBar.text = "Email: ${store.email}"
            timeBar.text = "Thời gian hoạt động: ${store.operationTime}"
            tableBar.text = "Số lượng bàn: ${store.table}"
            desBar.text = "Tổng quan: ${store.des}"
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}