package org.o7planning.myapplication.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.ItemSelectOrdergameBinding

class OutDataBookTable(val name: String)

class RvBooktable(val list: List<OutDataBookTable>):
    RecyclerView.Adapter<RvBooktable.viewHolderItem>()  {

        private var selectedPosition: Int = RecyclerView.NO_POSITION
    var onItemClick: ((OutDataBookTable, pos:Int)->Unit)? = null

    inner class viewHolderItem(val binding: ItemSelectOrdergameBinding):
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemSelectOrdergameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        holder.binding.btnOderGameType.text = list[position].name
        holder.binding.apply {
            if (position == selectedPosition){
                btnOderGameType.setBackgroundResource(R.drawable.bg_btn_add_bar)
            }else{
                btnOderGameType.setBackgroundResource(R.drawable.bg_btn_game)
            }
            btnOderGameType.setOnClickListener {
                val currentPosition = holder.adapterPosition
                if (selectedPosition != RecyclerView.NO_POSITION){
                    notifyItemChanged(selectedPosition)
                }
                selectedPosition = currentPosition
                notifyItemChanged(currentPosition)
                onItemClick?.invoke(list[position], position)
            }

        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}