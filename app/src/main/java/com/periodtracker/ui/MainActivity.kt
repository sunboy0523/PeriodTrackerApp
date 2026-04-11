package com.periodtracker.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayoutMediator
import com.periodtracker.R
import com.periodtracker.databinding.ActivityMainBinding
import com.periodtracker.viewmodel.PeriodViewModel
import com.periodtracker.viewmodel.PeriodViewModelFactory

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: PeriodViewModel
    private lateinit var pagerAdapter: MainPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(
            this,
            PeriodViewModelFactory(application)
        )[PeriodViewModel::class.java]

        setupViewPager()
        setupBottomNavigation()
        observeViewModel()
    }

    private fun setupViewPager() {
        pagerAdapter = MainPagerAdapter(this)
        binding.viewPager.adapter = pagerAdapter
        binding.viewPager.isUserInputEnabled = true

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                when (position) {
                    0 -> binding.bottomNav.selectedItemId = R.id.nav_calendar
                    1 -> binding.bottomNav.selectedItemId = R.id.nav_stats
                    2 -> binding.bottomNav.selectedItemId = R.id.nav_history
                }
            }
        })
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calendar -> binding.viewPager.currentItem = 0
                R.id.nav_stats -> binding.viewPager.currentItem = 1
                R.id.nav_history -> binding.viewPager.currentItem = 2
            }
            true
        }
    }

    private fun observeViewModel() {
        viewModel.operationResult.observe(this) { result ->
            result?.let {
                when (it) {
                    is com.periodtracker.viewmodel.OperationResult.Success -> {
                        Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
                        viewModel.clearOperationResult()
                    }
                    is com.periodtracker.viewmodel.OperationResult.Error -> {
                        Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
                        viewModel.clearOperationResult()
                    }
                }
            }
        }
    }
}
