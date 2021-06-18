package ru.otus.gpstracker.ui.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import ru.otus.gpstracker.LocationForegroundService
import ru.otus.gpstracker.R
import ru.otus.gpstracker.databinding.FragmentMapsBinding
import ru.otus.gpstracker.utils.factory

class MapsFragment : Fragment() {

    private var isCameraMovedManual: Boolean = false
    private lateinit var binding: FragmentMapsBinding
    private var locationChangedListener: LocationSource.OnLocationChangedListener? = null
    private var polyline: Polyline? = null

    private var isFirstPosition = true

    fun onLocationResult(lastLocation: Location) {
        locationChangedListener?.onLocationChanged(lastLocation)
        if (isCameraMovedManual) return
        val position = LatLng(lastLocation.latitude, lastLocation.longitude)
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(position, 18f)
        if (isFirstPosition) {
            map?.moveCamera(cameraUpdate)
            isFirstPosition = false
        } else {
            map?.animateCamera(cameraUpdate)
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val cameraMovedRunnable = Runnable {
        isCameraMovedManual = false
    }

    private val viewModel: MainViewModel by viewModels { factory }

    private lateinit var resultLauncher: ActivityResultLauncher<String>

    private var map: GoogleMap? = null

    @SuppressLint("MissingPermission")
    private val callback = OnMapReadyCallback { googleMap ->
        map = googleMap

        val color = ContextCompat.getColor(requireContext(), R.color.teal_700)
        val polylineOptions: PolylineOptions = PolylineOptions()
            .jointType(JointType.ROUND)
            .color(color)
        polyline = googleMap.addPolyline(polylineOptions)
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
        binding.btnStop.setOnClickListener { viewModel.onStopClick() }
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

        viewModel.startLocation.observe(viewLifecycleOwner, Observer { start ->
            val intent = Intent(requireContext(), LocationForegroundService::class.java)
            if (start) {
                map?.isMyLocationEnabled = true
                ContextCompat.startForegroundService(requireContext(), intent)
            } else {
                requireContext().stopService(intent)
            }
        })
        viewModel.showMyLocation.observe(viewLifecycleOwner, Observer {
            map?.isMyLocationEnabled = it
        })
        viewModel.points.observe(viewLifecycleOwner, Observer {
            polyline?.points = it
        })
        viewModel.location.observe(viewLifecycleOwner, Observer {
            onLocationResult(it)
        })
    }

    companion object {
        fun newInstance() = MapsFragment()
    }
}