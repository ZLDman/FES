package com.example.fes

import android.Manifest
import android.bluetooth.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val TAG = "BLETestVerbose"

    // Bluefruit UART UUIDs
    private val UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Notify
    private val RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Write
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var statusText: TextView
    private lateinit var sendButton: Button
    private lateinit var modeSwitch: Switch

    //slider bars
    private lateinit var ampBar: SeekBar
    private lateinit var freqBar: SeekBar
    private lateinit var powBar: SeekBar
    private lateinit var onSetBar: SeekBar
    private lateinit var offSetBar: SeekBar

    //Values
    private lateinit var ampValue: TextView
    private lateinit var freqValue: TextView
    private lateinit var powValue: TextView
    private lateinit var onSetValue: TextView
    private lateinit var offSetValue: TextView

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        modeSwitch = findViewById(R.id.modeSwitch)

        //sliders
        ampBar = findViewById(R.id.ampBar)
        freqBar = findViewById(R.id.freqBar)
        powBar = findViewById(R.id.powBar)
        onSetBar = findViewById(R.id.onSetBar)
        offSetBar = findViewById(R.id.offSetBar)

        //text Values
        ampValue = findViewById(R.id.ampValue)
        freqValue = findViewById(R.id.freqValue)
        powValue = findViewById(R.id.powValue)
        onSetValue = findViewById(R.id.onSetValue)
        offSetValue = findViewById(R.id.offSetValue)

        sendButton = findViewById(R.id.sendButton)

        Log.i(TAG, "=== App started ===")
        ensureBluetoothPermissions()

        // listeners
        ampBar.setOnSeekBarChangeListener(makeSeekListener { progress ->
            ampValue.text = "$progress mA"
        })

        freqBar.setOnSeekBarChangeListener(makeSeekListener { progress ->
            val f = progress + 30
            freqValue.text = "$f Hz"
        })

        powBar.setOnSeekBarChangeListener(makeSeekListener { progress ->
            val p = (progress * 5) + 250
            powValue.text = "$p μs"
        })

        onSetBar.setOnSeekBarChangeListener(makeSeekListener { progress ->
            onSetValue.text = "$progress"
        })

        offSetBar.setOnSeekBarChangeListener(makeSeekListener { progress ->
            offSetValue.text = "$progress"
        })

        sendButton.setOnClickListener {
            val f = freqBar.progress + 30
            val p = (powBar.progress * 5) + 250
            val json =
                "{${modeSwitch.isChecked},${ampBar.progress},${f},${p},${onSetBar.progress},${offSetBar.progress}}"
            Log.i(TAG, "Send button clicked: $json")
            sendMessage(json)
        }
    }

    private fun makeSeekListener(onChange: (Int) -> Unit) =
        object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                onChange(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

    /** Step 1: Check runtime permissions (Android 12+) */
    private fun ensureBluetoothPermissions() {
        val neededPerms = mutableListOf<String>()

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            neededPerms.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            neededPerms.add(Manifest.permission.BLUETOOTH_SCAN)
        }

        if (neededPerms.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, neededPerms.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            startBleConnection()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            startBleConnection()
        } else {
            statusText.text = "Bluetooth permissions denied"
        }
    }

    /** Step 2: Start BLE connection */
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

        val device = adapter.bondedDevices.firstOrNull { it.name == "FES" }
        if (device == null) {
            Log.w(TAG, "FES not paired")
            statusText.text = "FES not paired"
            return
        }

        Log.i(TAG, "Found paired device: ${device.name} (${device.address})")
        statusText.text = "Connecting to ${device.name}..."

        bluetoothGatt = if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            Log.e(TAG, "Missing BLUETOOTH_CONNECT permission!")
            null
        }
        Log.i(TAG, "connectGatt() returned: $bluetoothGatt")
    }

    /** Step 3: GATT callbacks */
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
                gatt.setCharacteristicNotification(txChar, true)
                val descriptor = txChar.getDescriptor(CCCD_UUID)
                descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (descriptor != null) {
                    gatt.writeDescriptor(descriptor)
                    Log.i(TAG, "TX notifications enabled with CCCD write")
                }
            } else {
                Log.w(TAG, "TX characteristic not found")
            }

            runOnUiThread { statusText.text = "UART service discovered" }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            if (characteristic?.uuid == TX_CHAR_UUID) {
                val msg = characteristic.getStringValue(0)
                Log.i(TAG, "Received message: $msg")
                runOnUiThread { statusText.text = "Recv: $msg" }
            }
        }
    }

    /** Step 4: Send messages */
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
