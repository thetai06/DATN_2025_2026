package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataOutstanding
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.databinding.ItemOutstandingBinding

interface  onClickOrderOutStandingListenner {
    fun onClickOderOutStanding(name: String, location: String)
}

class RvOutstanding(val list: List<dataStore>, private val listenner: FragmentHome) : RecyclerView.Adapter<RvOutstanding.viewHolderItem>() {

    var onClickItem:((dataStore, pos:Int)-> Unit)? = null

    inner class viewHolderItem(val binding: ItemOutstandingBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemOutstandingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(holder: viewHolderItem, position: Int) {
        holder.binding.apply {
            imgLogoShop.setImageResource(R.drawable.icons8_ball_32)
            txtNameShop.text = list[position].name
            txtLocationBook.text = list[position].address
            txtDescShop.text = list[position].des
            root.setOnClickListener {
                onClickItem?.invoke(list[position], position)
            }
            btnOrder.setOnClickListener {
                listenner.onClickOderOutStanding(list[position].name.toString(), list[position].address.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}