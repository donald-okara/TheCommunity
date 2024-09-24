package com.example.thecommunity.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.example.thecommunity.data.model.Community
import com.example.thecommunity.data.model.Event
import com.example.thecommunity.data.model.Space
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class FirebaseService(
    private val firestore: FirebaseFirestore,
    private val context: Context
) {
    private val storageInstance: FirebaseStorage by lazy {
        FirebaseStorage.getInstance()
    }

    private suspend fun getCommunitiesByStatus(status: String): List<Community> {
        return try {
            val snapshot = firestore.collection("communities")
                .whereEqualTo("status", status)
                .get(Source.DEFAULT)
                .await()
            snapshot.toObjects(Community::class.java)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error fetching communities by status", e)
            emptyList()
        }
    }

    fun observeCommunitiesByStatusRefresh(
        status: String,
        refreshIntervalMillis: Long = 600_000L // 10 minutes by default
    ): Flow<List<Community>> = flow {
        while (true) {
            val communities = getCommunitiesByStatus(status)
            emit(communities)
            delay(refreshIntervalMillis)
        }
    }

    private suspend fun getEventsByCommunityOrSpace(
        communityId: String?,
        spaceId: String?
    ): List<Event> {
        return try {
            var query: Query = firestore.collection("events")

            if (spaceId != null) {
                query = query.whereEqualTo("spaceId", spaceId)
            } else if (communityId != null) {
                query = query.whereEqualTo("communityId", communityId)
            }

            val snapshot = query
                .get(Source.DEFAULT)
                .await()

            snapshot.toObjects(Event::class.java)
        } catch (e: Exception) {
            Log.e("FirebaseService", "Error fetching events by communityId or spaceId", e)
            emptyList()
        }
    }




    fun observeEventsByCommunityOrSpace(
        communityId: String?,
        spaceId: String?,
        refreshIntervalMillis: Long = 600_000L // 10 minutes by default
    ): Flow<List<Event>> = flow {
        while (true) {
            val events = getEventsByCommunityOrSpace(communityId, spaceId)
            emit(events)
            delay(refreshIntervalMillis)
        }
    }

    private suspend fun getAllCommunities(firestore: FirebaseFirestore): List<Community> {
        return try {
            val snapshot = firestore.collection("communities")
                .get(Source.DEFAULT)
                .await()
            snapshot.toObjects(Community::class.java)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error fetching communities", e)
            emptyList()
        }
    }


    fun observeAllCommunitiesRefresh(
        refreshIntervalMillis: Long = 600_000L // 10 minutes by default
    ): Flow<List<Community>> = flow {
        while (true) {
            val communities = getAllCommunities(firestore = firestore)
            emit(communities)
            delay(refreshIntervalMillis)
        }
    }

    suspend fun getSpaceByStatus(status: String,parentCommunity: String): List<Community> {
        return try {
            val snapshot = firestore.collection("spaces")
                .whereEqualTo(
                    "status", status,
                )
                .whereEqualTo(
                    "parentCommunity", parentCommunity

                )
                .get()
                .await()
            snapshot.toObjects(Community::class.java)
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Error fetching communities by status", e)
            emptyList()
        }
    }
    private suspend fun getAllSpaces(): List<Space> {
        return try {
            val snapshot = firestore.collection("spaces")
                .get()
                .await()
            snapshot.toObjects(Space::class.java)
        } catch (e: Exception) {
            Log.e("SpaceRepository", "Error fetching communities by status", e)
            emptyList()
        }
    }

    fun observeSpacesByStatusRefresh(
        refreshIntervalMillis: Long = 600_000L // 10 minutes by default
    ): Flow<List<Space>> = flow {
        while (true) {
            val spaces = getAllSpaces()
            emit(spaces)
            delay(refreshIntervalMillis)
        }
    }

    suspend fun uploadImageToStorage(
        uri: Uri,
        path: String
    ): String? {
        return try {
            Log.d("CommunityRepository", "Uploading image with URI: $uri")

            val file = uriToCompressedFile(uri) // Convert URI to compressed file
            if (file == null || !file.exists()) {
                Log.e("CommunityRepository", "File does not exist: $file")
                return null
            }

            val storageRef = FirebaseStorage.getInstance().reference.child(path)

            // Upload the file
            file.inputStream().use { inputStream ->
                val uploadTask = storageRef.putStream(inputStream).await()
                // Verify upload success
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d("CommunityRepository", "Uploaded image to $path: $downloadUrl")
                downloadUrl
            }
        } catch (e: Exception) {
            Log.e("CommunityRepository", "Failed to upload image: ${e.message}", e)
            null
        }
    }


    // Convert URI to a compressed file
    private fun uriToCompressedFile(uri: Uri): File? {
        val context = context
        val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
        inputStream.close()

        val file = File.createTempFile("compressed_image", ".jpg", context.cacheDir)
        FileOutputStream(file).use {
            val maxSize = 1_500_000 // 1.5 MB
            var quality = 100 // Starting quality

            // Compress the image until it's under the maximum size
            while (quality > 0) {
                file.outputStream().use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos)
                }
                if (file.length() <= maxSize) {
                    return file
                }
                file.delete() // Delete the file and try again with lower quality
                quality -= 10
            }
        }

        return null
    }

    // Function to delete an image from Firebase Storage given its URL
    fun deleteImageFromStorage(imageUrl: String) {
        try {
            // Get a reference to the storage file using the image URL
            val storageReference = storageInstance.getReferenceFromUrl(imageUrl)

            // Delete the file from Firebase Storage
            storageReference.delete().addOnSuccessListener {
                // File deleted successfully
                Log.d("FirebaseService", "Image deleted successfully: $imageUrl")
            }.addOnFailureListener { exception ->
                // Handle any errors
                Log.e("FirebaseService", "Failed to delete image: $imageUrl", exception)
            }

        } catch (e: Exception) {
            Log.e("FirebaseService", "Error deleting image from storage", e)
        }
    }

}