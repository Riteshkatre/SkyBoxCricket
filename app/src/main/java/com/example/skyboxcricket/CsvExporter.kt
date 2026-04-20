package com.example.skyboxcricket

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    fun shareMonthlyRevenue(context: Context, bookings: List<Booking>) {
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val exportFile = File(exportDir, "monthly_revenue_${System.currentTimeMillis()}.csv")

        val csvContent = buildString {
            appendLine("Customer Name,Box Selection,Booking Date Time,To Date Time,Box Price,Cafe Price,Online Amount,Offline Amount,Total Amount")
            bookings.forEach { booking ->
                appendLine(
                    listOf(
                        booking.customerName.csvCell(),
                        booking.boxSelection.csvCell(),
                        booking.bookingDateTime.csvCell(),
                        booking.toDateTime.csvCell(),
                        booking.boxPrice.toString(),
                        booking.cafePrice.toString(),
                        booking.onlineAmount.toString(),
                        booking.offlineAmount.toString(),
                        booking.totalAmount.toString(),
                    ).joinToString(",")
                )
            }
        }

        exportFile.writeText(csvContent)

        val contentUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            exportFile
        )

        val timestamp = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            .format(Date())

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "Revenue export")
            putExtra(Intent.EXTRA_TEXT, "Monthly revenue export generated on $timestamp")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.export_revenue)))
    }

    private fun String.csvCell(): String = "\"${replace("\"", "\"\"")}\""
}
