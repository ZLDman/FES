package com.example.sco

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
import android.content.Context
import android.widget.LinearLayout
import android.graphics.Color
import android.widget.ImageView
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


// Create a DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity(), EditConfigDialogFragment.EditConfigDialogListener {

    private val TAG = "BLETestVerbose"
    private val messageBuffer = StringBuilder()

    // Bluefruit UART UUIDs
    private val UART_SERVICE_UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
    private val TX_CHAR_UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E") // Notify
    private val RX_CHAR_UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E") // Write
    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var statusText: TextView
    private lateinit var modeSwitch: Switch

    private lateinit var stimStatusLayout: LinearLayout
    private lateinit var stimStatusText: TextView

    private lateinit var rotationImage: ImageView


    //Values
    //Step Initiaion Thresholds
    private lateinit var stepTiltThresholdValue: TextView
    private lateinit var stepTiltRateThresholdValue: TextView

    //Knee Lock Thresholds
    private lateinit var lockTiltThresholdValue: TextView
    private lateinit var lockKneeAngleThresholdValue: TextView
    private lateinit var lockKneeAngleRateThresholdValue: TextView
    private lateinit var lockTimeValue: TextView
    private lateinit var editConfigButton: Button

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)

        //text Values
        stepTiltThresholdValue = findViewById(R.id.dialogStepTiltThresholdValue)
        stepTiltRateThresholdValue = findViewById(R.id.dialogStepTiltRateThresholdValue)
        lockTiltThresholdValue = findViewById(R.id.dialogLockTiltThresholdValue)
        lockKneeAngleThresholdValue = findViewById(R.id.dialogLockKneeAngleThresholdValue)
        lockKneeAngleRateThresholdValue = findViewById(R.id.dialogLockKneeAngleRateThresholdValue)
        lockTimeValue = findViewById(R.id.dialogLockTimeValue)


        stimStatusLayout = findViewById(R.id.stimStatusLayout)
        stimStatusText = findViewById(R.id.StimStatusText)
        stimStatusText.text = "STIM OFF"
        stimStatusLayout.setBackgroundColor(Color.parseColor("#FF0000")) // Green

        rotationImage = findViewById(R.id.rotationImage)


        editConfigButton = findViewById(R.id.editConfigButton)

        Log.i(TAG, "=== App started ===")
        ensureBluetoothPermissions()

        // Set click listener for the edit button
        editConfigButton.setOnClickListener {
            showEditDialog()
        }



        lifecycleScope.launch {
            val stepTilt = readValue(this@MainActivity, "stepTiltThreshold", "0")
            val stepTiltRate = readValue(this@MainActivity, "stepTiltRateThreshold", "0")
            val lockTilt = readValue(this@MainActivity, "lockTiltThreshold", "0")
            val lockKnee = readValue(this@MainActivity, "lockKneeAngleThreshold", "0")
            val lockKneeRate = readValue(this@MainActivity, "lockKneeAngleRateThreshold", "0")
            val lockTime = readValue(this@MainActivity, "lockTime", "0")


            stepTiltThresholdValue.text = "$stepTilt deg"
            stepTiltRateThresholdValue.text = "$stepTiltRate deg/sec"
            lockTiltThresholdValue.text = "$lockTilt deg"
            lockKneeAngleThresholdValue.text = "$lockKnee deg"
            lockKneeAngleRateThresholdValue.text = "$lockKneeRate deg/sec"
            lockTimeValue.text = "$lockTime ms"
        }
    }

    private suspend fun saveValue(context: Context, key: String, value: String) {
        context.dataStore.edit { settings ->
            // Define the key
            val dataStoreKey = stringPreferencesKey(key)
            // Put the value
            settings[dataStoreKey] = value
        }
    }

    private suspend fun readValue(context: Context, key: String, defaultValue: String): String? {
        val dataStoreKey = stringPreferencesKey(key)
        val preferences = context.dataStore.data.first()
        return preferences[dataStoreKey] ?: defaultValue
    }

    private fun showEditDialog() {
        val dialogFragment = EditConfigDialogFragment()

        // Pass current values to the dialog using a Bundle
        val args = Bundle().apply {
            // Safely parse the current text to an Int
            val stepTilt = stepTiltThresholdValue.text.toString().replace("deg", "").trim().toIntOrNull() ?: 0
            val stepTiltRate = stepTiltRateThresholdValue.text.toString().replace("deg/sec", "").trim().toIntOrNull() ?: 0
            val lockTilt = lockTiltThresholdValue.text.toString().replace("deg", "").trim().toIntOrNull() ?: 0
            val lockKnee = lockKneeAngleThresholdValue.text.toString().replace("deg", "").trim().toIntOrNull() ?: 0
            val lockKneeRate = lockKneeAngleRateThresholdValue.text.toString().replace("deg/sec", "").trim().toIntOrNull() ?: 0
            val lockTime = lockTimeValue.text.toString().replace("ms", "").trim().toIntOrNull() ?: 0

            putInt("stepTiltThreshold", stepTilt)
            putInt("stepTiltRateThreshold", stepTiltRate)
            putInt("lockTiltThreshold", lockTilt)
            putInt("lockKneeAngleThreshold", lockKnee)
            putInt("lockKneeAngleRateThreshold", lockKneeRate)
            putInt("lockTime", lockTime)
        }
        dialogFragment.arguments = args

        // Show the dialog
        dialogFragment.show(supportFragmentManager, "EditConfigDialogFragment")
    }

    // This method is called when the dialog's "Save" button is clicked
    override fun onFinishEditDialog(stepTiltThreshold: Int, stepTiltRateThreshold: Int, lockTiltThreshold: Int, lockKneeAngleThreshold: Int, lockKneeAngleRateThreshold: Int, lockTime: Int) {
        // Update the TextViews in MainActivity with the new values
        val stepTiltThreshold = stepTiltThreshold
        val stepTiltRateThreshold = stepTiltRateThreshold
        val lockTiltThreshold = lockTiltThreshold
        val lockKneeAngleThreshold = lockKneeAngleThreshold
        val lockKneeAngleRateThreshold = lockKneeAngleRateThreshold
        val lockTime = lockTime

        stepTiltThresholdValue.text = "$stepTiltThreshold mA"
        stepTiltRateThresholdValue.text = "$stepTiltRateThreshold Hz"
        lockTiltThresholdValue.text = "$lockTiltThreshold μs"
        lockKneeAngleThresholdValue.text = "$lockKneeAngleThreshold"
        lockKneeAngleRateThresholdValue.text = "$lockKneeAngleRateThreshold"
        lockTimeValue.text = "$lockTime"

        // Launch a coroutine to save the values
        lifecycleScope.launch {
            saveValue(this@MainActivity, "stepTiltThreshold", stepTiltThreshold.toString())
            saveValue(this@MainActivity, "stepTiltRateThreshold", stepTiltRateThreshold.toString())
            saveValue(this@MainActivity, "lockTiltThreshold", lockTiltThreshold.toString())
            saveValue(this@MainActivity, "lockKneeAngleThreshold", lockKneeAngleThreshold.toString())
            saveValue(this@MainActivity, "lockKneeAngleRateThreshold", lockKneeAngleRateThreshold.toString())
            saveValue(this@MainActivity, "lockTime", lockTime.toString())
        }

        val json =
            "{${stepTiltThreshold},${stepTiltRateThreshold},${lockTiltThreshold},${lockKneeAngleThreshold},${lockKneeAngleRateThreshold},${lockTime}}"
        Log.i(TAG, "Sending: $json")
        sendMessage(json)
        // You would also update the SeekBars' progress here if needed
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
                val incomingData = characteristic.getStringValue(0)
                Log.d(TAG, "Chunk received: '$incomingData'")

                // Check for the newline delimiter
                if (incomingData.contains("\n")) {
                    // Append the last part of the message (before the newline)
                    messageBuffer.append(incomingData.substringBefore("\n"))

                    val completeMessage = messageBuffer.toString().trim()
                    Log.i(TAG, "Complete message assembled: '$completeMessage'")

                    // Now that we have the full message, process it on the UI thread
                    runOnUiThread {
                        processCompleteMessage(completeMessage)
                    }

                    // Clear the buffer for the next message
                    messageBuffer.clear()

                    // If there's data after the newline, start the next message with it
                    val remainder = incomingData.substringAfter("\n", "")
                    if (remainder.isNotEmpty()) {
                        messageBuffer.append(remainder)
                    }

                } else {
                    // If no delimiter, just append the chunk to the buffer
                    messageBuffer.append(incomingData)
                }
            }
        }
    }

    private fun processCompleteMessage(message: String) {
        when {
            // Case 1: Handle STIM ON message
            message == "T" -> {
                Log.i(TAG, "Stimulation is ON")
                stimStatusLayout.setBackgroundColor(Color.parseColor("#00FF00")) // Green
                stimStatusText.text = "STIM ON"
            }

            // Case 2: Handle STIM OFF message
            message == "F" -> {
                Log.i(TAG, "Stimulation is OFF")
                stimStatusLayout.setBackgroundColor(Color.parseColor("#FF0000")) // Red
                stimStatusText.text = "STIM OFF"
            }

            // Case 3: Handle ANGLE message (e.g., "A90", "A180.5")
            message.startsWith("A") -> {
                val angleString = message.substring(1)
                val angle = angleString.toFloatOrNull() ?: 0.0f
                rotationImage.rotation = angle // Set rotation directly
                Log.d(TAG, "Angle set to: $angle")
            }

            // Optional: Handle other messages or log unknown messages
            else -> {
                Log.w(TAG, "Received unknown message: '$message'")
                statusText.text = "Recv: $message"
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
