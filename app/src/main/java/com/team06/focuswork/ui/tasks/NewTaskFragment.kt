package com.team06.focuswork.ui.tasks

import android.app.AlertDialog.*
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import com.team06.focuswork.R
import com.team06.focuswork.data.FireBaseFireStoreUtil
import com.team06.focuswork.data.Task
import com.team06.focuswork.databinding.FragmentNewTaskBinding
import com.team06.focuswork.model.TasksViewModel
import com.team06.focuswork.ui.util.DatePickerFragment
import com.team06.focuswork.ui.util.TimePickerFragment
import java.text.DateFormat.*
import java.util.*


class NewTaskFragment : Fragment() {

    private lateinit var workingTask: Task;
    var startCalendar: MutableLiveData<Calendar> = MutableLiveData(Calendar.getInstance())
    var endCalendar: MutableLiveData<Calendar> = MutableLiveData(Calendar.getInstance())

    private val startDatePicker = DatePickerFragment(startCalendar)
    private val startTimePicker = TimePickerFragment(startCalendar)
    private val endDatePicker = DatePickerFragment(endCalendar)
    private val endTimePicker = TimePickerFragment(endCalendar)
    private lateinit var binding: FragmentNewTaskBinding
    private val fireBaseStore = FireBaseFireStoreUtil()
    private val tasksViewModel: TasksViewModel by activityViewModels()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        binding = FragmentNewTaskBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val taskNameView = binding.taskName
        val taskDescriptionView = binding.taskDescription

        taskNameView.doAfterTextChanged { checkTextFilled(view) }
        taskDescriptionView.doAfterTextChanged { checkTextFilled(view) }

        tasksViewModel.currentTask.value.let { task ->
            if(task != null) {
                taskNameView.setText(task.taskName)
                taskDescriptionView.setText(task.taskDescription)
                startCalendar.value = task.startTime
                endCalendar.value = task.endTime

                (activity as AppCompatActivity).supportActionBar?.title = getString(R.string.title_edit_task)
                binding.taskCreate.text = getString(R.string.action_save_task)
            } else {
                endCalendar.value?.add(Calendar.HOUR, 1);

                (activity as AppCompatActivity).supportActionBar?.title =  getString(R.string.menu_new_task)
                binding.taskCreate.text = getString(R.string.action_create_task)
            }
        }

        prepareStartDateTextView(binding.taskStartDate)
        prepareStartTimeTextView(binding.taskStartTime)
        prepareEndDateTextView(binding.taskEndDate)
        prepareEndTimeTextView(binding.taskEndTime)
        setupObserverCallbacks()

        binding.taskCreate.setOnClickListener {
            saveTask()
            findNavController().navigateUp()
        }
        binding.taskSaveTemplate.setOnClickListener {saveTemplate()}
    }

    private fun saveTemplate() {
        val alertBuilder: Builder = Builder(requireContext())
        alertBuilder.setTitle("Title")

        val input = EditText(requireContext())
        input.inputType = InputType.TYPE_CLASS_TEXT
        alertBuilder.setView(input)

        alertBuilder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
            onTemplateSavedOK(input.text.toString())})
        alertBuilder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

        alertBuilder.show()
    }

    private fun onTemplateSavedOK(title: String) {
        val sharedPref = activity?.getPreferences(Context.MODE_PRIVATE) ?: return
        val template = mutableSetOf(binding.taskName.text.toString(),
                binding.taskDescription.text.toString())
        if (!sharedPref.contains("template_$title")) {
            with(sharedPref.edit()) {
                putStringSet("template_$title", template)
                apply()
            }
        } else
            showToast(getString(R.string.template_exists_toast))
    }

    private fun saveTask() {
        workingTask = Task(
                binding.taskName.text.toString(),
                binding.taskDescription.text.toString(),
                startCalendar.value!!,
                endCalendar.value!!)
        fireBaseStore.saveTask(workingTask, tasksViewModel::setSelectedTask)
    }

    private fun prepareStartTimeTextView(startTimeTextView: TextView) {
        startTimeTextView.text = formatTime(startCalendar.value)
        startTimeTextView.setOnClickListener {
            startTimePicker.arguments = createDateOrTimeBundle(isDate = false, startBundle = true)
            startTimePicker.show(childFragmentManager, TimePickerFragment.TAG)
        }
    }

    private fun prepareStartDateTextView(taskStartDate: TextView) {
        taskStartDate.text = formatDate(startCalendar.value)
        taskStartDate.setOnClickListener {
            startDatePicker.arguments = createDateOrTimeBundle(isDate = true, startBundle = true)
            startDatePicker.show(childFragmentManager, DatePickerFragment.TAG)
        }
    }

    private fun prepareEndTimeTextView(endTimeTextView: TextView) {
        endTimeTextView.text = formatTime(endCalendar.value)
        endTimeTextView.setOnClickListener {
            endTimePicker.arguments = createDateOrTimeBundle(isDate = false, startBundle = false)
            endTimePicker.show(childFragmentManager, TimePickerFragment.TAG)
        }
    }

    private fun prepareEndDateTextView(endDateTextView: TextView) {
        endDateTextView.text = formatDate(endCalendar.value)
        endDateTextView.setOnClickListener {
            endDatePicker.arguments = createDateOrTimeBundle(isDate = true, startBundle = false)
            endDatePicker.show(childFragmentManager, DatePickerFragment.TAG)
        }
    }

    private fun setupObserverCallbacks() {
        startCalendar.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            val cal = it ?: return@Observer
            val nowCal = Calendar.getInstance()
            nowCal.add(Calendar.MINUTE, -1)
            if (cal.before(nowCal)) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                startCalendar.value = cal
            }
            if (endCalendar.value?.before(cal) ?: return@Observer) {
                val newEndCal = GregorianCalendar(
                        startCalendar.value?.get(Calendar.YEAR) ?: return@Observer,
                        startCalendar.value?.get(Calendar.MONTH) ?: return@Observer,
                        startCalendar.value?.get(Calendar.DAY_OF_MONTH) ?: return@Observer,
                        endCalendar.value?.get(Calendar.HOUR_OF_DAY) ?: return@Observer,
                        endCalendar.value?.get(Calendar.MINUTE) ?: return@Observer
                )
                endCalendar.value = newEndCal
                binding.taskEndDate.text = formatDate(newEndCal)
                binding.taskEndTime.text = formatTime(newEndCal)
            }
            binding.taskStartDate.text = formatDate(cal)
            binding.taskStartTime.text = formatTime(cal)
        })
        endCalendar.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            var cal = it ?: return@Observer
            if (cal.before(startCalendar.value)) {
                val newEndCal = GregorianCalendar(
                        startCalendar.value?.get(Calendar.YEAR)!!,
                        startCalendar.value?.get(Calendar.MONTH)!!,
                        startCalendar.value?.get(Calendar.DAY_OF_MONTH)!!,
                        endCalendar.value?.get(Calendar.HOUR_OF_DAY)!!,
                        endCalendar.value?.get(Calendar.MINUTE)!!)
                if (newEndCal.before(startCalendar.value)) {
                    newEndCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                endCalendar.value = newEndCal
                cal = newEndCal
            }
            binding.taskEndDate.text = formatDate(cal)
            binding.taskEndTime.text = formatTime(cal)
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(context?.applicationContext, message, Toast.LENGTH_LONG).show()
    }

    private fun formatDate(cal: Calendar?): String {
        return getDateInstance(MEDIUM).format(cal!!.time)
    }

    private fun formatTime(cal: Calendar?): String {
        return getTimeInstance(SHORT).format(cal!!.time)
    }

    private fun checkTextFilled(view: View) {
        view.findViewById<Button>(R.id.taskCreate).isEnabled =
            !(view.findViewById<TextView>(R.id.taskName).text.isBlank() ||
                view.findViewById<TextView>(R.id.taskDescription).text.isBlank())
        view.findViewById<Button>(R.id.taskSaveTemplate).isEnabled =
                !(view.findViewById<TextView>(R.id.taskName).text.isBlank() ||
                        view.findViewById<TextView>(R.id.taskDescription).text.isBlank())
    }

    private fun createDateOrTimeBundle(isDate: Boolean, startBundle: Boolean): Bundle {
        val cal = if (startBundle) startCalendar.value else endCalendar.value
        return if (isDate) bundleOf(
                Pair("YEAR", cal?.get(Calendar.YEAR)),
                Pair("MONTH", cal?.get(Calendar.MONTH)),
                Pair("DAY", cal?.get(Calendar.DAY_OF_MONTH)),
                Pair(
                        "MIN_DATE",
                        if (startBundle) System.currentTimeMillis() - 1000
                        else startCalendar.value?.timeInMillis
                )
        ) else bundleOf(
                Pair("HOUR", cal?.get(Calendar.HOUR_OF_DAY)),
                Pair("MINUTE", cal?.get(Calendar.MINUTE))
        )
    }
}