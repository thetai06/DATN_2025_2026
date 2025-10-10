package org.o7planning.myapplication.Customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.ItemFeatureProfileBinding
import kotlin.coroutines.coroutineContext

class OutDataFeatureProfile(val name:String, val icon:Int)

class RvFeatureProfile(val list:List<OutDataFeatureProfile>) : RecyclerView.Adapter<RvFeatureProfile.viewHolderItem>() {

    var onClickItem:((OutDataFeatureProfile, pos:Int)-> Unit)? = null

    inner class viewHolderItem(val binding: ItemFeatureProfileBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemFeatureProfileBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        holder.binding.apply {
            tvTitle.text = list[position].name
            ivIcon.setImageResource(list[position].icon)
            if (tvTitle.text == "Đăng xuất"){
                val context = tvTitle.context
                val color = ContextCompat.getColor(context, R.color.colorRed)
                tvTitle.setTextColor(color)
                ivIcon.setColorFilter(color)
                root.setBackgroundResource(R.drawable.bg_violet)
            }
            root.setOnClickListener {
                onClickItem?.invoke((list[position]), position)
            }
        }

    }

    override fun getItemCount(): Int {
        return list.size
    }
}