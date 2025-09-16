package com.example.fes

import android.bluetooth.*
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private val TAG = "BLETestVerbose"

    // Bluefruit UART UUIDs
    private val UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Notify
    private val RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Write

    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var statusText: TextView
    private lateinit var sendButton: Button
    private lateinit var inputText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        sendButton = findViewById(R.id.sendButton)
        inputText = findViewById(R.id.inputText)

        Log.i(TAG, "=== App started ===")
        startBleConnection()

        sendButton.setOnClickListener {
            val msg = inputText.text.toString()
            Log.i(TAG, "Send button clicked: $msg")
            sendMessage(msg)
        }
    }

    /** Step 1: Start BLE connection directly (no runtime perms on Android 8.1) */
    private fun startBleConnection() {
        val adapter = BluetoothAdapter.getDefaultAdapter()
        if (adapter == null) {
            Log.e(TAG, "No Bluetooth adapter found!")
            statusText.text = "No Bluetooth adapter"
            return
        }

        if (!adapter.isEnabled) {
            Log.w(TAG, "Bluetooth disabled")
            statusText.text = "Enable Bluetooth first"
            return
        }

        Log.i(TAG, "Bluetooth adapter found: ${adapter.name}")

        val device = adapter.bondedDevices.firstOrNull { it.name == "XIAO_UART" }
        if (device == null) {
            Log.w(TAG, "XIAO_UART not paired")
            statusText.text = "XIAO_UART not paired"
            return
        }

        Log.i(TAG, "Found paired device: ${device.name} (${device.address})")
        statusText.text = "Connecting to ${device.name}..."

        bluetoothGatt = device.connectGatt(this, false, gattCallback)
        Log.i(TAG, "connectGatt() returned: $bluetoothGatt")
    }

    /** Step 2: GATT callbacks */
    private val gattCallback = object : BluetoothGattCallback() {

        init {
            Log.i(TAG, "GattCallback initialized")
        }

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            Log.i(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server, discovering services...")
                    gatt?.discoverServices()
                    runOnUiThread { statusText.text = "Connected to ${gatt?.device?.name}" }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from device")
                    runOnUiThread { statusText.text = "Disconnected" }
                }
                else -> {
                    Log.i(TAG, "Connection state changed: $newState")
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            Log.i(TAG, "onServicesDiscovered: status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Service discovery failed with status: $status")
                return
            }

            val service = gatt?.getService(UART_SERVICE_UUID)
            if (service == null) {
                Log.w(TAG, "UART service not found!")
                runOnUiThread { statusText.text = "UART service NOT found" }
                return
            }
            Log.i(TAG, "UART service found")

            val txChar = service.getCharacteristic(TX_CHAR_UUID)
            if (txChar != null) {
                val success = gatt.setCharacteristicNotification(txChar, true)
                Log.i(TAG, "TX characteristic found, notifications enabled: $success")
            } else {
                Log.w(TAG, "TX characteristic not found")
            }

            runOnUiThread { statusText.text = "UART service discovered" }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic?.uuid == TX_CHAR_UUID) {
                val msg = characteristic.getStringValue(0)
                Log.i(TAG, "Received message: $msg")
                runOnUiThread { statusText.text = "Recv: $msg" }
            }
        }
    }

    /** Step 3: Send messages */
    private fun sendMessage(msg: String) {
        val service = bluetoothGatt?.getService(UART_SERVICE_UUID)
        val rxChar = service?.getCharacteristic(RX_CHAR_UUID)
        if (rxChar != null) {
            rxChar.value = msg.toByteArray()
            val result = bluetoothGatt?.writeCharacteristic(rxChar) ?: false
            Log.i(TAG, "Write attempted: $result, message: $msg")
        } else {
            Log.w(TAG, "RX characteristic not found, cannot send message")
        }
    }
}
