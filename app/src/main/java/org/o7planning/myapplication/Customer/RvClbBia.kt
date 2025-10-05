package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataStort
import org.o7planning.myapplication.databinding.ItemClbBiaBinding

interface onOrderClickListener{
    fun onOrderClick(name: String, location: String)
}

class RvClbBia(val list:List<dataStort>, private val listener: onOrderClickListener): RecyclerView.Adapter<RvClbBia.viewHolderItem>() {

    var onClickItem:((dataStort, pos: Int)-> Unit)? = null

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
            if (position == selectionPosition){
                btnOrder.setBackgroundResource(R.drawable.bg_btn_add_bar)
            }else{
                btnOrder.setBackgroundResource(R.drawable.background_bogoc_cam)
            }
            btnOrder.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (selectionPosition != RecyclerView.NO_POSITION){
                    notifyItemChanged(selectionPosition)
                }
                selectionPosition = currentPosition
                notifyItemChanged(selectionPosition)

                listener.onOrderClick(list[position].name.toString(), list[position].address.toString())
            }
            imgLogoShop.setImageResource(R.drawable.icons8_ball_32)
            txtNameShop.text = list[position].name
            txtLocationBook.text = list[position].address
            txtDescShop.text = list[position].des
            root.setOnClickListener {
                onClickItem?.invoke(list[position], position)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


}

