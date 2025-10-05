package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.databinding.ItemSpecialOfferBinding

class OutDataSpecialOffer(
    val img: Int,
    val title: String,
    val des: String
)

class RvSpecialOffer(val list: List<OutDataSpecialOffer>):
    RecyclerView.Adapter<RvSpecialOffer.viewHolderItem>() {

    inner class viewHolderItem(val binding: ItemSpecialOfferBinding):
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemSpecialOfferBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        holder.binding.apply {
            imgSpecialOffer.setImageResource(list[position].img)
            txtSpecialOfferTitle.text = list[position].title
            txtSpecialOfferDesc.text = list[position].des
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}