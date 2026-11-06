package com.language.finebulary.di

import android.content.Context
import com.language.finebulary.data.api.DatamuseApiService
import com.language.finebulary.data.api.GeminiApiService
import com.language.finebulary.data.api.OpenAiApiService
import com.language.finebulary.data.api.PexelsApiService
import com.language.finebulary.data.db.AppDatabase
import com.language.finebulary.data.dao.VocabularyDao
import com.language.finebulary.data.pref.UserPreferences
import com.language.finebulary.repository.CourseRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideVocabularyDao(database: AppDatabase): VocabularyDao {
        return database.vocabularyDao()
    }

    @Provides
    @Singleton
    fun provideRepository(vocabularyDao: VocabularyDao): CourseRepository {
        return CourseRepository(vocabularyDao)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSpeechRecognizer(@ApplicationContext context: Context): com.language.finebulary.util.SpeechRecognizerManager {
        return com.language.finebulary.util.SpeechRecognizerManager(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun providePexelsApi(client: OkHttpClient): PexelsApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.pexels.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.language.finebulary.data.api.PexelsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApi(client: OkHttpClient): GeminiApiService {
        return Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.language.finebulary.data.api.GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDatamuseApi(client: OkHttpClient): DatamuseApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.datamuse.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.language.finebulary.data.api.DatamuseApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideOpenAiApi(client: OkHttpClient): OpenAiApiService {
        return Retrofit.Builder()
            .baseUrl("https://api.openai.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
            .create(com.language.finebulary.data.api.OpenAiApiService::class.java)
    }
}
