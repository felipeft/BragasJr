package com.example.demoapp.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.demoapp.R
import com.example.demoapp.bluetooth.BluetoothService
import com.example.demoapp.databinding.ActivityMenuBinding

class MenuActivity : AppCompatActivity() {
    // Configuração do binding para associar os componentes do layout com a lógica da Activity.
    private val binding by lazy {
        ActivityMenuBinding.inflate(layoutInflater)
    }

    // Lista de valores correspondentes à SeekBar
    private val seekBarValues = listOf(-30, -15, 0, +15, +30)

    /**
     * BroadcastReceiver para receber dados transmitidos via Bluetooth.
     * - Captura intents com a ação "com.example.app.BLE_DATA".
     * - Atualiza o TextView na interface com os dados recebidos.
     */
    private val bleDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Obtém a mensagem transmitida via Bluetooth
            val data = intent?.getStringExtra("BLE_MESSAGE")
            if (data != null) {
                // Atualiza o TextView com a mensagem recebida
                binding.receivedMessageTextView.text = data
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Registra o BroadcastReceiver para receber dados transmitidos via Bluetooth
        val filter = IntentFilter("com.example.app.BLE_DATA")
        registerReceiver(bleDataReceiver, filter)

        // Configuração inicial do Switch
        binding.switchMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) "ON" else "OFF"
            Toast.makeText(this, "Modo alterado para $mode", Toast.LENGTH_SHORT).show()

            // Envia o estado do Switch automaticamente quando alterado
            val message = "Modo alterado para: $mode"
            BluetoothService.sendMessage(message, this)
        }

        // Configuração da SeekBar
        binding.seekBar.max = seekBarValues.size - 1
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Atualiza o TextView com o valor atual correspondente ao progresso
                binding.textSeekBarValue.text = seekBarValues[progress].toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Não é necessário implementar nada aqui para este caso
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Não é necessário implementar nada aqui para este caso
            }
        })

        // Configuração do botão "Enviar"
        binding.buttonSend.setOnClickListener {
            val selectedValue = seekBarValues[binding.seekBar.progress].toString()

            // Inflate o layout personalizado
            val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)

            // Configure o AlertDialog
            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Configure as ações dos botões
            val messageTextView = dialogView.findViewById<TextView>(R.id.dialogMessage)
            val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
            val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)

            // Defina a mensagem
            messageTextView.text = "Deseja enviar o valor: $selectedValue?"

            // Ação para o botão "Não"
            buttonCancel.setOnClickListener {
                alertDialog.dismiss()
            }

            // Ação para o botão "Sim"
            buttonConfirm.setOnClickListener {
                val message = "Valor: $selectedValue"
                BluetoothService.sendMessage(message, this)
                Toast.makeText(this, "Mensagem enviada: $message", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }

            // Mostre o diálogo
            alertDialog.show()
        }

        // Configuração do botão "Limpar"
        binding.buttonClear.setOnClickListener {
            // Inflate o layout personalizado
            val dialogView = layoutInflater.inflate(R.layout.dialog_confirmation, null)

            // Configure o AlertDialog
            val alertDialog = AlertDialog.Builder(this)
                .setView(dialogView)
                .create()

            // Configure as ações dos botões
            val messageTextView = dialogView.findViewById<TextView>(R.id.dialogMessage)
            val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
            val buttonConfirm = dialogView.findViewById<Button>(R.id.buttonConfirm)

            // Defina a mensagem
            messageTextView.text = "Deseja resetar os valores?"

            // Ação para o botão "Não"
            buttonCancel.setOnClickListener {
                alertDialog.dismiss()
            }

            // Ação para o botão "Sim"
            buttonConfirm.setOnClickListener {
                binding.seekBar.progress = seekBarValues.indexOf(0) // Reseta o SeekBar para o valor 0
                binding.textSeekBarValue.text = "0" // Atualiza o TextView para exibir 0
                Toast.makeText(this, "Valores resetados", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
            // Mostre o diálogo
            alertDialog.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistra o BroadcastReceiver ao destruir a Activity
        // Isso previne vazamentos de memória ao garantir que o receiver não fique registrado.
        unregisterReceiver(bleDataReceiver)
    }
}