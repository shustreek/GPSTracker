package ru.otus.gpstracker.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import ru.otus.gpstracker.R
import ru.otus.gpstracker.databinding.FragmentMapsBinding

class MapsFragment : Fragment() {

    private var isCameraMovedManual: Boolean = false
    private lateinit var binding: FragmentMapsBinding
    private var locationChangedListener: LocationSource.OnLocationChangedListener? = null
    private val locationRequest = LocationRequest()
        .setInterval(1_000)
        .setFastestInterval(1_000)
        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

    private val locationCallback: LocationCallback = object : LocationCallback() {

        override fun onLocationResult(result: LocationResult?) {
            super.onLocationResult(result)
            if (result == null) return
            val lastLocation = result.lastLocation
            locationChangedListener?.onLocationChanged(lastLocation)
            if (isCameraMovedManual) return
            val position = LatLng(lastLocation.latitude, lastLocation.longitude)
            map?.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 18f))
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val cameraMovedRunnable = Runnable {
        isCameraMovedManual = false
    }

    private val viewModel: MainViewModel by viewModels()

    private lateinit var resultLauncher: ActivityResultLauncher<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var map: GoogleMap? = null

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        googleMap.setLocationSource(object : LocationSource {
            override fun activate(listener: LocationSource.OnLocationChangedListener) {
                locationChangedListener = listener
            }

            override fun deactivate() {
                locationChangedListener = null
            }
        })
        viewModel.onMapReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranded ->
                viewModel.onRequestResult(isGranded)
            }
        if (savedInstanceState == null) {
            resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resultLauncher.unregister()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        binding.mapCover.setOnTouchListener { _, _ ->
            handler.removeCallbacksAndMessages(null)
            isCameraMovedManual = true
            handler.postDelayed(cameraMovedRunnable, 5_000)
            false
        }
        initObservers()
    }

    @SuppressLint("MissingPermission")
    private fun initObservers() {
        viewModel.showDialog.observe(viewLifecycleOwner, Observer {
            AlertDialog.Builder(requireContext())
                .setMessage(R.string.dialog_message)
                .setPositiveButton(
                    android.R.string.ok,
                    DialogInterface.OnClickListener { dialog, which ->
                        resultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    })
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        })

        viewModel.startLocation.observe(viewLifecycleOwner, Observer {
            map?.isMyLocationEnabled = true
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        })
        viewModel.showMyLocation.observe(viewLifecycleOwner, Observer {
            map?.isMyLocationEnabled = it
        })
    }

    companion object {
        fun newInstance() = MapsFragment()
    }
}