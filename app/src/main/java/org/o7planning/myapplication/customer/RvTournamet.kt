package org.o7planning.myapplication.customer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.o7planning.myapplication.databinding.ItemTournamentBinding
import kotlin.apply

class OutDataTournament(
    val image: Int,
    val name: String,
    val desc: String)

class RvTournamet(val list: List<OutDataTournament>): RecyclerView.Adapter<RvTournamet.viewHolderItem>() {

    var onClickItem:((OutDataTournament, pos:Int)-> Unit)? = null


    inner class viewHolderItem(val binding: ItemTournamentBinding): RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): viewHolderItem {
        val binding = ItemTournamentBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return viewHolderItem(binding)
    }

    override fun onBindViewHolder(
        holder: viewHolderItem,
        position: Int
    ) {
        holder.binding.apply {
            imgTournament.setImageResource(list[position].image)
            txtTournament.text = list[position].name
            txtTournamentStart.text = list[position].desc
            root.setOnClickListener {
                onClickItem?.invoke(list[position], position)
            }
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

}


