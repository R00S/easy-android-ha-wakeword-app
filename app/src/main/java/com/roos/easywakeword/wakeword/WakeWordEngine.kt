/*
 * Wake Word Detection Engine
 * 
 * This file is derived from OpenWakeWord for Android by Hasanat Ahmed Lodhi
 * (https://github.com/hasanatlodhi/OpenwakewordforAndroid), which is itself
 * based on OpenWakeWord by David Scripka (https://github.com/dscripka/openWakeWord).
 * 
 * Original work licensed under Apache License, Version 2.0.
 * Modifications and Kotlin port by Easy Android HA Wakeword App Contributors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.roos.easywakeword.wakeword

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.res.AssetManager
import android.util.Log
import java.nio.FloatBuffer
import java.util.ArrayDeque
import kotlin.random.Random

/**
 * ONNX model runner for wake word detection.
 * Uses melspectrogram and embedding models to generate features,
 * then predicts wake word activation.
 */
class OnnxModelRunner(private val assetManager: AssetManager) {
    
    companion object {
        private const val TAG = "OnnxModelRunner"
        private const val BATCH_SIZE = 1
        
        // Mel spectrogram transformation constants
        // These normalize the spectrogram values to match the expected model input range
        private const val MEL_SPEC_SCALE_DIVISOR = 10.0f
        private const val MEL_SPEC_OFFSET = 2.0f
    }
    
    private val ortEnv: OrtEnvironment
    private var wakeWordSession: OrtSession? = null
    
    init {
        try {
            Log.d(TAG, "Initializing ONNX Runtime environment...")
            ortEnv = OrtEnvironment.getEnvironment()
            
            Log.d(TAG, "Loading wake word model...")
            val modelBytes = readModelFile("hey_mycroft.onnx")
            Log.d(TAG, "Creating ONNX session (${modelBytes.size} bytes)...")
            wakeWordSession = ortEnv.createSession(modelBytes)
            Log.d(TAG, "Wake word model loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ONNX model runner: ${e.message}", e)
            throw RuntimeException("Failed to initialize wake word model", e)
        }
    }
    
    fun getMelSpectrogram(inputArray: FloatArray): Array<FloatArray>? {
        var session: OrtSession? = null
        var inputTensor: OnnxTensor? = null
        
        try {
            val modelBytes = readModelFile("melspectrogram.onnx")
            session = ortEnv.createSession(modelBytes)
            
            val samples = inputArray.size
            val floatBuffer = FloatBuffer.wrap(inputArray)
            inputTensor = OnnxTensor.createTensor(ortEnv, floatBuffer, longArrayOf(BATCH_SIZE.toLong(), samples.toLong()))
            
            val inputName = session.inputNames.iterator().next()
            session.run(mapOf(inputName to inputTensor)).use { results ->
                @Suppress("UNCHECKED_CAST")
                val outputTensor = results[0].value as Array<Array<Array<FloatArray>>>
                val squeezed = squeeze(outputTensor)
                val transformed = applyMelSpecTransform(squeezed)
                Log.d(TAG, "Computed mel spectrogram: ${transformed.size} frames of ${transformed[0].size} mel bins")
                return transformed
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error computing mel spectrogram: ${e.message}", e)
            return null
        } finally {
            inputTensor?.close()
            session?.close()
        }
    }
    
    fun generateEmbeddings(input: Array<Array<Array<FloatArray>>>): Array<FloatArray>? {
        var session: OrtSession? = null
        var inputTensor: OnnxTensor? = null
        
        try {
            val modelBytes = readModelFile("embedding_model.onnx")
            session = ortEnv.createSession(modelBytes)
            
            inputTensor = OnnxTensor.createTensor(ortEnv, input)
            
            session.run(mapOf("input_1" to inputTensor)).use { results ->
                @Suppress("UNCHECKED_CAST")
                val rawOutput = results[0].value as Array<Array<Array<FloatArray>>>
                
                // Reshape from (n, 1, 1, 96) to (n, 96)
                val reshapedOutput = Array(rawOutput.size) { FloatArray(rawOutput[0][0][0].size) }
                for (i in rawOutput.indices) {
                    System.arraycopy(rawOutput[i][0][0], 0, reshapedOutput[i], 0, rawOutput[i][0][0].size)
                }
                Log.d(TAG, "Generated embeddings: ${reshapedOutput.size} frames of ${reshapedOutput[0].size} features")
                return reshapedOutput
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating embeddings: ${e.message}", e)
            return null
        } finally {
            inputTensor?.close()
            session?.close()
        }
    }
    
    fun predictWakeWord(inputArray: Array<Array<FloatArray>>): Float {
        val session = wakeWordSession
        if (session == null) {
            Log.w(TAG, "Wake word session is null, returning 0")
            return 0f
        }
        var inputTensor: OnnxTensor? = null
        
        try {
            inputTensor = OnnxTensor.createTensor(ortEnv, inputArray)
            val inputName = session.inputNames.iterator().next()
            
            session.run(mapOf(inputName to inputTensor)).use { results ->
                @Suppress("UNCHECKED_CAST")
                val result = results[0].value as Array<FloatArray>
                val score = result[0][0]
                if (score > 0.01f) {
                    Log.d(TAG, "Wake word model raw score: $score")
                }
                return score
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error predicting wake word: ${e.message}", e)
            return 0f
        } finally {
            inputTensor?.close()
        }
    }
    
    fun close() {
        wakeWordSession?.close()
        wakeWordSession = null
    }
    
    private fun readModelFile(filename: String): ByteArray {
        assetManager.open(filename).use { inputStream ->
            return inputStream.readBytes()
        }
    }
    
    private fun squeeze(originalArray: Array<Array<Array<FloatArray>>>): Array<FloatArray> {
        val height = originalArray[0][0].size
        val width = originalArray[0][0][0].size
        return Array(height) { i ->
            FloatArray(width) { j ->
                originalArray[0][0][i][j]
            }
        }
    }
    
    private fun applyMelSpecTransform(array: Array<FloatArray>): Array<FloatArray> {
        return Array(array.size) { i ->
            FloatArray(array[i].size) { j ->
                array[i][j] / MEL_SPEC_SCALE_DIVISOR + MEL_SPEC_OFFSET
            }
        }
    }
}

/**
 * Wake word detection model that processes audio and detects wake words.
 */
class WakeWordModel(private val modelRunner: OnnxModelRunner) {
    
    companion object {
        private const val TAG = "WakeWordModel"
        private const val SAMPLE_RATE = 16000
        private const val N_PREPARED_SAMPLES = 1280
        private const val MELSPECTROGRAM_MAX_LEN = 10 * 97
        private const val FEATURE_BUFFER_MAX_LEN = 120
        private const val WINDOW_SIZE = 76           // Number of mel-spectrogram frames per window
        private const val MEL_BINS = 32              // Number of mel frequency bins
        private const val STEP_SIZE = 8              // Frame step size for sliding window
        
        // Random initialization data range (simulates audio sample range)
        private const val INIT_RANDOM_RANGE = 2000f
        private const val INIT_RANDOM_OFFSET = 1000f
    }
    
    private var featureBuffer: Array<FloatArray>? = null
    private val rawDataBuffer = ArrayDeque<Float>(SAMPLE_RATE * 10)
    private var rawDataRemainder = FloatArray(0)
    private var melspectrogramBuffer = Array(WINDOW_SIZE) { FloatArray(MEL_BINS) { 1.0f } }
    private var accumulatedSamples = 0
    
    init {
        // Initialize feature buffer with random data to prime the model buffers
        try {
            val randomData = FloatArray(SAMPLE_RATE * 4) { 
                Random.nextFloat() * INIT_RANDOM_RANGE - INIT_RANDOM_OFFSET 
            }
            featureBuffer = getEmbeddings(randomData, WINDOW_SIZE, STEP_SIZE)
            if (featureBuffer != null) {
                Log.d(TAG, "Model initialized successfully, feature buffer size: ${featureBuffer!!.size}")
            } else {
                Log.e(TAG, "Model initialization returned null feature buffer")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing model: ${e.message}", e)
        }
    }
    
    fun predictWakeWord(audioBuffer: FloatArray): Float {
        streamingFeatures(audioBuffer)
        val features = getFeatures(16, -1)
        if (features.isEmpty() || features[0].isEmpty() || features[0][0].isEmpty()) {
            Log.w(TAG, "Empty or malformed features buffer, skipping prediction")
            return 0f
        }
        val score = modelRunner.predictWakeWord(features)
        if (score > 0.01f) {
            Log.d(TAG, "Wake word score: $score, features shape: [${features.size}][${features[0].size}][${features[0][0].size}]")
        }
        return score
    }
    
    private fun getFeatures(nFeatureFrames: Int, startNdxParam: Int): Array<Array<FloatArray>> {
        val buffer = featureBuffer ?: return arrayOf(arrayOf(FloatArray(96)))
        
        val startNdx: Int
        val endNdx: Int
        
        if (startNdxParam != -1) {
            startNdx = startNdxParam
            endNdx = if (startNdxParam + nFeatureFrames != 0) startNdxParam + nFeatureFrames else buffer.size
        } else {
            startNdx = maxOf(0, buffer.size - nFeatureFrames)
            endNdx = buffer.size
        }
        
        val length = endNdx - startNdx
        val result = Array(1) { Array(length) { FloatArray(buffer[0].size) } }
        
        for (i in 0 until length) {
            System.arraycopy(buffer[startNdx + i], 0, result[0][i], 0, buffer[startNdx + i].size)
        }
        
        return result
    }
    
    private fun getEmbeddings(x: FloatArray, windowSize: Int, stepSize: Int): Array<FloatArray>? {
        val spec = modelRunner.getMelSpectrogram(x) ?: return null
        val windows = mutableListOf<Array<FloatArray>>()
        
        var i = 0
        while (i <= spec.size - windowSize) {
            val window = Array(windowSize) { j -> 
                FloatArray(spec[0].size).also { 
                    System.arraycopy(spec[i + j], 0, it, 0, spec[0].size)
                }
            }
            if (window.size == windowSize) {
                windows.add(window)
            }
            i += stepSize
        }
        
        // Convert to batch with extra dimension
        val batch = Array(windows.size) { idx ->
            Array(windowSize) { j ->
                Array(spec[0].size) { k ->
                    floatArrayOf(windows[idx][j][k])
                }
            }
        }
        
        return modelRunner.generateEmbeddings(batch)
    }
    
    private fun bufferRawData(x: FloatArray?) {
        if (x == null) return
        
        while (rawDataBuffer.size + x.size > SAMPLE_RATE * 10) {
            rawDataBuffer.poll()
        }
        for (value in x) {
            rawDataBuffer.offer(value)
        }
    }
    
    private fun streamingMelSpectrogram(nSamples: Int) {
        if (rawDataBuffer.size < 400) {
            throw IllegalArgumentException("Need at least 400 samples")
        }
        
        val tempSize = nSamples + 480
        val tempArray = FloatArray(tempSize)
        val rawDataArray = rawDataBuffer.toTypedArray()
        val startIdx = maxOf(0, rawDataArray.size - tempSize)
        
        for (i in startIdx until rawDataArray.size) {
            tempArray[i - startIdx] = rawDataArray[i]
        }
        
        val newMelSpectrogram = modelRunner.getMelSpectrogram(tempArray) ?: return
        
        // Combine buffers
        val combined = Array(melspectrogramBuffer.size + newMelSpectrogram.size) { i ->
            if (i < melspectrogramBuffer.size) {
                melspectrogramBuffer[i]
            } else {
                newMelSpectrogram[i - melspectrogramBuffer.size]
            }
        }
        melspectrogramBuffer = combined
        
        // Trim if needed
        if (melspectrogramBuffer.size > MELSPECTROGRAM_MAX_LEN) {
            val startTrim = melspectrogramBuffer.size - MELSPECTROGRAM_MAX_LEN
            melspectrogramBuffer = Array(MELSPECTROGRAM_MAX_LEN) { melspectrogramBuffer[startTrim + it] }
        }
    }
    
    private fun streamingFeatures(audioBuffer: FloatArray): Int {
        var processedSamples = 0
        accumulatedSamples = 0
        
        var buffer = audioBuffer
        
        if (rawDataRemainder.isNotEmpty()) {
            buffer = rawDataRemainder + audioBuffer
            rawDataRemainder = FloatArray(0)
        }
        
        if (accumulatedSamples + buffer.size >= N_PREPARED_SAMPLES) {
            val remainder = (accumulatedSamples + buffer.size) % N_PREPARED_SAMPLES
            if (remainder != 0) {
                val evenChunks = buffer.copyOfRange(0, buffer.size - remainder)
                bufferRawData(evenChunks)
                accumulatedSamples += evenChunks.size
                rawDataRemainder = buffer.copyOfRange(buffer.size - remainder, buffer.size)
            } else {
                bufferRawData(buffer)
                accumulatedSamples += buffer.size
                rawDataRemainder = FloatArray(0)
            }
        } else {
            accumulatedSamples += buffer.size
            bufferRawData(buffer)
        }
        
        if (accumulatedSamples >= N_PREPARED_SAMPLES && accumulatedSamples % N_PREPARED_SAMPLES == 0) {
            streamingMelSpectrogram(accumulatedSamples)
            
            val x = Array(1) { Array(WINDOW_SIZE) { Array(MEL_BINS) { floatArrayOf(0f) } } }
            
            for (i in (accumulatedSamples / N_PREPARED_SAMPLES - 1) downTo 0) {
                // Correct index calculation: for i=0, use full buffer size; for i>0, offset backwards
                val ndx = if (i == 0) melspectrogramBuffer.size else melspectrogramBuffer.size - STEP_SIZE * i
                
                // Ensure we don't go out of bounds
                if (ndx <= 0 || ndx > melspectrogramBuffer.size) continue
                
                val start = maxOf(0, ndx - WINDOW_SIZE)
                val end = minOf(ndx, melspectrogramBuffer.size)
                
                // Only process if we have enough frames
                if (end - start < WINDOW_SIZE) continue
                
                for ((k, j) in (start until end).withIndex()) {
                    for (w in 0 until MEL_BINS) {
                        x[0][k][w][0] = melspectrogramBuffer[j][w]
                    }
                }
                
                if (x[0].size == WINDOW_SIZE) {
                    val newFeatures = modelRunner.generateEmbeddings(x)
                    if (newFeatures != null) {
                        featureBuffer = if (featureBuffer == null) {
                            newFeatures
                        } else {
                            val current = featureBuffer!!
                            Array(current.size + newFeatures.size) { idx ->
                                if (idx < current.size) current[idx] else newFeatures[idx - current.size]
                            }
                        }
                    }
                }
            }
            processedSamples = accumulatedSamples
            accumulatedSamples = 0
        }
        
        // Trim feature buffer
        featureBuffer?.let { buffer ->
            if (buffer.size > FEATURE_BUFFER_MAX_LEN) {
                val startTrim = buffer.size - FEATURE_BUFFER_MAX_LEN
                featureBuffer = Array(FEATURE_BUFFER_MAX_LEN) { buffer[startTrim + it] }
            }
        }
        
        return if (processedSamples != 0) processedSamples else accumulatedSamples
    }
}
