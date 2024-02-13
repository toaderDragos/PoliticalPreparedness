package com.example.android.politicalpreparedness.election

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.android.politicalpreparedness.R
import com.example.android.politicalpreparedness.database.utils.hideLoadingAnimation
import com.example.android.politicalpreparedness.database.utils.setDisplayHomeAsUpEnabled
import com.example.android.politicalpreparedness.database.utils.setTitle
import com.example.android.politicalpreparedness.databinding.FragmentElectionBinding
import com.example.android.politicalpreparedness.election.adapter.ElectionListAdapter
import org.koin.androidx.viewmodel.ext.android.viewModel

class ElectionsFragment: Fragment() {

    // ViewModel with Koin
    val _viewModel: ElectionsViewModel by viewModel()
    private lateinit var binding: FragmentElectionBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?)
    : View? {
        binding = DataBindingUtil.inflate(
                inflater,
                R.layout.fragment_election, container, false
        )
        binding.viewModel = _viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.upcomingElectionsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.savedElectionsRecycler.layoutManager = LinearLayoutManager(requireContext())


        // Adapters
        val apiAdapter = ElectionListAdapter( ElectionListAdapter.CustomOnClickListener {
            this.findNavController().navigate(ElectionsFragmentDirections.actionElectionsFragmentToVoterInfoFragment(it))
        })
        val roomAdapter = ElectionListAdapter( ElectionListAdapter.CustomOnClickListener {
            this.findNavController().navigate(ElectionsFragmentDirections.actionElectionsFragmentToVoterInfoFragment(it))
        })
        binding.upcomingElectionsRecycler.adapter = apiAdapter
        binding.savedElectionsRecycler.adapter = roomAdapter   // It's the same!

        // showLoadingAnimation()
        // Populate recycler adapters
        _viewModel.getUpcomingElections()
        _viewModel.upcomingElectionsList.observe(viewLifecycleOwner) {
            it?.let {
                apiAdapter.submitList(it)
               //  hideLoadingAnimationIfDataIsReady()
            }
        }

        _viewModel.getSavedElections()
        _viewModel.savedElectionsList.observe(viewLifecycleOwner) {
            it?.let {
                roomAdapter.submitList(it)
                // hideLoadingAnimationIfDataIsReady()
            }
        }

        // Found in Extensions.kt
        setTitle(getString(R.string.app_name))
        setDisplayHomeAsUpEnabled(false)
        return binding.root
    }

    private fun hideLoadingAnimationIfDataIsReady() {
        if (_viewModel.upcomingElectionsList.value != null && _viewModel.savedElectionsList.value != null) {
            hideLoadingAnimation()
        }
    }


}