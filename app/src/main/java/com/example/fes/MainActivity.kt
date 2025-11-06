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
import android.content.Context
import androidx.activity.result.launch
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch


// Create a DataStore instance at the top level
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : AppCompatActivity(), EditConfigDialogFragment.EditConfigDialogListener {

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
    private lateinit var editConfigButton: Button

    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        modeSwitch = findViewById(R.id.modeSwitch)

        //text Values
        ampValue = findViewById(R.id.ampValue)
        freqValue = findViewById(R.id.freqValue)
        powValue = findViewById(R.id.powValue)
        onSetValue = findViewById(R.id.onSetValue)
        offSetValue = findViewById(R.id.offSetValue)

        editConfigButton = findViewById(R.id.editConfigButton)

        Log.i(TAG, "=== App started ===")
        ensureBluetoothPermissions()

        // Set click listener for the edit button
        editConfigButton.setOnClickListener {
            showEditDialog()
        }

        modeSwitch.setOnClickListener() {
            val isChecked = modeSwitch.isChecked
            Log.i(TAG, "Sending: $isChecked")
            sendMessage(isChecked.toString())
        }

        lifecycleScope.launch {
            val amp = readValue(this@MainActivity, "amplitude", "0")
            val freq = readValue(this@MainActivity, "frequency", "30")
            val pow = readValue(this@MainActivity, "power", "250")
            val on = readValue(this@MainActivity, "on", "0")
            val off = readValue(this@MainActivity, "off", "0")

            ampValue.text = "$amp mA"
            freqValue.text = "$freq Hz"
            powValue.text = "$pow μs"
            onSetValue.text = "$on"
            offSetValue.text = "$off"
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
            val currentAmp = ampValue.text.toString().replace("mA", "").trim().toIntOrNull() ?: 0
            val currentFreq = freqValue.text.toString().replace("Hz", "").trim().toIntOrNull() ?: 0
            val currentPower = powValue.text.toString().replace("μs", "").trim().toIntOrNull() ?: 0
            val currentOn = onSetValue.text.toString().trim().toIntOrNull() ?: 0
            val currentOff = offSetValue.text.toString().trim().toIntOrNull() ?: 0



            putInt("amplitude", currentAmp)
            putInt("frequency", currentFreq)
            putInt("power", currentPower)
            putInt("on", currentOn)
            putInt("off", currentOff)
        }
        dialogFragment.arguments = args

        // Show the dialog
        dialogFragment.show(supportFragmentManager, "EditConfigDialogFragment")
    }

    // This method is called when the dialog's "Save" button is clicked
    override fun onFinishEditDialog(amplitude: Int, frequency: Int, power: Int, on: Int, off: Int) {
        // Update the TextViews in MainActivity with the new values
        val f = frequency + 30
        val p = power * 5 + 250
        val on2 = on - 5
        val off2 = off - 5
        ampValue.text = "$amplitude mA"
        freqValue.text = "$f Hz"
        powValue.text = "$p μs"
        onSetValue.text = "$on2"
        offSetValue.text = "$off2"

        // Launch a coroutine to save the values
        lifecycleScope.launch {
            saveValue(this@MainActivity, "amplitude", amplitude.toString())
            saveValue(this@MainActivity, "frequency", f.toString())
            saveValue(this@MainActivity, "power", p.toString())
            saveValue(this@MainActivity, "on", on2.toString())
            saveValue(this@MainActivity, "off", off2.toString())
        }

        val json =
            "{${amplitude},${f},${p},${on2},${off2}}"
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
