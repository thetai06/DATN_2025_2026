package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.data.dataStoreDisplayInfo
import org.o7planning.myapplication.databinding.ItemOutstandingBinding

interface  onClickOrderOutStandingListenner {
    fun onClickOderOutStanding(name: String, location: String, storeId: String, ownerId: String)
}

class RvOutstanding(val list: ArrayList<dataStoreDisplayInfo>, private val listenner: FragmentHome) : RecyclerView.Adapter<RvOutstanding.viewHolderItem>() {

    var onClickItem:((dataStoreDisplayInfo, pos:Int)-> Unit)? = null

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
            val data = list[position]
            imgLogoShop.setImageResource(R.drawable.icons8_ball_32)
            txtNameShop.text = data.name
            txtLocationBook.text = "Cơ sở: ${data.address}"
            txtDescShop.text = "Số lượng bàn: ${data.tableNumber}"
            root.setOnClickListener {
                onClickItem?.invoke(data, position)
            }
            btnOrder.setOnClickListener {
                listenner.onClickOderOutStanding(data.name.toString(), data.address.toString(),data.storeId.toString(), data.ownerId.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}