package com.hectordev.mvp.data.util

import android.Manifest
import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.hectordev.mvp.MVPApplication
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.firebase.firestore.FirebaseFirestore
import com.hectordev.mvp.R
import com.hectordev.mvp.domain.DiplomaData
import com.hectordev.mvp.domain.repository.DiplomaRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiplomaGenerator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore
) : DiplomaRepository {

    override suspend fun generateAndSave(data: DiplomaData): String = withContext(Dispatchers.IO) {
        val photoBitmap = loadPhoto(data.galaPhotoUrl)
        val winnerVotes = fetchWinnerVotes(data.eventId, data.winnerId)

        val pageWidth = 842
        val pageHeight = 595
        val pdfDocument = PdfDocument()
        val page = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        )
        drawPage(page.canvas, data, photoBitmap, winnerVotes, pageWidth, pageHeight)
        pdfDocument.finishPage(page)

        val sanitized = data.eventTitle
            .replace(Regex("[^\\p{L}\\p{N}\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "_")
        val fileName = "mvp_certificate_$sanitized.pdf"

        val savedUri = saveToDownloads(pdfDocument, fileName)
        pdfDocument.close()

        showDownloadNotification(fileName, savedUri)
        fileName
    }

    private fun saveToDownloads(pdfDocument: PdfDocument, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            resolver.openOutputStream(uri)?.use { pdfDocument.writeTo(it) }
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            uri
        } else {
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            dir.mkdirs()
            val file = File(dir, fileName)
            FileOutputStream(file).use { pdfDocument.writeTo(it) }
            Uri.fromFile(file)
        }
    }

    private fun showDownloadNotification(fileName: String, fileUri: Uri?) {
        val canNotify = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
        } else true

        if (!canNotify || fileUri == null) return

        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MVPApplication.CHANNEL_CERTIFICATES)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.diploma_notification_title))
            .setContentText(fileName)
            .setSubText(context.getString(R.string.diploma_notification_tap_to_open))
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(fileName.hashCode(), notification)
    }

    private suspend fun loadPhoto(url: String?): Bitmap? {
        url ?: return null
        return try {
            val request = ImageRequest.Builder(context).data(url).allowHardware(false).build()
            (context.imageLoader.execute(request) as? SuccessResult)?.drawable?.toBitmap()
        } catch (e: Exception) { null }
    }

    private suspend fun fetchWinnerVotes(eventId: String, winnerId: String): Int {
        return try {
            firestore.collection("events").document(eventId)
                .collection("final_votes")
                .whereEqualTo("votedForCandidateId", winnerId)
                .get().await().size()
        } catch (e: Exception) { 0 }
    }

    private fun drawPage(
        canvas: Canvas,
        data: DiplomaData,
        photo: Bitmap?,
        winnerVotes: Int,
        width: Int,
        height: Int
    ) {
        val cx = width / 2f
        val margin = 52f

        // ── Background ──────────────────────────────────────────────────
        canvas.drawColor(android.graphics.Color.WHITE)

        val navy = android.graphics.Color.parseColor("#1A1A2E")
        val gold = android.graphics.Color.parseColor("#C9A84C")
        val grey = android.graphics.Color.parseColor("#666666")
        val bodyColor = android.graphics.Color.parseColor("#333333")
        val lineColor = android.graphics.Color.parseColor("#DDDDDD")

        // Outer border (navy)
        val outerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = navy; style = Paint.Style.STROKE; strokeWidth = 3f
        }
        canvas.drawRect(16f, 16f, width - 16f, height - 16f, outerBorder)

        // Inner border (gold)
        val innerBorder = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold; style = Paint.Style.STROKE; strokeWidth = 1.5f
        }
        canvas.drawRect(22f, 22f, width - 22f, height - 22f, innerBorder)

        // Corner ornaments — small gold squares at each corner
        val ornamentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = gold; style = Paint.Style.FILL }
        val orn = 5f
        listOf(
            RectF(13f, 13f, 13f + orn, 13f + orn),
            RectF(width - 13f - orn, 13f, width - 13f, 13f + orn),
            RectF(13f, height - 13f - orn, 13f + orn, height - 13f),
            RectF(width - 13f - orn, height - 13f - orn, width - 13f, height - 13f)
        ).forEach { canvas.drawRect(it, ornamentPaint) }

        // ── Helper paints ────────────────────────────────────────────────
        fun centeredPaint(size: Float, color: Int, bold: Boolean = false) =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                textSize = size
                typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
                textAlign = Paint.Align.CENTER
            }

        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lineColor; strokeWidth = 1f
        }
        val goldLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = gold; strokeWidth = 1f
        }

        // ── Header ───────────────────────────────────────────────────────
        var y = margin + 36f

        canvas.drawText(
            context.getString(R.string.diploma_winner_label),
            cx, y, centeredPaint(15f, grey)
        )
        y += 28f
        canvas.drawText(
            data.eventTitle,
            cx, y, centeredPaint(22f, navy, bold = true)
        )
        y += 26f

        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        val dateStr = "${dateFormat.format(Date(data.startDate))} – ${dateFormat.format(Date(data.endDate))}"
        canvas.drawText(dateStr, cx, y, centeredPaint(13f, grey))
        y += 14f

        // Gold divider
        canvas.drawLine(margin + 40f, y, width - margin - 40f, y, goldLinePaint)
        y += 14f

        // ── Gala photo ───────────────────────────────────────────────────
        if (photo != null) {
            val maxPhotoH = 200f
            val maxPhotoW = width - margin * 2
            val ratio = photo.width.toFloat() / photo.height.toFloat()
            val photoH = minOf(maxPhotoH, maxPhotoW / ratio)
            val photoW = photoH * ratio
            val left = cx - photoW / 2
            canvas.drawBitmap(photo, null, RectF(left, y, left + photoW, y + photoH), null)
            y += photoH + 14f
        } else {
            y += 20f
        }

        // ── Certificate text ─────────────────────────────────────────────
        canvas.drawText(context.getString(R.string.diploma_certifies_line1), cx, y, centeredPaint(15f, bodyColor))
        y += 28f
        canvas.drawText(data.winnerName, cx, y, centeredPaint(26f, navy, bold = true))
        y += 26f
        canvas.drawText(context.getString(R.string.diploma_certifies_line2), cx, y, centeredPaint(15f, bodyColor))
        y += 18f

        // Gold divider
        canvas.drawLine(margin + 40f, y, width - margin - 40f, y, goldLinePaint)
        y += 14f

        // ── Vote description ─────────────────────────────────────────────
        val voteDescription = when {
            winnerVotes == data.totalParticipants -> context.getString(R.string.diploma_vote_unanimous)
            winnerVotes * 4 >= data.totalParticipants * 3 -> context.getString(R.string.diploma_vote_overwhelming)
            winnerVotes * 2 >= data.totalParticipants -> context.getString(R.string.diploma_vote_clear)
            else -> context.getString(R.string.diploma_vote_hard_fought)
        }
        canvas.drawText(voteDescription, cx, y, centeredPaint(12f, grey))

        // ── MVP seal (bottom-right) ──────────────────────────────────────
        val sealSize = 80f
        val sealBitmap = context.getDrawable(R.drawable.stamp_winner)?.toBitmap()
        if (sealBitmap != null) {
            val sealLeft = width - margin - sealSize
            val sealTop = height - margin - sealSize + 10f
            canvas.drawBitmap(sealBitmap, null, RectF(sealLeft, sealTop, sealLeft + sealSize, sealTop + sealSize), null)
        }
    }
}