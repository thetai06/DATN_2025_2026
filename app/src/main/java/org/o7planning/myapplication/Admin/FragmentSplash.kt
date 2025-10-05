package org.o7planning.myapplication.Admin

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.google.firebase.auth.FirebaseAuth
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.FragmentSplashBinding

class FragmentSplash : Fragment() {
    private lateinit var binding: FragmentSplashBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var handler: Handler
    private lateinit var runnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSplashBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()

        showLoading()

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable{
            hideLoading()
        }

        handler.postDelayed(runnable, 3000)

        checkUserStatusAndNavigate()
    }

    private fun completeNavigation(targetId: Int, options: NavOptions?) {
        handler.removeCallbacks(runnable)
        findNavController().navigate(targetId, null, options)
    }

    private fun checkUserStatusAndNavigate() {
        val currenUser = mAuth.currentUser

        if (currenUser == null) {
            findNavController().navigate(R.id.fragment_login)
            (activity as MainActivity).hideBottomNavigation()
            return
        }

        val navOptions = navOptions {
            popUpTo(R.id.fragment_splash) {
                inclusive = true
            }
        }

        (activity as MainActivity).navigateBasedOnRole(currenUser.uid, navOptions)
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.textView.visibility = View.GONE
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.textView.visibility = View.VISIBLE
    }

    override fun onStop() {
        super.onStop()
        handler.removeCallbacks(runnable)
    }
}