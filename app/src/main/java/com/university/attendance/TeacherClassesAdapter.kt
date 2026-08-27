package com.university.attendance

import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TeacherClassesAdapter(
    private val onClick: (Subject) -> Unit
) : RecyclerView.Adapter<TeacherClassesAdapter.ClassViewHolder>() {

    private val items =
        mutableListOf<Subject>()

    fun submitList(
        newList: List<Subject>
    ) {

        items.clear()
        items.addAll(newList)

        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ClassViewHolder {

        val context =
            parent.context

        val container =
            LinearLayout(context).apply {

                orientation =
                    LinearLayout.VERTICAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    32,
                    28,
                    32,
                    28
                )

                layoutParams =
                    RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {

                        setMargins(
                            20,
                            10,
                            20,
                            10
                        )
                    }

                setBackgroundColor(
                    android.graphics.Color.WHITE
                )

                isClickable = true
                isFocusable = true
            }

        val subjectName =
            TextView(context).apply {

                textSize = 18f

                setTypeface(
                    null,
                    Typeface.BOLD
                )

                setTextColor(
                    android.graphics.Color.rgb(
                        25,
                        35,
                        55
                    )
                )
            }

        val courseCode =
            TextView(context).apply {

                textSize = 14f

                setTextColor(
                    android.graphics.Color.DKGRAY
                )

                setPadding(
                    0,
                    8,
                    0,
                    0
                )
            }

        val program =
            TextView(context).apply {

                textSize = 14f

                setTextColor(
                    android.graphics.Color.GRAY
                )

                setPadding(
                    0,
                    5,
                    0,
                    0
                )
            }

        val semester =
            TextView(context).apply {

                textSize = 14f

                setTextColor(
                    android.graphics.Color.GRAY
                )

                setPadding(
                    0,
                    5,
                    0,
                    0
                )
            }

        container.addView(
            subjectName
        )

        container.addView(
            courseCode
        )

        container.addView(
            program
        )

        container.addView(
            semester
        )

        return ClassViewHolder(
            container,
            subjectName,
            courseCode,
            program,
            semester
        )
    }

    override fun onBindViewHolder(
        holder: ClassViewHolder,
        position: Int
    ) {

        holder.bind(
            items[position]
        )
    }

    override fun getItemCount(): Int =
        items.size

    inner class ClassViewHolder(
        itemView: View,
        private val subjectName: TextView,
        private val courseCode: TextView,
        private val program: TextView,
        private val semester: TextView
    ) : RecyclerView.ViewHolder(itemView) {

        fun bind(
            subject: Subject
        ) {

            subjectName.text =
                subject.subjectName

            courseCode.text =
                "Course Code: ${subject.courseCode}"

            program.text =
                "Program: ${subject.programName}"

            semester.text =
                "Semester: ${subject.semester}"

            itemView.setOnClickListener {

                onClick(subject)
            }
        }
    }
}

