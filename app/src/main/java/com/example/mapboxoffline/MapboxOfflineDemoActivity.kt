package com.example.mapboxoffline

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import com.example.mapboxoffline.databinding.ActivityOfflineIssueBinding
import com.mapbox.bindgen.Value
import com.mapbox.common.NetworkRestriction
import com.mapbox.common.TileDataDomain
import com.mapbox.common.TileRegionLoadOptions
import com.mapbox.common.TileStore
import com.mapbox.common.TileStoreOptions
import com.mapbox.geojson.Point
import com.mapbox.maps.GlyphsRasterizationMode
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.OfflineManager
import com.mapbox.maps.StylePackLoadOptions
import com.mapbox.maps.TilesetDescriptorOptions
import com.mapbox.maps.extension.style.style
import com.mapbox.maps.plugin.Plugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapboxOfflineDemoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOfflineIssueBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOfflineIssueBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.btDownloadEmbedded.setOnClickListener {
            downloadOfflineMaps(style = "asset://style.json")
        }
        binding.btDownloadRemote.setOnClickListener {
            downloadOfflineMaps(style = "mapbox://styles/maxneust/cl9gvi3ir00ai15nlwmi6uqop")
        }
        putFragment()

    }

    /**
     * Download a set of tiles using the offline functionality of mapbox.
     */
    private fun downloadOfflineMaps(style: String) {
        val stylePackLoadOptions = StylePackLoadOptions.Builder()
            .glyphsRasterizationMode(GlyphsRasterizationMode.IDEOGRAPHS_RASTERIZED_LOCALLY)
            .build()

        val offlineManager =
            OfflineManager(MapInitOptions.getDefaultResourceOptions(this))

        val tilesetDescriptor = offlineManager.createTilesetDescriptor(
            TilesetDescriptorOptions.Builder()
                .styleURI(style)
                .stylePackOptions(stylePackLoadOptions)
                .minZoom(0)
                .maxZoom(4)
                .build()
        )
        val tileRegionLoadOptions = TileRegionLoadOptions.Builder()
            .descriptors(listOf(tilesetDescriptor))
            .acceptExpired(true)
            .geometry(Point.fromLngLat(0.0, 0.0))
            .networkRestriction(NetworkRestriction.NONE)
            .build()

        val tileStore = TileStore.create().also {
            // Set default access token for the created tile store instance
            it.setOption(
                TileStoreOptions.MAPBOX_ACCESS_TOKEN,
                TileDataDomain.MAPS,
                Value(getString(R.string.mapbox_access_token))
            )
        }

        tileStore.loadTileRegion(
            "WORLD",
            tileRegionLoadOptions,
            { progress ->
                Log.v("OFFLINE", "Downloading tileregion ... $progress")
                // Handle the download progress
            }
        ) { expected ->
            if (expected.isValue) {
                // Tile region download finishes successfully
                Log.v("OFFLINE", "Downloaded tileregion")
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MapboxOfflineDemoActivity,
                        "Tile region downloaded",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (expected.isError) {
                Log.e("OFFLINE", "${expected.error}")
                lifecycleScope.launch(Dispatchers.Main) {
                    Toast.makeText(
                        this@MapboxOfflineDemoActivity,
                        "Tile region error: ${expected.error}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
            }
        }
    }

    private fun putFragment() {
        supportFragmentManager.commitNow {
            val offlineDemoMapFragment = OfflineDemoMapFragment()
            this.add(binding.mapContainer.id, offlineDemoMapFragment, "map")
        }
    }
}

class OfflineDemoMapFragment : Fragment() {

    private val mapView
        get() = requireView() as MapView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = MapView(
        requireContext(),
        MapInitOptions(
            requireContext(),
            textureView = true,
            plugins = listOf(
                Plugin.Mapbox(Plugin.MAPBOX_CAMERA_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_GESTURES_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_COMPASS_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_LOGO_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_ATTRIBUTION_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_LIFECYCLE_PLUGIN_ID),
                Plugin.Mapbox(Plugin.MAPBOX_MAP_OVERLAY_PLUGIN_ID)
            ),
            resourceOptions = MapInitOptions.getDefaultResourceOptions(requireContext())
                .toBuilder()
                .accessToken(resources.getString(R.string.mapbox_access_token))
                .build(),
            mapOptions = MapInitOptions.getDefaultMapOptions(requireContext()).toBuilder().apply {
                this.optimizeForTerrain(false)
            }.build()
        )
    ).apply {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mapView.getMapboxMap().loadStyle(style("asset://style.json") {})
    }


}