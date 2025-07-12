package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    companion object {
        private const val TAG = "MainActivity"
        private const val STORAGE_PERMISSION_CODE = 23
    }

    /** ───────── 1. launchers ───────── */

    /**
     * Launcher that opens system Settings for “All files access” (API 30+)
     * or requests legacy runtime permissions (pre‑30).
     */

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (isStoragePermissionGranted()) {
                //openDirectoryPicker()        // we can now pick a folder
                copyRawAndScan()
            } else {
                toast("Storage permission denied")
            }
        }

    /**
     * SAF folder picker – returns the chosen directory URI.
     */

    private val openDirectoryLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            runCatching {
                requireNotNull(treeUri) { "No directory selected" }

                /* 1 ─ Persist permission (best‑effort) */
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                try {
                    contentResolver.takePersistableUriPermission(treeUri, takeFlags)
                } catch (se: SecurityException) {
                    contentResolver.takePersistableUriPermission(
                        treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }

                /* 2 ─ Resolve real path */
                val fullPath = treeUri.toFullPath()
                    ?: error("Folder is not on primary storage")

                val displayedPath = fullPath ?: treeUri.toString()
                binding.pathText.text = displayedPath

                /* 2a ─ >>> show the path immediately <<< */
                binding.pathText.text = displayedPath        // <-- NEW line

                /* 3 ─ Native scan */
                NativeLib.scanDirectory(fullPath).also { result ->
                    binding.sampleText.text = result          // <- UI update
                }
            }.onFailure { ex ->
                Log.e(TAG, "Directory pick failed", ex)
                toast(ex.message ?: "Something went wrong")
            }
        }

    /** ───────── 2. lifecycle ───────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pickDirButton.setOnClickListener {                        // ← add a button in your layout
            if (isStoragePermissionGranted()) {
                //openDirectoryPicker()
                copyRawAndScan()
            } else {
                requestStoragePermissions()
            }
        }
    }

    /** ───────── 3. permission helpers ───────── */

    private fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val readGranted  = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            val writeGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
            readGranted && writeGranted
        }
    }

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Jump to the system “All files access” toggle for this package
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.parse("package:$packageName"))
            storagePermissionLauncher.launch(intent)
        } else {
            // Legacy runtime permission request
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    /** ───────── 4. directory picker ───────── */

    private fun openDirectoryPicker() {
        openDirectoryLauncher.launch(null)   // null uses the default start location
    }

    /** ───────── 5. utilities ───────── */

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    /**
     * Convert a SAF tree‑URI to a best‑guess absolute path.
     * Works for primary and most secondary volumes that are publicly mounted under /storage.
     *
     * Returns null if the volume type is unknown (e.g. DownloadsProvider).
     */
    private fun Uri.toFullPath(): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP &&
            DocumentsContract.isDocumentUri(this@MainActivity, this)
        ) {
            val docId = DocumentsContract.getTreeDocumentId(this)          // e.g. “primary:DCIM/Camera”
            val split = docId.split(':', limit = 2)                        // ["primary", "DCIM/Camera"]
            if (split.size == 2) {
                val volume = split[0]                                      // "primary" or "XXXX-XXXX"
                val relativePath = split[1]                                // "DCIM/Camera"

                // Map the volume ID to a real mount point
                val base = if (volume.equals("primary", true)) {
                    Environment.getExternalStorageDirectory().path         // /storage/emulated/0
                } else {
                    "/storage/$volume"                                     // /storage/XXXX-XXXX  (SD card)
                }

                return "$base/$relativePath".removeSuffix("/")             // tidy up possible trailing /
            }
        }
        return null
    }


    private fun copyRawAndScan() {
        // 1. IDs of the files you want to scan
        val rawIds = intArrayOf(
            R.raw.libjniloader,
            R.raw.libanw14,
            R.raw.libanw18,
            R.raw.libarm64v8a,
            R.raw.libiomx13,
            R.raw.libudev
        )

        // 2. Destination folder (cleared each time for simplicity)
        val outDir = File(cacheDir, "raw_copy").apply {
            deleteRecursively()
            mkdirs()
        }

        // 3. Copy raw resources to .so files
        rawIds.forEach { resId ->
            val base = resources.getResourceEntryName(resId) // e.g., "libanw14"
            val fileName = if (base.endsWith(".so")) base else "$base.so"
            val outFile = File(outDir, fileName)

            resources.openRawResource(resId).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }

        // 4. Run the native scanner
        val scanOut = NativeLib.scanDirectory(outDir.absolutePath)
        Log.d("NativeScan", "\n$scanOut")

        // 5. Create a map of filename -> size
        val sizeMap = outDir.listFiles()?.associate { it.name to it.length() } ?: emptyMap()

        // 6. Build output table
        val rowRx = Regex("""^(lib\S+\.so)\s+(\S+)""")
        val table = buildString {
            append("Total libraries: ${sizeMap.size}\n\n")
            append(String.format("%-20s %-12s %10s\n", "File", "ArchType", "Size"))
            append("=".repeat(44)).append('\n')

            val matchedFiles = mutableSetOf<String>()

            // Parse and print matched scan rows
            scanOut.lineSequence().forEach { line ->
                rowRx.find(line.trim())?.let { match ->
                    val (file, arch) = match.destructured
                    val size = sizeMap[file] ?: 0L
                    matchedFiles += file
                    append(
                        String.format(
                            "%-20s %-12s %10s\n",
                            file, arch,
                            android.text.format.Formatter.formatFileSize(this@MainActivity, size)
                        )
                    )
                }
            }

            // Fallback for any missing file entries
            sizeMap.keys.subtract(matchedFiles).forEach { file ->
                val size = sizeMap[file] ?: 0L
                append(
                    String.format(
                        "%-20s %-12s %10s\n",
                        file, "unknown",
                        android.text.format.Formatter.formatFileSize(this@MainActivity, size)
                    )
                )
            }
        }

        // 7. Display result in TextView
        binding.sampleText.text = table
    }

}
