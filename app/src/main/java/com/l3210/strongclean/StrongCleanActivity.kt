package com.l3210.strongclean

import android.app.AlertDialog
import androidx.activity.ComponentActivity
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.widget.*
import androidx.lifecycle.lifecycleScope
import info.tekware.aereset.service.PrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class StrongCleanActivity : ComponentActivity() {
    private lateinit var service: PrinterService
    private var connection: PrinterService.ConnectionState? = null
    private lateinit var status: TextView
    private lateinit var connect: Button
    private lateinit var strong: Button
    private lateinit var nozzle: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        service = PrinterService(this) { msg -> runOnUiThread { status.text = msg } }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 40, 32, 32)
        }
        val title = TextView(this).apply {
            text = "EPSON L3210\nStrong Cleaning"
            textSize = 24f
            setPadding(0, 0, 0, 24)
        }
        status = TextView(this).apply { text = "Connect Epson L3210 by USB OTG"; textSize = 16f }
        connect = Button(this).apply { text = "CONNECT" }
        strong = Button(this).apply { text = "STRONG CLEANING"; isEnabled = false }
        nozzle = Button(this).apply { text = "NOZZLE CHECK"; isEnabled = false }

        box.addView(title); box.addView(status)
        box.addView(connect); box.addView(strong); box.addView(nozzle)
        setContentView(box)

        connect.setOnClickListener { discoverAndConnect() }
        strong.setOnClickListener { confirmStrongCleaning() }
        nozzle.setOnClickListener { runNozzleCheck() }

        @Suppress("DEPRECATION")
        val device = intent?.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
        if (device != null) connect(device)
    }

    private fun discoverAndConnect() {
        lifecycleScope.launch {
            try {
                status.text = "Searching for Epson printer..."
                val device = withContext(Dispatchers.IO) {
                    service.detectPrinters().firstOrNull()
                } ?: error("No Epson printer detected")
                connect(device)
            } catch (e: Exception) {
                status.text = "Error: ${e.message}"
            }
        }
    }

    private fun connect(device: UsbDevice) {
        lifecycleScope.launch {
            try {
                status.text = "Connecting..."
                connection?.let { service.close(it) }
                val c = withContext(Dispatchers.IO) { service.connectPrinter(device) }
                connection = c
                status.text = "Connected: ${c.spec.modelName}"
                strong.isEnabled = c.spec.cleaningOptions.any { it.name == "Strong % Cleaning" }
                nozzle.isEnabled = true
                if (!strong.isEnabled) status.text = "Connected, but Strong % Cleaning is not listed"
            } catch (e: Exception) {
                status.text = "Connection error: ${e.message}"
            }
        }
    }

    private fun confirmStrongCleaning() {
        AlertDialog.Builder(this)
            .setTitle("Strong Cleaning")
            .setMessage(
                "This sends the L3200/L3210 'Strong % Cleaning' service command. " +
                "It may consume significant ink. Make sure all tanks are at least 1/3 full. Continue?"
            )
            .setNegativeButton("CANCEL", null)
            .setPositiveButton("CONTINUE") { _, _ -> runStrongCleaning() }
            .show()
    }

    private fun runStrongCleaning() {
        val c = connection ?: return
        strong.isEnabled = false
        nozzle.isEnabled = false
        status.text = "Sending Strong Cleaning..."
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    service.runHeadCleaning(c, "Strong % Cleaning")
                }
                status.text = "Command sent. Wait for printer to finish, then print a nozzle check."
            } catch (e: Exception) {
                status.text = "Strong Cleaning error: ${e.message}"
            } finally {
                strong.isEnabled = true
                nozzle.isEnabled = true
            }
        }
    }

    private fun runNozzleCheck() {
        val c = connection ?: return
        nozzle.isEnabled = false
        status.text = "Sending nozzle check..."
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    service.printNozzleCheck(c, 0)
                }
                status.text = "Nozzle check sent."
            } catch (e: Exception) {
                status.text = "Nozzle check error: ${e.message}"
            } finally {
                nozzle.isEnabled = true
            }
        }
    }

    override fun onDestroy() {
        connection?.let { service.close(it) }
        connection = null
        super.onDestroy()
    }
}
