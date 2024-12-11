package com.example.demoapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.demoapp.bluetooth.BluetoothConnectionActivity
import com.example.demoapp.databinding.ActivityMainBinding

/**
 * Classe principal do aplicativo, responsável pela tela inicial e interação do usuário.
 * Esta classe gerencia a inicialização da interface e o redirecionamento para a próxima atividade.
 */
class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    /**
     * Método chamado quando a atividade é criada.
     * Responsável por configurar o layout da atividade e inicializar os componentes necessários.
     *
     * @param savedInstanceState Instância salva do estado anterior da atividade, se existir.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configurando o layout principal usando View Binding
        setContentView(binding.root)

        /**
         * Configurando o listener do botão "Início".
         * Este listener inicia a tela de conexão Bluetooth quando o botão é pressionado.
         */
        binding.buttonStart.setOnClickListener {
            // Criando um Intent para redirecionar para BluetoothConnectionActivity
            val intent = Intent(this, BluetoothConnectionActivity::class.java)
            // Iniciando a atividade de conexão Bluetooth
            startActivity(intent)
        }
    }
}
