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
                openDirectoryPicker()        // we can now pick a folder
            } else {
                toast("Storage permission denied")
            }
        }

    /**
     * SAF folder picker – returns the chosen directory URI.
     */

    private val openDirectoryLauncher:
            ActivityResultLauncher<Uri?> =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { treeUri ->
            if (treeUri != null) {
                // Persist permission so we can use it again without prompting
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(treeUri, flags)

                // Try to resolve a real path (will still work on API 30+ if Manage‑All‑Files is granted)
                val fullPath = treeUri.toFullPath()
                    ?: treeUri.toString()   // fall back to URI string

                Log.d(TAG, "Selected dir path = $fullPath")

                // Call your native scanner
                val result = NativeLib.scanDirectory(fullPath)
                binding.sampleText.text = result
            } else {
                toast("No directory selected")
            }
        }

    /** ───────── 2. lifecycle ───────── */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.pickDirButton.setOnClickListener {                        // ← add a button in your layout
            if (isStoragePermissionGranted()) {
                openDirectoryPicker()
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
     * Best‑effort conversion of a SAF tree‑URI to an absolute filesystem path.
     * Returns null on failure.
     */
    private fun Uri.toFullPath(): String? {
        // Only works if the URI represents primary storage and the app has broad storage access.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            && DocumentsContract.isDocumentUri(this@MainActivity, this)
        ) {
            val docId = DocumentsContract.getTreeDocumentId(this)
            val split = docId.split(':')
            if (split.size >= 2 && split[0].equals("primary", true)) {
                return File(Environment.getExternalStorageDirectory(), split[1]).path
            }
        }
        return null
    }
}

//package com.example.myapplication
//
//import android.Manifest
//import android.app.Activity
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.os.Environment
//import android.provider.Settings
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.appcompat.app.AppCompatActivity
//import androidx.core.app.ActivityCompat
//import androidx.core.content.ContextCompat
//import com.example.myapplication.databinding.ActivityMainBinding
//
//
//class MainActivity : AppCompatActivity() {
//
//    private lateinit var binding: ActivityMainBinding
//
//    companion object {
//        private const val TAG = "MainActivity"
//    }
//
//    private val storageActivityResultLauncher =
//        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//                if (Environment.isExternalStorageManager()) {
//                    Log.d(TAG, "Manage External Storage permission granted")
//                    // proceed with the work that needs the permission
//                } else {
//                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
//                }
//            } else {
//                if (result.resultCode == Activity.RESULT_OK &&
//                    ContextCompat.checkSelfPermission(
//                        this,
//                        Manifest.permission.READ_EXTERNAL_STORAGE
//                    ) == PackageManager.PERMISSION_GRANTED
//                ) {
//                    Log.d(TAG, "Legacy storage permission granted")
//                } else {
//                    Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        binding = ActivityMainBinding.inflate(layoutInflater)
//        setContentView(binding.root)
//
//        // Example of a call to a native method
//
//        val folderPath = "/sdcard/Download/libs" // replace with valid path
//        val result = NativeLib.scanDirectory(folderPath)
//
//        binding.sampleText.text = result
//
//
//    }
//
//    private val STORAGE_PERMISSION_CODE = 23
//
//    private fun requestForStoragePermissions() {
//        //Android is 11 (R) or above
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            try {
//                val intent = Intent()
//                intent.action = Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
//                val uri: Uri = Uri.fromParts("package", this.packageName, null)
//                intent.data = uri
//                storageActivityResultLauncher.launch(intent)
//            } catch (e: Exception) {
//                val intent = Intent()
//                intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
//                storageActivityResultLauncher.launch(intent)
//            }
//        } else {
//            //Below android 11
//            ActivityCompat.requestPermissions(
//                this, arrayOf(
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//                ),
//                STORAGE_PERMISSION_CODE
//            )
//        }
//    }
//}