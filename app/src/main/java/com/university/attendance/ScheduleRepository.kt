package com.university.attendance

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ScheduleRepository(
    private val firestore: FirebaseFirestore =
        FirebaseFirestore.getInstance(),

    private val storage: FirebaseStorage =
        FirebaseStorage.getInstance()
) {

    private val scheduleRef =
        firestore.collection("schedule")

    // ============================================================
    // RESULT
    // ============================================================

    sealed class OpResult {

        data class Success(
            val scheduleId: String
        ) : OpResult()

        data class Error(
            val message: String,
            val exception: Exception? = null
        ) : OpResult()
    }

    // ============================================================
    // GET SCHEDULE FOR TEACHER
    // ============================================================

    suspend fun getScheduleForTeacher(
        teacherAuthUid: String
    ): Schedule? {

        if (teacherAuthUid.isBlank()) {
            return null
        }

        val snapshot =
            scheduleRef
                .whereEqualTo(
                    "teacherAuthUid",
                    teacherAuthUid
                )
                .limit(1)
                .get()
                .await()

        if (snapshot.isEmpty) {
            return null
        }

        val document =
            snapshot.documents.first()

        return document
            .toObject(Schedule::class.java)
            ?.apply {
                scheduleId = document.id
            }
    }

    // ============================================================
    // UPLOAD PDF + SAVE / UPDATE FIRESTORE
    // ============================================================

    suspend fun saveSchedule(
        teacherAuthUid: String,
        teacherName: String,
        fileName: String,
        note: String,
        pdfUri: Uri
    ): OpResult {

        var uploadedStoragePath: String? = null

        return try {

            // ====================================================
            // VALIDATION
            // ====================================================

            if (teacherAuthUid.isBlank()) {
                return OpResult.Error(
                    "Teacher Firebase Auth UID is missing."
                )
            }

            if (fileName.isBlank()) {
                return OpResult.Error(
                    "Please enter a file name."
                )
            }

            // ====================================================
            // FIND EXISTING SCHEDULE
            // ====================================================

            val existingSnapshot =
                scheduleRef
                    .whereEqualTo(
                        "teacherAuthUid",
                        teacherAuthUid
                    )
                    .limit(1)
                    .get()
                    .await()

            val existingDocument =
                if (!existingSnapshot.isEmpty) {
                    existingSnapshot.documents.first()
                } else {
                    null
                }

            val oldSchedule =
                existingDocument
                    ?.toObject(Schedule::class.java)

            // ====================================================
            // CLEAN FILE NAME
            // ====================================================

            var cleanFileName =
                fileName
                    .trim()
                    .replace("/", "_")
                    .replace("\\", "_")

            if (
                !cleanFileName
                    .lowercase()
                    .endsWith(".pdf")
            ) {
                cleanFileName += ".pdf"
            }

            // ====================================================
            // UNIQUE STORAGE FILE NAME
            //
            // Same teacher can upload a new PDF without
            // accidentally overwriting the previous file.
            // ====================================================

            val uniqueFileName =
                "${UUID.randomUUID()}_$cleanFileName"

            // ====================================================
            // FIREBASE STORAGE PATH
            // ====================================================

            val storagePath =
                "schedules/$teacherAuthUid/$uniqueFileName"

            val storageRef =
                storage.reference
                    .child(storagePath)

            // ====================================================
            // PDF METADATA
            // ====================================================

            val metadata =
                StorageMetadata.Builder()
                    .setContentType("application/pdf")
                    .build()

            // ====================================================
            // UPLOAD PDF
            // ====================================================

            storageRef
                .putFile(
                    pdfUri,
                    metadata
                )
                .await()

            uploadedStoragePath =
                storagePath

            // ====================================================
            // GET DOWNLOAD URL
            // ====================================================

            val downloadUrl =
                storageRef
                    .downloadUrl
                    .await()
                    .toString()

            // ====================================================
            // FIRESTORE DATA
            // ====================================================

            val schedule =
                Schedule(

                    teacherAuthUid =
                        teacherAuthUid,

                    teacherName =
                        teacherName.trim(),

                    fileName =
                        cleanFileName,

                    note =
                        note.trim(),

                    uploadedBy =
                        "Admin",

                    storagePath =
                        storagePath,

                    fileUrl =
                        downloadUrl
                )

            // ====================================================
            // UPDATE EXISTING SCHEDULE
            // ====================================================

            if (existingDocument != null) {

                scheduleRef
                    .document(existingDocument.id)
                    .set(schedule.toMap())
                    .await()

                // ----------------------------------------------
                // DELETE OLD PDF
                // ----------------------------------------------

                val oldStoragePath =
                    oldSchedule?.storagePath

                if (
                    !oldStoragePath.isNullOrBlank() &&
                    oldStoragePath != storagePath
                ) {

                    try {

                        storage.reference
                            .child(oldStoragePath)
                            .delete()
                            .await()

                    } catch (_: Exception) {

                        // Old file deletion failure should
                        // not make the new upload fail.
                    }
                }

                return OpResult.Success(
                    scheduleId =
                        existingDocument.id
                )
            }

            // ====================================================
            // CREATE NEW SCHEDULE
            // ====================================================

            val newDocument =
                scheduleRef.document()

            newDocument
                .set(schedule.toMap())
                .await()

            OpResult.Success(
                scheduleId =
                    newDocument.id
            )

        } catch (e: Exception) {

            // ====================================================
            // CLEANUP IF FIRESTORE FAILED AFTER STORAGE UPLOAD
            // ====================================================

            if (!uploadedStoragePath.isNullOrBlank()) {

                try {

                    storage.reference
                        .child(
                            uploadedStoragePath!!
                        )
                        .delete()
                        .await()

                } catch (_: Exception) {
                    // Ignore cleanup failure.
                }
            }

            // ====================================================
            // RETURN ERROR
            // ====================================================

            OpResult.Error(

                message =
                    e.message
                        ?: "Failed to upload schedule PDF.",

                exception =
                    e
            )
        }
    }
}