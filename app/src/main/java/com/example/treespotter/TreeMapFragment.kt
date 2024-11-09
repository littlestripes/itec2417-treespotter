package com.example.treespotter

import android.content.pm.PackageManager
import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.GeoPoint
import java.util.Date

private const val TAG = "TREE_MAP_FRAGMENT"

class TreeMapFragment: Fragment() {

    private lateinit var addTreeButton: FloatingActionButton

    // if already granted, no need to ask again
    private var locationPermissionGranted = false

    // track if map has already moved to user's location on load
    private var movedMapToUserLocation = false

    // get user's location
    private var fusedLocationProvider: FusedLocationProviderClient? = null

    // map and list of markers on map, storing all markers in a list
    private var map: GoogleMap? = null
    private val treeMarkers = mutableListOf<Marker>()

    private var treeList = listOf<Tree>()

    private val treeViewModel: TreeViewModel by lazy {
        ViewModelProvider(requireActivity()).get(TreeViewModel::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val mainView = inflater.inflate(R.layout.fragment_tree_map, container, false)

        addTreeButton = mainView.findViewById(R.id.add_tree)
        addTreeButton.setOnClickListener {
            addTreeAtLocation()
        }

        // more robust (in this case) alternative to findViewById
        val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?
        mapFragment?.getMapAsync(mapReadyCallback)

        // disable add tree btn until location is available
        setAddTreeButtonEnabled(false)

        // request permission to access device location
        requestLocationPermission()

        // draw existing trees on map, one marker per
        treeViewModel.latestTrees.observe(requireActivity()) { trees ->
            treeList = trees
            drawTrees()
        }

        return mainView
    }

    private val mapReadyCallback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * If Play services isn't installed on the device, the user will be prompted to do
         * so in SupportMapFragment. This method will only be triggered once the user has
         * installed Google Play services and returned to the app.
         */
        Log.d(TAG, "Map ready")
        map = googleMap

        googleMap.setOnInfoWindowClickListener { marker ->
            val treeForMarker = marker.tag as Tree
            requestDeleteTree(treeForMarker)
        }

        updateMap()
    }

    private fun requestDeleteTree(tree: Tree) {
        AlertDialog.Builder(requireActivity())
            .setTitle(getString(R.string.delete))
            .setMessage(getString(R.string.confirm_delete_tree, tree.name))
            .setPositiveButton(android.R.string.ok) { dialog, id ->
                treeViewModel.deleteTree(tree)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, id ->
                // user cancels, no action necessary
            }
            .create()
            .show()
    }

    private fun updateMap() {
        // draw markers
        drawTrees()

        if (locationPermissionGranted) {
            if (!movedMapToUserLocation) {  // when map opens, won't keep moving
                moveMapToUserLocation()
            }
            setAddTreeButtonEnabled(true)
        }
    }

    private fun setAddTreeButtonEnabled(isEnabled: Boolean) {
        addTreeButton.isClickable = isEnabled
        addTreeButton.isFocusable = isEnabled

        if (isEnabled) {
            addTreeButton.backgroundTintList = AppCompatResources.getColorStateList(requireActivity(), android.R.color.holo_green_light)
        } else {
            addTreeButton.backgroundTintList = AppCompatResources.getColorStateList(requireActivity(), android.R.color.darker_gray)
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
    }

    private fun requestLocationPermission() {
        // already granted perms?
        if (ContextCompat.checkSelfPermission(requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true
            Log.d(TAG, "Permission already granted")
            updateMap()
            setAddTreeButtonEnabled(true)
            fusedLocationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
        } else {
            // need to ask
            val requestLocationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (granted) {
                    Log.d(TAG, "User granted permission")
                    setAddTreeButtonEnabled(true)
                    locationPermissionGranted = true
                    fusedLocationProvider = LocationServices.getFusedLocationProviderClient(requireActivity())
                } else {
                    Log.d(TAG, "User did not grant permission")
                    setAddTreeButtonEnabled(false)
                    locationPermissionGranted = false
                    showSnackbar(getString(R.string.give_permission))
                }

                updateMap()
            }

            requestLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @SuppressLint("MissingPermission")
    private fun moveMapToUserLocation() {
        if (map == null) {
            return
        }

        if (locationPermissionGranted) {
            map?.isMyLocationEnabled = true
            map?.uiSettings?.isMyLocationButtonEnabled = true
            map?.uiSettings?.isZoomControlsEnabled = true

            fusedLocationProvider?.lastLocation?.addOnCompleteListener { getLocationTask ->
                val location = getLocationTask.result
                if (location != null) {
                    Log.d(TAG, "User's location $location")
                    val center = LatLng(location.latitude, location.longitude)
                    val zoomLevel = 8f
                    map?.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoomLevel))
                    movedMapToUserLocation = true
                } else {
                    showSnackbar(getString(R.string.no_location))
                }
            }
        }
    }

    private fun getTreeName(): String {
        // return a random tree name
        // TODO ask for tree info
        return listOf("Fir", "Pine", "Cedar", "Spruce", "Redwood", "Bristlecone", "Giant Sequoia", "Juniper").random()
    }

    @SuppressLint("MissingPermission")  // perms should already be granted
    private fun addTreeAtLocation() {
        if (fusedLocationProvider == null) { return }
        if (!locationPermissionGranted) {
            showSnackbar(getString(R.string.give_location_permission))
        }

        try {
            fusedLocationProvider?.lastLocation?.addOnCompleteListener(requireActivity()) { task ->
                val location = task.result

                if (location != null) {
                    val treeName = getTreeName()
                    val tree = Tree(
                        name = treeName,
                        dateSpotted = Date(),
                        location = GeoPoint(location.latitude, location.longitude)
                    )
                    treeViewModel.addTree(tree)
                    moveMapToUserLocation()  // so user can see new marker
                    showSnackbar(getString(R.string.tree_added, treeName))
                } else {
                    showSnackbar(getString(R.string.no_location))
                }
            }
        } catch (ex: SecurityException) {
            Log.e(TAG, "Adding tree at location - permission not granted", ex)
            // TODO consider requesting perms again but avoid nagging user
        }
    }

    private fun drawTrees() {
        if (map == null) { return }

        // with lots of markers, more efficient to resolve deltas (i.e. remove old + add new)

        for (marker in treeMarkers) {
            marker.remove()
        }

        treeMarkers.clear()

        for (tree in treeList) {

            // show heart if fav, tree otherwise

            val isFavorite = tree.favorite ?: false
            val iconId = if (isFavorite) R.drawable.filled_heart_small else R.drawable.tree_small

            // make marker for each tree + add to map
            tree.location?.let { geoPoint ->
                val markerOptions = MarkerOptions()
                    .position(LatLng(geoPoint.latitude, geoPoint.longitude))
                    .title(tree.name)
                    .snippet("Spotted on ${tree.dateSpotted}")
                    .icon(BitmapDescriptorFactory.fromResource(iconId))

                map?.addMarker(markerOptions)?.also { marker ->
                    treeMarkers.add(marker)
                    marker.tag = tree  // tag can be used to store any object
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = TreeMapFragment()
    }
}