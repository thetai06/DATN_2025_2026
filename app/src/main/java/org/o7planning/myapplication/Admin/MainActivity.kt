package org.o7planning.myapplication.Admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationBarView.OnItemSelectedListener {
    private lateinit var binding: ActivityMainBinding
    lateinit var navController: NavController
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mAuth = FirebaseAuth.getInstance()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        binding.bottomNavigation.visibility = View.GONE

        val colorSelect = ContextCompat.getColorStateList(this, R.color.bottom_nav_color_selector)
        binding.bottomNavigation.itemTextColor = colorSelect
        binding.bottomNavigation.itemIconTintList = colorSelect
    }

    fun navigateBasedOnRole(userId: String, navOptions: NavOptions?) {
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val role = document.getString("role")
                val isOwner = role == "owner"

                setupBottomNavigation(isOwner)

                if (isOwner) {
                    navController.navigate(R.id.fragment_overview, null, navOptions)
                } else {
                    navController.navigate(R.id.fragment_home, null, navOptions)
                }
            }
            .addOnFailureListener {
                mAuth.signOut()
                hideBottomNavigation()
                navController.navigate(R.id.fragment_login)
            }
    }


    fun setupBottomNavigation(isOwner: Boolean) {
        binding.bottomNavigation.menu.clear()
        if (isOwner) {
            binding.bottomNavigation.inflateMenu(R.menu.owner_menu)
        } else {
            binding.bottomNavigation.inflateMenu(R.menu.customer_menu)
        }
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController)
        binding.bottomNavigation.setOnItemSelectedListener(this)
        showBottomNavigation()
    }

    fun showBottomNavigation() {
        binding.bottomNavigation.visibility = View.VISIBLE
    }

    fun hideBottomNavigation() {
        binding.bottomNavigation.visibility = View.GONE
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        return NavigationUI.onNavDestinationSelected(p0, navController)
    }

}