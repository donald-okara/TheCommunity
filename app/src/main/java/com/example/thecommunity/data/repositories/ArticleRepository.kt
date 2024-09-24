package com.example.thecommunity.data.repositories

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.example.thecommunity.data.FirebaseService
import com.example.thecommunity.data.model.Article
import com.example.thecommunity.data.model.ArticleDetails
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ArticleRepository(
    private val db : FirebaseFirestore,
    private val firebaseService: FirebaseService,
    private val context: Context
) {
    private val Tag = "ArticleRepository"

    /**
     * CREATE
     */
    suspend fun publishArticleToDraft(
        author: String,
        title: String,
        topic: String,
        communityId: String?,
        spaceId: String?,
        body: String?,
        imageUris: List<Uri>?
    ): Pair<String?, Boolean> {
        // Generate a new ID for the article
        val articleId = db.collection("articles").document().id

        // Upload images and get their URLs with metadata
        val imageMetadataList = imageUris?.map { uri ->
            val imageName = "${UUID.randomUUID()}.jpg" // Generate a unique name
            val imageUrl = firebaseService.uploadImageToStorage(
                uri,
                "events/images/${articleId}/$imageName"
            )
            mapOf("name" to imageName, "url" to imageUrl)
        } ?: emptyList()

        // Create the Article object
        val article = Article(
            id = articleId,
            author = author,
            topic = topic,
            title = title,
            draft = true,
            communityId = communityId,
            spaceId = spaceId,
            body = body,
            images = imageMetadataList,
        )

        return try {
            // Start a batch operation
            val batch = db.batch()

            // Add the new article to the articles collection
            val articleRef = db.collection("articles").document(articleId)
            batch.set(articleRef, article)

            // Optionally, update the community or space with the new article reference
            communityId?.let {
                val communityRef = db.collection("communities").document(it)
                val communityDoc = communityRef.get().await()
                val currentArticles = communityDoc.get("articles") as? List<String> ?: emptyList()
                val updatedArticles = currentArticles.toMutableList().apply { add(articleId) }
                batch.update(communityRef, "articles", updatedArticles)
            }

            spaceId?.let {
                val spaceRef = db.collection("spaces").document(it)
                val spaceDoc = spaceRef.get().await()
                val currentArticles = spaceDoc.get("articles") as? List<String> ?: emptyList()
                val updatedArticles = currentArticles.toMutableList().apply { add(articleId) }
                batch.update(spaceRef, "articles", updatedArticles)
            }

            // Commit the batch
            batch.commit().await()

            Log.d(Tag, "Article created successfully: $articleId")
            Toast.makeText(context, "Article created successfully", Toast.LENGTH_SHORT).show()
            // Return the article ID and success as true
            Pair(articleId, true)

        } catch (e: Exception) {
            Log.e(Tag, "Error creating Article: $articleId", e)
            // Return null for the article ID and success as false in case of error
            Pair(null, false)
        }
    }


    suspend fun getArticleById(articleId: String): Article? {
        return try {
            val documentSnapshot = db.collection("articles")
                .document(articleId)
                .get(Source.DEFAULT)
                .await()

            // Convert the document snapshot to a Community object
            documentSnapshot.toObject(Article::class.java)
        } catch (e: Exception) {
            Log.e(Tag, "Error fetching document by ID", e)
            null
        }
    }

    suspend fun getArticleDetails(article: Article): ArticleDetails? {
        return try {
            // Assuming the author, community, and space details are stored in separate collections.
            val authorId = article.author // Assuming article contains authorId
            val communityId = article.communityId // Assuming article contains communityId
            val spaceId = article.spaceId // Assuming article contains spaceId

            // Fetch the author details
            val authorDocument =db.collection("users")
                .document(authorId)
                .get()
                .await()

            // Fetch the community details
            val communityDocument = communityId?.let {
                db.collection("communities")
                    .document(it)
                    .get()
                    .await()
            }

            // Fetch the space details
            val spaceDocument = spaceId?.let {
                db.collection("spaces")
                    .document(it)
                    .get()
                    .await()
            }

            // Construct the ArticleDetails object
            ArticleDetails(
                authorProfile = authorDocument.getString("profilePictureUrl"),
                authorUserName = authorDocument.getString("username"),
                communityProfileUri = communityDocument?.getString("profileUrl"),
                spaceProfileUri = spaceDocument?.getString("profileUri"),
                communityName = communityDocument?.getString("name"),
                spaceName = spaceDocument?.getString("name"),
                article = article
            )
        } catch (e: Exception) {
            Log.e(Tag, "Error fetching document by ID", e)
            null
        }
    }

    suspend fun publishArticleToPublic(
        articleId: String
    ): Boolean {
        return try {
            // Update the 'draft' field to false
            db.collection("articles")
                .document(articleId)
                .update("draft", false)
                .await()  // Use await to suspend the coroutine until the operation is complete
            // You can log success or handle it accordingly
            Log.d("publishArticleToPublic", "Article published to public successfully.")
            true
        } catch (e: Exception) {
            // Handle any errors
            Log.e("publishArticleToPublic", "Failed to publish article", e)
            false

        }


    }




    
}