import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateJsonAdapter : JsonAdapter<Date>() {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @FromJson
    override fun fromJson(reader: JsonReader): Date? {
        return try {
            val dateString = reader.nextString()
            dateFormat.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    @ToJson
    override fun toJson(writer: JsonWriter, value: Date?) {
        if (value != null) {
            writer.value(dateFormat.format(value))
        }
    }
}