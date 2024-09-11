import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.content.Context
import android.graphics.Color
import android.widget.Toast
import com.ams.timesyncedualertv2.R
import com.ams.timesyncedualertv2.model.CourseEntity

class CourseAdapter(private val context: Context, private val courses: List<CourseEntity>) : RecyclerView.Adapter<CourseAdapter.CourseViewHolder>() {

    class CourseViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.textView_course_name)
        val locationTextView: TextView = view.findViewById(R.id.textView_course_location)
        val timeTextView: TextView = view.findViewById(R.id.textView_course_time)
        val cardView: View = view.findViewById(R.id.card_background)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_course, parent, false)
        return CourseViewHolder(view)
    }

    override fun onBindViewHolder(holder: CourseViewHolder, position: Int) {
        val course = courses[position]
        holder.nameTextView.text = course.name
        holder.locationTextView.text = course.location
        holder.timeTextView.text = context.getString(R.string.course_time_format, course.startTime, course.endTime)
        holder.cardView.background.setTint(course.color)
        // 根据背景颜色调整字体颜色
        val textColor = if (isColorDark(course.color)) {
            Color.WHITE // 如果背景颜色较暗，字体使用白色
        } else {
            Color.BLACK // 如果背景颜色较亮，字体使用黑色
        }

        // 设置字体颜色
        holder.nameTextView.setTextColor(textColor)
        holder.locationTextView.setTextColor(textColor)
        holder.timeTextView.setTextColor(textColor)


        holder.itemView.setOnClickListener {
            //TODO:Handle item click here if needed
            Toast.makeText(context, "Clicked on ${course.name}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return courses.size
    }

    private fun isColorDark(color: Int): Boolean {
        val darkness = 1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255
        return darkness >= 0.5
    }
}
