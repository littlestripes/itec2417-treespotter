package com.example.treespotter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

private const val TAG = "TREE_RECYCLER_ADAPTER"

class TreeRecyclerViewAdapter(var trees: List<Tree>, val heartTreeListener: (Tree, Boolean) -> Unit): RecyclerView.Adapter<TreeRecyclerViewAdapter.ViewHolder>() {

    inner class ViewHolder(private val view: View): RecyclerView.ViewHolder(view) {
        private lateinit var treeNameTextView: TextView
        private lateinit var dateSpottedTextView: TextView

        fun bind(tree: Tree) {
            view.findViewById<TextView>(R.id.tree_name).text = tree.name
            view.findViewById<TextView>(R.id.date_spotted).text = "${tree.dateSpotted}"
            view.findViewById<CheckBox>(R.id.heart_check).apply {
                isChecked = tree.favorite
                setOnCheckedChangeListener { checkbox, isChecked ->
                    Log.d(TAG, "Setting ${tree.name} favorite $isChecked")
                    heartTreeListener(tree, isChecked)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_tree_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tree = trees[position]
        holder.bind(tree)
    }

    override fun getItemCount(): Int {
        return trees.size
    }
}