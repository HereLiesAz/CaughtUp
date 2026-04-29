
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.channels.FileChannel
import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

class ResearchAgent(context: Context) {
    private var interpreter: Interpreter? = null
    private val vocab = mutableListOf<String>()

    init {
        // 1. Load Model
        val assetFileDescriptor = context.assets.openFd("research_agent.tflite")
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, assetFileDescriptor.startOffset, assetFileDescriptor.declaredLength)
        
        val options = Interpreter.Options().apply {
            setUseNNAPI(true)
        }
        interpreter = Interpreter(modelBuffer, options)

        // 2. Load Vocab
        context.assets.open("research_agent_vocab.txt").bufferedReader().useLines { lines ->
            lines.forEach { vocab.add(it) }
        }
    }

    fun classifySnippet(text: String): Float {
        // The TextVectorization layer is INSIDE the TFLite model,
        // so we pass the raw String if the model was exported with the string input,
        // otherwise we would pre-tokenize here using the loaded 'vocab'.
        val input = arrayOf(text)
        val output = Array(1) { FloatArray(1) }
        
        interpreter?.run(input, output)
        return output[0][0] // Confidence score (0.0 to 1.0)
    }
}
