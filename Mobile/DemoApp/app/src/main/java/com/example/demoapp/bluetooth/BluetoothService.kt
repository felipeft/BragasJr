package com.example.demoapp.bluetooth

import android.Manifest
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat

/**
 * Objeto responsável por gerenciar a comunicação com dispositivos BLE.
 * Permite o envio de mensagens para dispositivos conectados utilizando características BLE.
 */

object BluetoothService {

    /**
     * Armazenando a instância atual da conexão GATT com o dispositivo BLE.
     * Utilizada para realizar operações de leitura, escrita e notificações.
     */
    var bluetoothGatt: BluetoothGatt? = null

    /**
     * Armazenando a característica BLE que suporta operações de escrita.
     * Necessária para enviar mensagens ao dispositivo BLE conectado.
     */
    var writableCharacteristic: BluetoothGattCharacteristic? = null

    /**
     * Enviando uma mensagem para o dispositivo BLE conectado.
     * Utilizando a característica de escrita configurada para transmitir os dados.
     *
     * @param message Especificando a mensagem a ser enviada para o dispositivo BLE.
     * @param context Fornecendo o contexto atual, necessário para exibir mensagens ao usuário.
     */
    fun sendMessage(message: String, context: Context) {
        // Verificando se a característica de escrita está configurada
        writableCharacteristic?.let { characteristic ->
            // Definindo o valor da característica com os bytes da mensagem
            characteristic.value = message.toByteArray()

            // Verificando se a permissão necessária foi concedida
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Exibindo uma mensagem ao usuário caso a permissão não tenha sido concedida
                Toast.makeText(context, "Permissão Bluetooth não concedida.", Toast.LENGTH_SHORT).show()
                return
            }

            // Escrevendo a característica no dispositivo e verificando o sucesso
            bluetoothGatt?.writeCharacteristic(characteristic)?.let { success ->
                if (success) {
                    // Exibindo uma mensagem de sucesso ao enviar
                    Toast.makeText(context, "Mensagem enviada: $message", Toast.LENGTH_SHORT).show()
                } else {
                    // Exibindo uma mensagem de erro caso a escrita falhe
                    Toast.makeText(context, "Falha ao enviar mensagem.", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            // Exibindo uma mensagem caso a característica de escrita não esteja configurada
            Toast.makeText(context, "Característica de escrita não encontrada.", Toast.LENGTH_SHORT).show()
        }
    }
}
