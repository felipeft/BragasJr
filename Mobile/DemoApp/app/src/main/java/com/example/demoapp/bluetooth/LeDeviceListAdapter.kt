package com.example.demoapp.bluetooth

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView

/**
 * Adaptador personalizado gerenciando a exibição de dispositivos BLE encontrados.
 * Configurando a interface do RecyclerView para listar dispositivos Bluetooth disponíveis.
 *
 * @param onClick Callback executado quando um dispositivo é selecionado.
 */
class LeDeviceListAdapter(
    private val onClick: (BluetoothDevice) -> Unit
) : RecyclerView.Adapter<LeDeviceListAdapter.DeviceViewHolder>() {

    /**
     * Lista armazenando os dispositivos BLE encontrados durante o escaneamento.
     */
    private val devices = mutableListOf<BluetoothDevice>()

    /**
     * Adicionando um dispositivo à lista, caso ele ainda não tenha sido adicionado.
     *
     * @param device Dispositivo BLE a ser adicionado à lista.
     */
    fun addDevice(device: BluetoothDevice) {
        if (!devices.contains(device)) {
            devices.add(device)
        }
    }

    /**
     * Criando um novo ViewHolder para representar um item na lista.
     *
     * @param parent ViewGroup que conterá a nova View.
     * @param viewType Tipo da View (não utilizado neste adaptador).
     * @return Uma nova instância de DeviceViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_2, parent, false)
        return DeviceViewHolder(view, onClick)
    }

    /**
     * Vinculando os dados do dispositivo a um ViewHolder.
     *
     * @param holder Instância de DeviceViewHolder para ser preenchida com os dados.
     * @param position Posição do item na lista.
     */
    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    /**
     * Obtendo o número total de dispositivos na lista.
     *
     * @return Quantidade de dispositivos armazenados.
     */
    override fun getItemCount(): Int = devices.size

    /**
     * ViewHolder representando um item da lista de dispositivos BLE.
     * Configurando a exibição do nome e endereço do dispositivo e gerenciando cliques no item.
     *
     * @param view View representando o layout do item.
     * @param onClick Callback executado ao clicar no item.
     */
    inner class DeviceViewHolder(
        view: View,
        private val onClick: (BluetoothDevice) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        /**
         * TextView exibindo o nome do dispositivo.
         */
        private val nameTextView: TextView = itemView.findViewById(android.R.id.text1)

        /**
         * TextView exibindo o endereço do dispositivo.
         */
        private val addressTextView: TextView = itemView.findViewById(android.R.id.text2)

        /**
         * Vinculando os dados de um dispositivo BLE à interface do item da lista.
         * Configurando as informações do dispositivo e o comportamento ao clicar no item.
         *
         * @param device Dispositivo BLE a ser exibido no item.
         */
        fun bind(device: BluetoothDevice) {
            val context = itemView.context

            // Verificando se a permissão para acessar dispositivos BLE foi concedida
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("BLE Adapter", "Permissão BLUETOOTH_CONNECT ausente.")
                return
            }

            // Configurando o nome do dispositivo ou exibindo "Desconhecido" caso não tenha nome
            nameTextView.text = device.name ?: "Desconhecido"

            // Configurando o endereço do dispositivo
            addressTextView.text = device.address

            // Configurando o comportamento ao clicar no item da lista
            itemView.setOnClickListener { onClick(device) }
        }
    }
}
