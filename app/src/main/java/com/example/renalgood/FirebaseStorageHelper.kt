package com.example.renalgood

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class FirebaseStorageHelper {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    suspend fun uploadNutriologoImages(
        nutriologoId: String,
        identificacionUri: Uri?,
        selfieUri: Uri?,
        profileUri: Uri?
    ): Triple<String?, String?, String?> = coroutineScope {
        val baseFolder = "verificacion/$nutriologoId"

        // Subir las imágenes en paralelo
        val identificacionJob = async {
            identificacionUri?.let {
                uploadImage("$baseFolder/identificacion.jpg", it)
            }
        }
        val selfieJob = async {
            selfieUri?.let {
                uploadImage("$baseFolder/selfie.jpg", it)
            }
        }
        val profileJob = async {
            profileUri?.let {
                uploadImage("$baseFolder/profile.jpg", it)
            }
        }

        // Esperar que todas las subidas terminen
        Triple(
            identificacionJob.await(),
            selfieJob.await(),
            profileJob.await()
        )
    }

    private suspend fun uploadImage(path: String, uri: Uri): String? {
        return try {
            val imageRef = storageRef.child(path)
            val uploadTask = imageRef.putFile(uri)

            // Esperar que termine la subida
            uploadTask.await()

            // Obtener la URL de descarga
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Error uploading image: ${e.message}")
            null
        }
    }

    suspend fun getNutriologoImages(nutriologoId: String): Triple<String?, String?, String?> {
        return coroutineScope {
            val baseFolder = "verificacion/$nutriologoId"

            val identificacionJob = async {
                getImageUrl("$baseFolder/identificacion.jpg")
            }
            val selfieJob = async {
                getImageUrl("$baseFolder/selfie.jpg")
            }
            val profileJob = async {
                getImageUrl("$baseFolder/profile.jpg")
            }

            Triple(
                identificacionJob.await(),
                selfieJob.await(),
                profileJob.await()
            )
        }
    }

    private suspend fun getImageUrl(path: String): String? {
        return try {
            val imageRef = storageRef.child(path)
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirebaseStorage", "Error getting image URL: ${e.message}")
            null
        }
    }
}