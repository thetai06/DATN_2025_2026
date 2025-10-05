package org.o7planning.myapplication.Owner

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.data.dataOverviewOwner
import org.o7planning.myapplication.databinding.ItemOverviewBinding

class RvOverview(val list: List<dataOverviewOwner>) :
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
            imgHeader.setImageResource(list[position].img)
            txtTitleShop.text = list[position].name
            txtLocationOverview.text = list[position].location
            txtAssessment.text = list[position].assessment
            txtSumTable.text = list[position].sumTable
            txtConfirm.text = list[position].confirm
            txtProfit.text = list[position].profit
            txtProcessing.text = list[position].processing
            txtStatisticsStatus.text = list[position].statisticsStatus
            txtTableActive.text = list[position].tableActive
            txtTableEmpty.text = list[position].tableEmpty
            maintenance.text = list[position].maintenance
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}