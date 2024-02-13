package com.example.android.politicalpreparedness.election.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.android.politicalpreparedness.databinding.ElectionItemBinding
import com.example.android.politicalpreparedness.network.models.Election

class ElectionListAdapter(private val onClickListener: CustomOnClickListener)
    : ListAdapter<Election, ElectionListAdapter.ElectionViewHolder>(ElectionDiffCallback) {

    /**
     * ViewHolder constructor takes the binding variable from the associated
     * ElectionItemBinding, which nicely gives it access to the full [Election] information.
     */
    class ElectionViewHolder(val binding: ElectionItemBinding):
        RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup) = ElectionViewHolder(
                ElectionItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        fun bind(election: Election, customClickListener: CustomOnClickListener) {
            binding.election = election
            binding.customClick = customClickListener
            println("dra - ElectionListAdapter - ElectionViewHolder - bind - election.name: ${election.name}")
            // This is important, because it forces the data binding to execute immediately,
            // which allows the RecyclerView to make the correct view size measurements
            binding.executePendingBindings()
        }
    }

    /** * Create new [RecyclerView] item views (invoked by the layout manager) */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElectionViewHolder {
        return ElectionViewHolder.from(parent)
    }

    /** * Replaces the contents of a view (invoked by the layout manager)*/
    override fun onBindViewHolder(holder: ElectionViewHolder, position: Int) {
        val election = getItem(position)
        // This bind function is defined above ....
        holder.bind(election, onClickListener)
    }

    // OnClickListener class with a lambda in its constructor that initializes a matching onClick function
    class CustomOnClickListener(val clickListener: (election: Election) -> Unit) {
        fun onClick(election: Election) = clickListener(election)
    }

    /**
     * Allows the RecyclerView to determine which items have changed when the [List] of [Election]
     * has been updated.
     */
    companion object ElectionDiffCallback : DiffUtil.ItemCallback<Election>() {
        override fun areItemsTheSame(oldItem: Election, newItem: Election): Boolean {
            return oldItem === newItem
        }

        override fun areContentsTheSame(oldItem: Election, newItem: Election): Boolean {
            return oldItem.id == newItem.id
        }
    }

}
