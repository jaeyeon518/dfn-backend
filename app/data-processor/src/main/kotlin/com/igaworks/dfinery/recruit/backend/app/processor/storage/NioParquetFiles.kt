package com.igaworks.dfinery.recruit.backend.app.processor.storage

import org.apache.parquet.io.DelegatingSeekableInputStream
import org.apache.parquet.io.InputFile
import org.apache.parquet.io.OutputFile
import org.apache.parquet.io.PositionOutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption

internal object NioParquetFiles {

    fun outputFile(path: Path): OutputFile = NioOutputFile(path)

    fun inputFile(path: Path): InputFile = NioInputFile(path)

    private class NioOutputFile(
        private val path: Path
    ) : OutputFile {

        override fun create(blockSizeHint: Long): PositionOutputStream {
            return newStream(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)
        }

        override fun createOrOverwrite(blockSizeHint: Long): PositionOutputStream {
            return newStream(
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            )
        }

        override fun supportsBlockSize(): Boolean = false

        override fun defaultBlockSize(): Long = 0L

        private fun newStream(vararg options: OpenOption): PositionOutputStream {
            path.parent?.let(Files::createDirectories)
            val channel = Files.newByteChannel(path, *options)
            return object : PositionOutputStream() {
                override fun getPos(): Long = channel.position()

                override fun write(b: Int) {
                    write(byteArrayOf(b.toByte()), 0, 1)
                }

                override fun write(b: ByteArray, off: Int, len: Int) {
                    writeFully(channel, ByteBuffer.wrap(b, off, len))
                }

                override fun close() {
                    channel.close()
                }
            }
        }
    }

    private class NioInputFile(
        private val path: Path
    ) : InputFile {

        override fun getLength(): Long = Files.size(path)

        override fun newStream(): DelegatingSeekableInputStream {
            val channel = Files.newByteChannel(path, StandardOpenOption.READ)
            val inputStream = Channels.newInputStream(channel)
            return object : DelegatingSeekableInputStream(inputStream) {
                override fun getPos(): Long = channel.position()

                override fun seek(newPos: Long) {
                    channel.position(newPos)
                }

                override fun readFully(bytes: ByteArray) {
                    readFully(bytes, 0, bytes.size)
                }

                override fun readFully(bytes: ByteArray, start: Int, len: Int) {
                    val buffer = ByteBuffer.wrap(bytes, start, len)
                    readFully(channel, buffer)
                }

                override fun close() {
                    channel.close()
                }
            }
        }
    }

    private fun writeFully(channel: SeekableByteChannel, buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            channel.write(buffer)
        }
    }

    private fun readFully(channel: SeekableByteChannel, buffer: ByteBuffer) {
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) < 0) {
                throw java.io.EOFException("Reached end of parquet input")
            }
        }
    }
}
