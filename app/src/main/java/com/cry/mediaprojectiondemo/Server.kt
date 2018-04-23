package com.cry.mediaprojectiondemo

import android.graphics.Bitmap
import android.net.LocalServerSocket
import android.net.LocalSocket
import android.system.Os.read
import java.io.*

/**
 * Created by a2957 on 4/21/2018.
 */
class Server {

    var serverSocket: LocalServerSocket? = null
    var socket: LocalSocket? = null

    companion object {
        fun start(): Server {
            val server = Server()
//
            Thread {
                println("listen.....")
                try {
                    server.serverSocket = LocalServerSocket("puppet-ver1")

                    server.socket = server.serverSocket!!.accept()

//                    acceptConnect(socket)
                } catch (e: Exception) {
                    server.serverSocket = LocalServerSocket("puppet-ver1")
                }
            }.start()
            return server
        }
    }


    private fun acceptConnect(socket: LocalSocket) {
        println("accepted...")
//        write(socket)
    }

    fun write(bitmap: Bitmap) {
        write(socket!!, bitmap)
    }

    private fun write(socket: LocalSocket, bitmap: Bitmap) {
        object : Thread() {
            override fun run() {
                super.run()
                try {
                    val VERSION = 2
                    val outputStream = BufferedOutputStream(socket.outputStream)
                    while (true) {
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteArrayOutputStream)

                        outputStream.write(2)
                        writeInt(outputStream, byteArrayOutputStream.size())
                        outputStream.write(byteArrayOutputStream.toByteArray())
                        outputStream.flush()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }.start()
    }

    @Throws(IOException::class)
    private fun writeInt(outputStream: OutputStream, v: Int) {
        outputStream.write(v shr 24)
        outputStream.write(v shr 16)
        outputStream.write(v shr 8)
        outputStream.write(v)
    }


}