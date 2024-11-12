package com.example.appl300.mainActivity

import android.content.Context
import androidx.lifecycle.ViewModel
import br.com.positivo.api.mifare.CardReaderInfo
import br.com.positivo.api.mifare.Mifare
import br.com.positivo.api.mifare.MifareCardCallback
import br.com.positivo.api.mifare.MifareTypes
import br.com.positivo.lib.provider.PositivoDeviceProvider


class MainActivityViewModel : ViewModel() {

    private val DEFAULT_KEY_NFC = byteArrayOf()
    private val JONH_KEY_NFC = byteArrayOf()
    private val PERMISSION_KEY_NFC: ByteArray = byteArrayOf()

    fun format(context: Context) {
        val positivoDeviceProvider : PositivoDeviceProvider = PositivoDeviceProvider()
        val mMifare : Mifare = positivoDeviceProvider.getMifare(context)
        formatNfcTag(mMifare, context)
    }


    fun formatNfcTag(mMifare: Mifare, context: Context): Int {
        // 1. Ativar o cartão
        val activateResult = mMifare.activateCard()
        if (activateResult != 0) {
            println("Erro ao ativar a leitora NFC")
            return activateResult
        }

        // 2. Detectar o cartão
        mMifare.detectCards(object : MifareCardCallback {
            override fun onError(errorCode: Int) {
                println("Erro na detecção do cartão, código de erro: $errorCode")
            }

            override fun onSuccess(cardReaderInfo: CardReaderInfo?) {
                if (cardReaderInfo != null) {
                    println("Cartão detectado com sucesso: ${cardReaderInfo.cardType}")
                }
                for (sector in 0..15) {
                    // 3. Autenticar o setor onde as chaves serão trocadas
                    val authResult = authenticateSector(mMifare, JONH_KEY_NFC, sector.toByte())
                    if (authResult == 0) {
                        println("Autenticação do setor $sector bem-sucedida.")
                        for (block in 0..2) {
                            // 4. Substituir as chaves
                            val concatenatedBytes = DEFAULT_KEY_NFC + PERMISSION_KEY_NFC + DEFAULT_KEY_NFC
                            val writeResult = writeBlock(mMifare, sector.toByte(), block.toByte(), concatenatedBytes)
                            if (writeResult == 0) {
                                println("Chave DEFAULT_KEY_NFC escrita com sucesso no setor $sector, bloco $block")
                            } else {
                                println("Erro ao escrever DEFAULT_KEY_NFC no setor $sector")
                            }
                        }
                    } else {
                        println("Erro ao autenticar o setor $sector com a chave JONH_KEY_NFC")
                    }
                }
            }
        })

        return 0 // Sucesso
    }

    private fun writeBlock(mMifare: Mifare, sector: Byte, block: Byte, data: ByteArray): Int {
        val isWriteSuccess = mMifare.writeBlock(sector, block, data)
        return isWriteSuccess
    }

    fun authenticateSector(mMifare: Mifare, keyData: ByteArray, sectorNum: Byte): Int {
        val isAuthenticated = mMifare.authenticateSector(MifareTypes.TypeA, keyData, sectorNum)
        return isAuthenticated
    }


}