package com.example.treespotter

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.Firebase
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore

private const val TAG = "TREE_VIEW_MODEL"

class TreeViewModel: ViewModel() {

    private val db = Firebase.firestore
    private val treeCollectionReference = db.collection("trees")

    val latestTrees = MutableLiveData<List<Tree>>()

    private val latestTreesListener = treeCollectionReference
        .orderBy("dateSpotted", Query.Direction.DESCENDING)
        .limit(10)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error getting latest trees", error)
            }

            if (snapshot != null) {
                // convert snapshot to tree objects
                // val trees = snapshot.toObjects(Tree::class.java)

                // but we want to store the tree references so we need to loop + convert, add doc refs
                val trees = mutableListOf<Tree>()
                for (treeDocument in snapshot) {
                    val tree = treeDocument.toObject(Tree::class.java)
                    tree.documentReference = treeDocument.reference
                    trees.add(tree)
                }
                Log.d(TAG, "Trees from firebase: $trees")
                latestTrees.postValue(trees)
            }
        }

    fun setIsFavorite(tree: Tree, favorite: Boolean) {
        tree.favorite = favorite
        // updates the favorite status on Firestore using the doc ref
        tree.documentReference?.update("favorite", favorite)
    }

    fun addTree(tree: Tree) {
        treeCollectionReference.add(tree)
            .addOnSuccessListener { treeDocumentReference ->
                Log.d(TAG, "Added tree document at ${treeDocumentReference.path}")
                tree.documentReference = treeDocumentReference
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Error adding tree $tree", error)
            }
    }

    fun deleteTree(tree: Tree) {
        tree.documentReference?.delete()
    }
}