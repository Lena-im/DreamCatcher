package edu.vt.cs.cs5254.dreamcatcher

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Adapter
import androidx.fragment.app.Fragment
import android.widget.Button
import android.widget.CheckBox
import androidx.core.content.FileProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs.cs5254.dreamcatcher.databinding.FragmentDreamDetailBinding
import edu.vt.cs.cs5254.dreamcatcher.databinding.ListItemDreamEntryBinding
import edu.vt.cs.cs5254.dreamcatcher.util.CameraUtil
import java.io.File
import java.text.DateFormat
import java.util.UUID

private const val ARG_DREAM_ID = "dream_id"
private const val TAG = "Detail"
private const val DIALOG_ADD_REFLECTION = "DialogAddReflection"
private const val REQUEST_ADD_REFLECTION = 0

class DreamDetailFragment : Fragment(), AddReflectionDialog.Callbacks {
    private lateinit var dreamWithEntries: DreamWithEntries
    private lateinit var photoFile: File
    private lateinit var photoUri: Uri

    private var _binding: FragmentDreamDetailBinding? = null
    private val binding: FragmentDreamDetailBinding get() = _binding!!

    private var adapter: DreamEntryAdapter? = null

    private val viewModel: DreamDetailViewModel by lazy {
        ViewModelProvider(this).get(DreamDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        val dreamId: UUID = arguments?.getSerializable(ARG_DREAM_ID) as UUID
        viewModel.loadDreamWithEntries(dreamId)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_dream_detail, menu)
        val cameraAvailable = CameraUtil.isCameraAvailable(requireActivity())
        val menuItem = menu.findItem(R.id.take_dream_photo)
        menuItem.apply {
            Log.d(TAG, "Camera available: $cameraAvailable")
            isEnabled = cameraAvailable
            isVisible = cameraAvailable
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.take_dream_photo -> {
                val captureImageIntent =
                    CameraUtil.createCaptureImageIntent(requireActivity(), photoUri)
                startActivity(captureImageIntent)
                true
            }
            R.id.share_dream -> {
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, getDreamReport())
                    putExtra(
                        Intent.EXTRA_SUBJECT,
                        getString(R.string.dream_report_subject))
                }.also { intent ->
                    val chooserIntent =
                        Intent.createChooser(intent, getString(R.string.send_dream_report))
                    startActivity(chooserIntent)
                }
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    private fun getDreamReport(): String {

        // create string parts
        val newline = System.getProperty("line.separator")
        val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
        val dateString = df.format(dreamWithEntries.dream.date)
        val dateMessage = getString(R.string.dream_report_date, dateString)
        val stateString = if (dreamWithEntries.dream.isFulfilled) {
            getString(R.string.dream_report_fulfilled)
        }
        else if (dreamWithEntries.dream.isDeferred) {
            getString(R.string.dream_report_deferred)
        }
        else {
            null
        }
        val reflectionString = getString(R.string.dream_report_reflections)

        // create and return complete string
        val sb = StringBuilder()
        sb.append(">> ${dreamWithEntries.dream.title} << $newline")
        sb.append("- $dateMessage $newline")
        sb.append("- $reflectionString $newline")
        dreamWithEntries.dreamEntries
            .filter { dreamEntry ->  dreamEntry.kind == DreamEntryKind.REFLECTION}
            .forEach {
                val reflection = it.text
                sb.append("- $reflection $newline")
            }
        if (dreamWithEntries.dream.isFulfilled or dreamWithEntries.dream.isDeferred){
            sb.append("$stateString $newline")
        }
        return sb.toString()
    }


        override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentDreamDetailBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.dreamEntryRecyclerView.layoutManager = LinearLayoutManager(context)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dreamWithEntriesLiveData.observe(
            viewLifecycleOwner,
            Observer { dreamWithEntriesFromViewModel ->
                dreamWithEntriesFromViewModel?.let {
                    this.dreamWithEntries = dreamWithEntriesFromViewModel
                    photoFile = viewModel.getPhotoFile(dreamWithEntriesFromViewModel.dream)
                    photoUri = FileProvider.getUriForFile(requireActivity(),
                        "com.bignerdranch.android.criminalintent.fileprovider",

                        photoFile)
                    updateUI()
                }
            })
    }


    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?, start: Int, count: Int, after: Int) { }
            override fun onTextChanged(sequence: CharSequence?,
                                       start: Int, before: Int, count: Int) {
                dreamWithEntries.dream.title = sequence.toString()
            }
            override fun afterTextChanged(sequence: Editable?) { }
        }
        binding.dreamTitleText.addTextChangedListener(titleWatcher)
        binding.dreamFulfilledCheckbox.apply {
            setOnClickListener { checkbox ->
                val isChecked = (checkbox as CheckBox).isChecked
                dreamWithEntries.dream.isFulfilled = isChecked
                if (isChecked){
                    dreamWithEntries.dreamEntries +=
                        DreamEntry(kind = DreamEntryKind.FULFILLED,
                            dreamId = dreamWithEntries.dream.id)
                }else{
                    dreamWithEntries.dreamEntries = dreamWithEntries.dreamEntries.filterNot { it.kind == DreamEntryKind.FULFILLED }
                }
                updateUI()
            }
        }
        binding.dreamDeferredCheckbox.apply {
            setOnClickListener { checkbox ->
                val isChecked = (checkbox as CheckBox).isChecked
                dreamWithEntries.dream.isDeferred = isChecked
                if (isChecked){
                    dreamWithEntries.dreamEntries +=
                        DreamEntry(kind = DreamEntryKind.DEFERRED,
                            dreamId = dreamWithEntries.dream.id)
                }else{
//                    dreamWithEntries.dreamEntries = dreamWithEntries.dreamEntries.dropLast(1)
                    dreamWithEntries.dreamEntries = dreamWithEntries.dreamEntries.filterNot { it.kind == DreamEntryKind.DEFERRED }
                }
                updateUI()
            }
        }

        binding.addReflectionButton.setOnClickListener {
            AddReflectionDialog().apply{
                setTargetFragment(this@DreamDetailFragment, REQUEST_ADD_REFLECTION)
                show(this@DreamDetailFragment.parentFragmentManager, DIALOG_ADD_REFLECTION)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStop() {
        super.onStop()
        viewModel.saveDream(dreamWithEntries)
    }

    private fun updateUI() {

        binding.dreamTitleText.setText(dreamWithEntries.dream.title)

        when {
            dreamWithEntries.dream.isFulfilled -> {
                binding.dreamFulfilledCheckbox.isChecked = true
                binding.dreamDeferredCheckbox.isEnabled = false
                binding.addReflectionButton.isEnabled = false
            }
            dreamWithEntries.dream.isDeferred -> {
                binding.dreamDeferredCheckbox.isChecked = true
                binding.dreamFulfilledCheckbox.isEnabled = false
            }
            else -> {
                binding.dreamFulfilledCheckbox.isChecked = false
                binding.dreamFulfilledCheckbox.isEnabled = true
                binding.dreamDeferredCheckbox.isChecked = false
                binding.dreamDeferredCheckbox.isEnabled = true
                binding.addReflectionButton.isEnabled = true
            }
        }
        binding.dreamFulfilledCheckbox.jumpDrawablesToCurrentState()
        binding.dreamDeferredCheckbox.jumpDrawablesToCurrentState()

        adapter = DreamEntryAdapter(dreamWithEntries.dreamEntries)
        binding.dreamEntryRecyclerView.adapter = adapter
        var itemTouchHelper:ItemTouchHelper = ItemTouchHelper(DreamEntrySwipeToDeleteCallback())
        itemTouchHelper.attachToRecyclerView(binding.dreamEntryRecyclerView)

        updatePhotoView()

    }

    private fun updatePhotoView() {
        if (photoFile.exists()) {
            val bitmap = CameraUtil.getScaledBitmap(photoFile.path, requireActivity())
            binding.dreamPhoto.setImageBitmap(bitmap)
        } else {
            binding.dreamPhoto.setImageDrawable(null)
        }
    }

    private fun updateEntryButton(dreamEntry: DreamEntry, dreamEntryButton: Button) {

        when {
            dreamEntry.kind == DreamEntryKind.CONCEIVED -> {
                dreamEntryButton.setText(dreamEntry.kind.toString())
                dreamEntryButton.setBackgroundColor(Color.parseColor("#98c1d9"))
                dreamEntryButton.setTextColor(Color.BLACK)
            }
            dreamEntry.kind == DreamEntryKind.REFLECTION -> {
                val df = DateFormat.getDateInstance(DateFormat.MEDIUM)
                val date = df.format(dreamEntry.date)
                dreamEntryButton.setText("${date}: ${dreamEntry.text}")
                dreamEntryButton.setBackgroundColor(Color.parseColor("#e0fbfc"))
                dreamEntryButton.setTextColor(Color.BLACK)
            }
            dreamEntry.kind == DreamEntryKind.FULFILLED -> {
                dreamEntryButton.setText(dreamEntry.kind.toString())
                dreamEntryButton.setBackgroundColor(Color.parseColor("#3d5a80"))
                dreamEntryButton.setTextColor(Color.WHITE)
            }
            dreamEntry.kind == DreamEntryKind.DEFERRED -> {
                dreamEntryButton.setText(dreamEntry.kind.toString())
                dreamEntryButton.setBackgroundColor(Color.parseColor("#9e2a2b"))
                dreamEntryButton.setTextColor(Color.WHITE)
            }
        }

    }

    inner class DreamEntryHolder(val entryBinding: ListItemDreamEntryBinding)
        : RecyclerView.ViewHolder(entryBinding.root), View.OnClickListener {

        private lateinit var dreamEntry: DreamEntry

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(dreamEntry: DreamEntry){
            this.dreamEntry = dreamEntry

            updateEntryButton(this.dreamEntry, entryBinding.dreamEntryButton)
        }

        override fun onClick(v: View?) {
            TODO("Not yet implemented")
        }
    }


    private inner class DreamEntryAdapter( var dreamEntries: List<DreamEntry>)
        :RecyclerView.Adapter<DreamEntryHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DreamEntryHolder {
            val entryBinding = ListItemDreamEntryBinding.
                inflate(LayoutInflater.from(parent.context), parent, false)
            return DreamEntryHolder(entryBinding)
        }

        override fun getItemCount() = dreamEntries.size

        override fun onBindViewHolder(holder: DreamEntryHolder, position: Int) {
            val dreamEntry = dreamEntries[position]
            holder.bind(dreamEntry)
        }

    }

    override fun onReflectionProvided(reflectionText: String) {
        dreamWithEntries.dreamEntries +=
            DreamEntry(text = reflectionText,
                kind = DreamEntryKind.REFLECTION,
                dreamId = dreamWithEntries.dream.id)
        updateUI()
    }

    inner class DreamEntrySwipeToDeleteCallback() :
        ItemTouchHelper.SimpleCallback(0,ItemTouchHelper.LEFT) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            TODO("Not yet implemented")
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            var position = viewHolder.adapterPosition
            if (dreamWithEntries.dreamEntries[position].kind == DreamEntryKind.REFLECTION){
                dreamWithEntries.dreamEntries = dreamWithEntries.dreamEntries.
                filterIndexed{index, dreamEntry ->
                    index != position
                }
            }
            updateUI()
        }
    }

    companion object {
        fun newInstance(dreamId: UUID): DreamDetailFragment {
            val args = Bundle().apply {
                putSerializable(ARG_DREAM_ID, dreamId)
            }
            return DreamDetailFragment().apply {
                arguments = args
            }
        }
    }



}