package com.example.demoapp.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.demoapp.databinding.ActivityBluetoothConnectionBinding
import com.example.demoapp.view.MenuActivity
import java.util.UUID

/**
 * Classe responsável por gerenciar a conexão Bluetooth Low Energy (BLE).
 * Configura a interface e os componentes necessários para escanear, conectar e se comunicar com dispositivos BLE.
 */
class BluetoothConnectionActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityBluetoothConnectionBinding.inflate(layoutInflater)
    }

    /**
     * Gerenciador Bluetooth, utilizado para obter o adaptador Bluetooth e outras funções relacionadas.
     */
    private lateinit var bluetoothManager: BluetoothManager

    /**
     * Adaptador Bluetooth usado para interagir com dispositivos BLE.
     */
    private lateinit var bluetoothAdapter: BluetoothAdapter

    /**
     * Handler utilizado para postar tarefas na thread principal.
     */
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Período de escaneamento para dispositivos BLE (2 segundos).
     */
    private val SCAN_PERIOD: Long = 2000

    /**
     * Flag para indicar se o escaneamento BLE está ativo.
     */
    private var scanning = false

    /**
     * Adaptador para exibir a lista de dispositivos encontrados no escaneamento.
     * A ação de clique conecta ao dispositivo selecionado.
     */
    private val deviceListAdapter = LeDeviceListAdapter { device -> connectToDevice(device) }

    /**
     * Scanner BLE usado para realizar escaneamento de dispositivos.
     * É inicializado preguiçosamente com base no adaptador Bluetooth.
     */
    private val bluetoothLeScanner by lazy { bluetoothAdapter.bluetoothLeScanner }

    /**
     * Representa a conexão GATT com um dispositivo BLE.
     * Pode ser utilizada para ler, escrever e receber notificações de características.
     */
    private var bluetoothGatt: BluetoothGatt? = null

    /**
     * Característica BLE que pode ser escrita pelo aplicativo.
     */
    private var writableCharacteristic: BluetoothGattCharacteristic? = null

    /**
     * UUID do serviço BLE utilizado para comunicação com o dispositivo.
     */
    private val SERVICE_UUID: UUID = UUID.fromString("ab0828b1-198e-4351-b779-901fa0e0371e")

    /**
     * UUID para configuração da característica do cliente (notificações e indicações).
     */
    private val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    /**
     * UUID da característica para receber dados (RX).
     */
    private val CHARACTERISTIC_UUID_RX: UUID = UUID.fromString("4ac8a682-9736-4e5d-932b-e9b31405049c")

    /**
     * UUID da característica para enviar dados (TX).
     */
    private val CHARACTERISTIC_UUID_TX: UUID = UUID.fromString("84d4f420-e7f0-4b0c-b16a-a125b0521aed")

    /**
     * Método chamado quando a atividade é criada.
     * Configura a interface e inicializa o gerenciador e adaptador Bluetooth.
     *
     * @param savedInstanceState Instância salva do estado anterior da atividade, se existir.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configurando o layout principal usando View Binding
        setContentView(binding.root)

        // Obtendo o gerenciador Bluetooth e o adaptador associado
        bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Configurando o RecyclerView para exibir a lista de dispositivos
        setupRecyclerView()

        /**
         * Configurando o listener do Switch para iniciar ou parar o escaneamento BLE.
         */
        binding.bluetoothSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Iniciar o escaneamento
                if (hasPermissions()) {
                    scanLeDevice()
                } else {
                    // Solicitar permissões necessárias
                    requestPermissions()
                }
            } else {
                // Parar o escaneamento
                stopScanning()
            }
        }
    }
    /**
     * Configura o RecyclerView para exibir a lista de dispositivos encontrados.
     * Define o gerenciador de layout e o adaptador para o RecyclerView.
     */
    private fun setupRecyclerView() {
        binding.recyclerViewDevices.apply {
            // Configurando o gerenciador de layout como LinearLayout
            layoutManager = LinearLayoutManager(this@BluetoothConnectionActivity)
            // Associando o adaptador personalizado para exibir dispositivos BLE
            adapter = deviceListAdapter
        }
    }

    /**
     * Navega para a tela do menu principal.
     * Finaliza a atividade atual após a navegação.
     */
    private fun navigateToMenu() {
        // Criando um Intent para abrir a MenuActivity
        val intent = Intent(this, MenuActivity::class.java)
        // Iniciando a MenuActivity
        startActivity(intent)
        // Finalizando a atividade atual para liberar recursos
        finish()
    }

    /**
     * Inicia o escaneamento de dispositivos BLE.
     * Exibe um indicador de progresso (ProgressBar) e configura um temporizador
     * para interromper o escaneamento após um período definido (SCAN_PERIOD).
     */
    private fun scanLeDevice() {
        if (!scanning) {
            // Tornando visível o ProgressBar enquanto o escaneamento está ativo
            binding.progressBar.visibility = View.VISIBLE

            // Configurando um temporizador para parar o escaneamento após SCAN_PERIOD
            handler.postDelayed({
                stopScanning()
            }, SCAN_PERIOD)

            scanning = true // Atualizando o estado para indicar que o escaneamento está ativo

            // Verificando permissões antes de iniciar o escaneamento
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            // Iniciando o escaneamento de dispositivos BLE
            bluetoothLeScanner.startScan(leScanCallback)
            Toast.makeText(this, "Escaneamento iniciado", Toast.LENGTH_SHORT).show()
        } else {
            // Caso o escaneamento já esteja ativo, interrompe-lo
            stopScanning()
        }
    }

    /**
     * Para o escaneamento de dispositivos BLE.
     * Oculta o indicador de progresso (ProgressBar) e atualiza o estado.
     */
    private fun stopScanning() {
        // Tornando invisível o ProgressBar após o término do escaneamento
        binding.progressBar.visibility = View.GONE
        scanning = false // Atualizando o estado para indicar que o escaneamento está inativo

        // Verificando permissões antes de parar o escaneamento
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Parando o escaneamento de dispositivos BLE
        bluetoothLeScanner.stopScan(leScanCallback)
        Toast.makeText(this, "Escaneamento finalizado", Toast.LENGTH_SHORT).show()
    }

    /**
     * Callback para lidar com os resultados do escaneamento BLE.
     * Recebe os dispositivos encontrados e trata erros durante o processo de escaneamento.
     */
    private val leScanCallback = object : ScanCallback() {

        /**
         * Chamado quando um dispositivo é encontrado durante o escaneamento BLE.
         *
         * @param callbackType Tipo do callback, indicando o motivo pelo qual o método foi chamado.
         * @param result Resultado do escaneamento, contendo informações sobre o dispositivo encontrado.
         */
        @SuppressLint("NotifyDataSetChanged")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device

            // Verificando permissões antes de acessar informações do dispositivo
            if (ActivityCompat.checkSelfPermission(
                    this@BluetoothConnectionActivity,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            // Adicionando dispositivos com nome não nulo à lista
            if (device.name != null) {
                deviceListAdapter.addDevice(device) // Adicionando o dispositivo ao adaptador
                deviceListAdapter.notifyDataSetChanged() // Atualizando a lista visível
            }
        }

        /**
         * Chamado quando o escaneamento BLE falha.
         *
         * @param errorCode Código de erro que indica a causa da falha.
         */
        override fun onScanFailed(errorCode: Int) {
            // Registrando o erro no log para depuração
            Log.e("BLE Scan", "Erro no escaneamento: $errorCode")

            // Exibindo uma mensagem de erro ao usuário
            Toast.makeText(
                this@BluetoothConnectionActivity,
                "Erro no escaneamento BLE: $errorCode",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Verifica se todas as permissões necessárias para o funcionamento do Bluetooth Low Energy (BLE)
     * e acesso à localização estão concedidas.
     *
     * @return `true` se todas as permissões necessárias foram concedidas, `false` caso contrário.
     */
    private fun hasPermissions(): Boolean {
        // Lista de permissões necessárias, dependendo da versão do SDK
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,       // Permissão para escanear dispositivos BLE
                Manifest.permission.BLUETOOTH_CONNECT,   // Permissão para conectar a dispositivos BLE
                Manifest.permission.ACCESS_FINE_LOCATION // Permissão para acessar a localização precisa
            )
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION) // Apenas localização precisa para versões mais antigas
        }

        // Retorna true se todas as permissões necessárias foram concedidas
        return permissions.all {
            ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Solicita ao usuário as permissões necessárias para o funcionamento do BLE e acesso à localização.
     * As permissões solicitadas dependem da versão do SDK.
     */
    private fun requestPermissions() {
        // Lista de permissões necessárias, dependendo da versão do SDK
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,       // Permissão para escanear dispositivos BLE
                Manifest.permission.BLUETOOTH_CONNECT,   // Permissão para conectar a dispositivos BLE
                Manifest.permission.ACCESS_FINE_LOCATION // Permissão para acessar a localização precisa
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION) // Apenas localização precisa para versões mais antigas
        }

        // Solicita as permissões ao usuário
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    /**
     * Objeto de companheiro para armazenar constantes utilizadas na classe.
     */
    companion object {
        /**
         * Código de solicitação para identificar o pedido de permissões.
         */
        private const val PERMISSION_REQUEST_CODE = 1
    }

    /**
     * Conecta-se a um dispositivo Bluetooth Low Energy (BLE) e gerencia as interações com ele.
     * Configura os callbacks para lidar com mudanças de estado da conexão, descoberta de serviços e
     * recepção de dados via notificações BLE.
     *
     * @param device Dispositivo BLE ao qual se deseja conectar.
     */
    private fun connectToDevice(device: BluetoothDevice) {
        // Verifica se a permissão necessária para conectar está concedida
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Inicia a conexão GATT com o dispositivo selecionado
        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {

            /**
             * Callback chamado quando o estado da conexão com o dispositivo BLE muda.
             *
             * @param gatt Instância do GATT associado ao dispositivo.
             * @param status Status atual da conexão.
             * @param newState Novo estado da conexão (ex.: conectado ou desconectado).
             */
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d("BLE Connection", "Conectado ao dispositivo: ${device.address}")
                        runOnUiThread {
                            Toast.makeText(
                                this@BluetoothConnectionActivity,
                                "Conectado ao dispositivo ${device.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        // Descobrir serviços disponíveis no dispositivo conectado
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.d("BLE Connection", "Dispositivo desconectado: ${device.address}")
                        runOnUiThread {
                            Toast.makeText(
                                this@BluetoothConnectionActivity,
                                "Desconectado do dispositivo ${device.name}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        bluetoothGatt = null
                        if (ActivityCompat.checkSelfPermission(
                                this@BluetoothConnectionActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        gatt.close() // Fecha a conexão GATT
                    }
                }
            }

            /**
             * Callback chamado quando os serviços BLE são descobertos no dispositivo.
             *
             * @param gatt Instância do GATT associado ao dispositivo.
             * @param status Status da operação de descoberta de serviços.
             */
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE Services", "Serviços descobertos: ${gatt.services}")
                    val service = gatt.getService(SERVICE_UUID)

                    writableCharacteristic = service?.getCharacteristic(CHARACTERISTIC_UUID_RX)
                    val txCharacteristic = service?.getCharacteristic(CHARACTERISTIC_UUID_TX)

                    // Configura notificações para a característica TX
                    if (txCharacteristic != null) {
                        if (ActivityCompat.checkSelfPermission(
                                this@BluetoothConnectionActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        gatt.setCharacteristicNotification(txCharacteristic, true)
                        val descriptor = txCharacteristic.getDescriptor(
                            UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)
                        )
                        descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        if (ActivityCompat.checkSelfPermission(
                                this@BluetoothConnectionActivity,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return
                        }
                        gatt.writeDescriptor(descriptor)

                        runOnUiThread {
                            // Navega para o menu após a configuração bem-sucedida
                            navigateToMenu()
                        }
                        Log.d("BLE Notification", "Notificações habilitadas para a característica TX")
                    } else {
                        Log.e("BLE Characteristic", "Característica TX não encontrada.")
                    }
                } else {
                    Log.w("BLE Services", "Falha ao descobrir serviços: $status")
                }
            }

            /**
             * Callback chamado quando os dados de uma característica BLE são alterados.
             * Utilizado para receber notificações ou indicações de um dispositivo.
             *
             * @param gatt Instância do GATT associado ao dispositivo.
             * @param characteristic Característica que foi alterada.
             */
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                if (characteristic.uuid == CHARACTERISTIC_UUID_TX) {
                    val data = characteristic.value.decodeToString() // Decodifica os dados recebidos
                    Log.d("BLE Notification", "Dados recebidos: $data")

                    // Envia os dados recebidos como um broadcast para outras partes do aplicativo
                    val intent = Intent("com.example.app.BLE_DATA")
                    intent.putExtra("BLE_MESSAGE", data)
                    sendBroadcast(intent)
                }
            }
        })
    }
}