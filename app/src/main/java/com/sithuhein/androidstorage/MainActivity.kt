package com.sithuhein.androidstorage

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.lang.Exception
import java.util.*


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {


    private lateinit var internalStoragePhotoAdapter: InternalStoragePhotoAdapter
    private lateinit var sharedStoragePhotoAdapter : SharedPhotoAdapter


    private var isReadPermissionGranted = false
    private var isWritePermissionGranted = false

    private lateinit var permissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var intentSenderLauncher : ActivityResultLauncher<IntentSenderRequest>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        internalStoragePhotoAdapter = InternalStoragePhotoAdapter {
            val isDeletedSuccessful = deletePhotoFromInternalStorage(it.name)
            if (isDeletedSuccessful) {
                loadInternalStoragePhotosIntoRecyclerview()
                Toast.makeText(this, "Photo Deleted Successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Couldn't delete photo", Toast.LENGTH_SHORT).show()
            }
        }
        setInternalStoragePhotoRecyclerview()
        loadInternalStoragePhotosIntoRecyclerview()

        sharedStoragePhotoAdapter = SharedPhotoAdapter{
            lifecycleScope.launch {
                deletePhotoFromSharedStorage(it.contentUri)
            }
        }

        setSharedStoragePhotoRecyclerview()
        loadSharedStoragePhotosIntoRecyclerview()

        // its being used now since startActivityForResult has been deprecated
        val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) {
            val isPrivatStorage = switch_private.isChecked

            val isSuccessfullySaved = when {
                isPrivatStorage -> saveIntoInternalStorage(UUID.randomUUID().toString(), it)
                isWritePermissionGranted -> saveIntoSharedStorage(UUID.randomUUID().toString(), it)
                else -> false
            }
            if (isSuccessfullySaved) {
                loadInternalStoragePhotosIntoRecyclerview()
                loadSharedStoragePhotosIntoRecyclerview()
                Toast.makeText(this, "Photo saved successfully!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Couldn't save photo", Toast.LENGTH_SHORT).show()
            }

        }

        intentSenderLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()){
            if(it.resultCode == Activity.RESULT_OK){
                Toast.makeText(this, "Photo Deleted successfully.", Toast.LENGTH_SHORT).show()
            }else {
                Toast.makeText(this, "Photo couldn't be deleted!", Toast.LENGTH_SHORT).show()
            }
        }


        permissionsLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                isReadPermissionGranted =
                    permissions[android.Manifest.permission.READ_EXTERNAL_STORAGE]
                        ?: isReadPermissionGranted
                isWritePermissionGranted =
                    permissions[android.Manifest.permission.WRITE_EXTERNAL_STORAGE]
                        ?: isWritePermissionGranted

            }
        updateOrRequestPermissions()



        btn_camera.setOnClickListener {
            takePhoto.launch()
        }

    }


    private fun updateOrRequestPermissions() {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val permissionToRequest = mutableListOf<String>()
        val isSdk29AndAbove = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

        isReadPermissionGranted = hasReadPermission
        isWritePermissionGranted = hasWritePermission || isSdk29AndAbove

        if (!isReadPermissionGranted) {
            permissionToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!isWritePermissionGranted) {
            permissionToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionToRequest.toTypedArray())
        }
    }


    private fun setInternalStoragePhotoRecyclerview() {
        rv_internal_storage_photo.apply {
            adapter = internalStoragePhotoAdapter
            //layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL , false)
            layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
        }
    }


    private fun setSharedStoragePhotoRecyclerview() {
        rv_shared_storage_photo.apply {
            adapter = sharedStoragePhotoAdapter
            //layoutManager = LinearLayoutManager(this@MainActivity, LinearLayoutManager.HORIZONTAL , false)
            layoutManager = StaggeredGridLayoutManager(3, RecyclerView.VERTICAL)
        }
    }

    private fun loadInternalStoragePhotosIntoRecyclerview() {
        lifecycleScope.launch {
            val photos = loadPhotosFromInternalStorage()
            internalStoragePhotoAdapter.setNewData(photos)
        }
    }

    private fun loadSharedStoragePhotosIntoRecyclerview() {
        lifecycleScope.launch {
            val photos = loadPhotosFromSharedStorage()
            sharedStoragePhotoAdapter.setNewData(photos)
        }
    }

    private fun deletePhotoFromInternalStorage(fileName: String): Boolean {
        return try {
            deleteFile(fileName)
        } catch (e: Exception) {
            e.stackTrace
            false
        }
    }


    private suspend fun deletePhotoFromSharedStorage(photoUri : Uri) {
        withContext(Dispatchers.IO){
            try {
                // this deletion is for api 28 and below
                contentResolver.delete(photoUri, null, null)
            }catch (e : SecurityException){
                val intentSender = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        MediaStore.createDeleteRequest(contentResolver, listOf(photoUri)).intentSender
                    }

                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        val recoverableSecurityException = e as? RecoverableSecurityException
                        recoverableSecurityException?.userAction?.actionIntent?.intentSender
                    }

                    else -> null
                }

                intentSender?.let { sender ->
                    intentSenderLauncher.launch(
                        IntentSenderRequest.Builder(sender).build()
                    )
                }
            }
        }
    }


    private suspend fun loadPhotosFromInternalStorage(): List<InternalStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val files = filesDir.listFiles()
            files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }?.map {
                val bytes = it.readBytes()
                val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                InternalStoragePhoto(it.name, bmp)
            } ?: listOf()
        }
    }

    private suspend fun loadPhotosFromSharedStorage(): List<SharedStoragePhoto> {
        return withContext(Dispatchers.IO) {
            val collection = sdk29AndUp {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI


            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
            )

            val photos = mutableListOf<SharedStoragePhoto>()
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)

                while(cursor.moveToNext()){
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(displayNameColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    photos.add(SharedStoragePhoto(id, displayName, width, height, contentUri))

                }
                photos.toList()
            } ?: listOf()
        }
    }


    private fun saveIntoInternalStorage(fileName: String, bitmap: Bitmap): Boolean {
        return try {
            openFileOutput("$fileName.jpg", MODE_PRIVATE).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save photo!")
                }
            }
            true
        } catch (e: IOException) {
            false
        }
    }


    private fun saveIntoSharedStorage(fileName: String, bitmap: Bitmap): Boolean {
        val imageCollection = sdk29AndUp {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.jpg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        return try {
            contentResolver.insert(imageCollection, contentValues)?.also {
                contentResolver.openOutputStream(it).use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap!")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry!")

            true
        } catch (e: IOException) {
            false
        }
    }
}