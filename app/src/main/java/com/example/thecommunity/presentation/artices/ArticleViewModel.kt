package com.example.thecommunity.presentation.artices

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.thecommunity.data.model.Article
import com.example.thecommunity.data.model.ArticleDetails
import com.example.thecommunity.data.repositories.ArticleRepository
import com.example.thecommunity.data.repositories.UserRepository

class ArticleViewModel(
    private val articleRepository: ArticleRepository,
    private val userRepository: UserRepository
): ViewModel() {
    suspend fun publishArticleToDraft(
        title: String,
        topic : String,
        communityId: String?,
        spaceId: String?,
        body: String?,
        imageUris: List<Uri>?
    ): Pair<String?, Boolean> {
       return try {
           val currentUser = userRepository.getCurrentUser()

           articleRepository.publishArticleToDraft(
               author = "${currentUser?.userId}",
               title = title,
               communityId = communityId,
               spaceId = spaceId,
               body = body,
               imageUris = imageUris,
               topic = topic
           )
       }catch (e:Exception){
           Pair(null, false)
       }
    }

    suspend fun getArticleById(
        articleId: String
    ): Article? {
        return articleRepository.getArticleById(articleId)
    }

    suspend fun getArticleDetails(
        article: Article
    ): ArticleDetails?{
        return try{
            articleRepository.getArticleDetails(
                article = article
            )
        }catch (e : Exception){
            Log.e("ArticleViewModel", "Error fetching article details", e)
            null
        }
    }

    suspend fun publishArticleToPublic(
        articleId : String
    ): Boolean{
        return try {
            articleRepository.publishArticleToPublic(
                articleId = articleId
            )
        }catch (e: Exception){
            Log.e("ArticleViewModel", "Error publishing article to public", e)
            false
        }
    }
}