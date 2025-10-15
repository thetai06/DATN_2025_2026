package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataStore
import org.o7planning.myapplication.databinding.ItemClbBiaBinding
import android.graphics.Color

interface onOrderClickListener{
    fun onOrderClick(id:String, ownerId:String, name: String, location: String)
}

class RvClbBia(val list:List<dataStore>, private val listener: onOrderClickListener): RecyclerView.Adapter<RvClbBia.viewHolderItem>() {

    var onClickItem:((dataStore, pos: Int)-> Unit)? = null

    private var selectionPosition: Int = RecyclerView.NO_POSITION

    inner class viewHolderItem(val binding: ItemClbBiaBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemClbBiaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        holder.binding.apply {
            val data = list[position]
            imgLogoShop.setImageResource(R.drawable.icons8_ball_32)
            txtNameShop.text = data.name
            txtLocationBook.text = "Cơ sở: ${data.address}"
            txtDescShop.text = "Số lượng bàn:  / ${data.tableNumber}"
            root.setOnClickListener {
                onClickItem?.invoke(data, position)
            }
            if (position == selectionPosition){
                btnOrder.setBackgroundResource(R.drawable.bg_btn_add_bar)
                btnOrder.setTextColor(Color.WHITE)
            }else{
                btnOrder.setBackgroundResource(R.drawable.bg_xam)
            }
            btnOrder.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (selectionPosition != RecyclerView.NO_POSITION){
                    notifyItemChanged(selectionPosition)
                }
                selectionPosition = currentPosition
                notifyItemChanged(selectionPosition)

                listener.onOrderClick(data.storeId.toString(),data.ownerId.toString(),data.name.toString(), data.address.toString())
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}

