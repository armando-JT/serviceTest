package com.mycompany.servicetest

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.os.IBinder.DeathRecipient
import android.os.RemoteException
import android.util.Log
import androidx.annotation.NonNull
import androidx.annotation.RequiresApi
import com.kinpos.kinposconnect.posservice.BuildConfig
import com.kinpos.kinposconnect.posservice.KEMVPROC
import com.kinpos.posservice.IScrCallbackListener
import com.kinpos.posservice.IService_KEMV
import com.kinpos.posservice.TxnDataAjustAmount
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import java.nio.charset.StandardCharsets

class MainActivity: FlutterActivity() {
    private lateinit var channel : MethodChannel

    fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((s[i].digitToIntOrNull(16) ?: -1 shl 4)
            + s[i + 1].digitToIntOrNull(16)!! ?: -1).toByte()
            i += 2
        }
        return data
    }

    val TAG = "KINPOS"
    var isBound = false
    var mService: IService_KEMV? = null
    val emvVersion = "V656";
    var client = "VISANET".toByteArray(StandardCharsets.UTF_8);
    var vector = hexStringToByteArray("15CCF770285E140FDE2DD65687E34467AC0C8D7EAD50FA5AF82CC1099E58A30F01FC826AF77D530490BB84733EF78B11261D051FB4F0411F8A126392C6400ECE");

    lateinit var myCbListener: CallbackListener

    private val mDeathRecipient: DeathRecipient = object : DeathRecipient {
        override fun binderDied() {
            if (mService != null) {
                //Unexpected death on Server side
                try {
                    mService!!.unReg_SCR_Callback(myCbListener)
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
                mService!!.asBinder().unlinkToDeath(this, 0)
                mService = null

                KEMVPROC.bindService(connection, MainActivity(), vector, client, emvVersion)
            }
        }
    }

    class CallbackListener : IScrCallbackListener.Stub() {
        @Throws(RemoteException::class)
        override fun scr_callback(codMsg: Int, msg: String) {
            Log.d("KINPOS", "scr_callback: cod[$codMsg] $msg")
            val final_msg = msg

            try {
//            @ActivityContext.runOnUiThread(Runnable {
//                if (progressDoalog != null) {
//                    progressDoalog.setMessage(final_msg)
//                }
//               if (progressDoalog != null) {
//                   progressDoalog.show()
//               }
//            })

                // Colocar delay de 2 segundos a los siguientes mensajes
                // - Scr_SeePhone      codigo = 4,  mensaje en pantalla "VEA EL TELEFONO"
                // - VerifyPINOK       codigo = 7,  mensaje en pantalla "PIN OK Espere..."
                // - VerifyPINNOTOK    codigo = 10, mensaje en pantalla "ERROR EN P.I.N.\nEspere..."
                // - VerifyPINLAST     codigo = 11, mensaje en pantalla "ULTIMO INTENTO"
                // - Scr_PinNotEntered codigo = 13, mensaje en pantalla "Pin no Ingreado"
                if (codMsg == 4 || codMsg == 7 || codMsg == 10 || codMsg == 11 || codMsg == 13) Thread.sleep(
                    2000
                )
            } catch (e: Exception) {
                Log.e("KINPOS", "scr_callback ", e)
            }
        }

        @Throws(RemoteException::class)
        override fun AjustAmount_callback(txnData: TxnDataAjustAmount): TxnDataAjustAmount {
            //Log.d(TAG, "AjustAmount_callback: bin[" + Utility.byteArrayToHexString(txnData.cardbin) + "] " + " cardType >" + txnData.cardtype + "<  ( 0=Banda, 1=Clss, 2=Contacto, 4=Manual )");
            val txnajust = TxnDataAjustAmount()

            return txnajust
        }
    }

    val connection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            //if (BuildConfig.DEBUG) Log.i("KEMV", ">> onServiceConnected")
            mService = KEMVPROC.getServiceKEMV(service) // IService_KEMV.Stub.asInterface(service);

            if (mService != null) {
                try {
                    /* Activar debug en Clss y EMV  */
                    if (BuildConfig.DEBUG) mService?.SetAllow_Debug(true, true)

                    val version = mService?.FWL_getVersion()
                    Log.i(TAG, "Version KEMV : $version")

                    myCbListener = CallbackListener()
                    service.linkToDeath(mDeathRecipient, 0)

                    mService!!.Reg_SCR_Callback(myCbListener)
                    Log.i("KEMV", ">> POSService Connected")

                    isBound = true
                } catch (e: RemoteException) {
                    Log.e("onServiceConnected", "RemoteException", e)
                    //e.printStackTrace()
                }
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            if (BuildConfig.DEBUG) println(">> onServiceDisconnected")
            isBound = false
            //mService = null
        }
    }


    //FUNCTION METHOD TO CALL THE CONNECTION AFTER PRESSING THE BUTTON

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {

        super.configureFlutterEngine(flutterEngine)

        channel = MethodChannel(flutterEngine.dartExecutor.binaryMessenger, "com.mycompany.serviceTest/printer").apply {
            setMethodCallHandler { call, result ->
                // Handle Method Calls from Flutter
                if (! isBound)
                    KEMVPROC.bindService(connection, context, vector, client, emvVersion)
                else{
                    val version = mService?.FWL_getVersion()
                    Log.i(TAG, "Version KEMV : $version")
                }
            }
        }
    }
}
