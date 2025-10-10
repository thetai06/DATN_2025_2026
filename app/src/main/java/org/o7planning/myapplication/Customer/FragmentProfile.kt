package org.o7planning.myapplication.Customer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import org.o7planning.myapplication.Admin.MainActivity
import org.o7planning.myapplication.R
import org.o7planning.myapplication.data.dataUser
import org.o7planning.myapplication.databinding.FragmentProfileBinding

class FragmentProfile : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var dbRefUser: DatabaseReference
    private lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()
        dbRefUser = FirebaseDatabase.getInstance().getReference("dataUser")

        boxProfile()
        boxFeatureProfile()
    }

    private fun boxProfile() {
        val userId = mAuth.currentUser?.uid.toString()

        dbRefUser.child(userId).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()){
                    val dbuser = snapshot.getValue(dataUser::class.java)
                    binding.apply {
                        nameProfile.setText(dbuser?.name)
                        emailProfile.setText(dbuser?.email)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {
            }

        })

    }

    private fun boxFeatureProfile() {
        val list = mutableListOf<OutDataFeatureProfile>()
        list.add(OutDataFeatureProfile("Nâng cấp tài khoản", R.drawable.icons8pro64))
        list.add(OutDataFeatureProfile("Lịch sử đặt bàn  ", R.drawable.icons8calendar24))
        list.add(OutDataFeatureProfile("Phương thức thanh toán", R.drawable.icons8card24))
        list.add(OutDataFeatureProfile("Thông báo", R.drawable.icons8notification24))
        list.add(OutDataFeatureProfile("Thông tin cá nhân", R.drawable.icons8person24))
        list.add(OutDataFeatureProfile("Cài đặt", R.drawable.icons8settings24))
        list.add(OutDataFeatureProfile("Đăng xuất", R.drawable.icons8export24))



        binding.rvFeatureProfile.adapter = RvFeatureProfile(list).apply {
            onClickItem = { item, pos ->
                handleClick(item, pos)
            }
        }
        binding.rvFeatureProfile.layoutManager = GridLayoutManager(
            requireContext(), 1, GridLayoutManager.VERTICAL, false
        )
    }
    fun handleClick(item: OutDataFeatureProfile, position: Int) {
        when (position) {
            0 -> {
                Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.fragment_upgrade)
                (activity as MainActivity).hideBottomNavigation()
            }
            1 -> {
                Toast.makeText(requireContext(), item.name , Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.fragment_history)
                (activity as MainActivity).hideBottomNavigation()
            }
            2 -> {
                Toast.makeText(requireContext(), item.name , Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.fragment_payment_method)
                (activity as MainActivity).hideBottomNavigation()
            }
            3 -> {
                Toast.makeText(requireContext(), item.name , Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.fragment_notification)
                (activity as MainActivity).hideBottomNavigation()
            }
            4 -> {
                Toast.makeText(requireContext(), item.name , Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.fragment_personal_information)
                (activity as MainActivity).hideBottomNavigation()
            }
            5 -> {
                Toast.makeText(requireContext(), item.name , Toast.LENGTH_SHORT).show()
            }
            else  -> {
                Toast.makeText(requireContext(), item.name, Toast.LENGTH_SHORT).show()
                val navOptions = androidx.navigation.navOptions {
                    popUpTo(R.id.nav_graph){
                        inclusive = true
                    }
                }
                mAuth.signOut()
                (activity as MainActivity).hideBottomNavigation()
                findNavController().navigate(R.id.fragment_splash,null,navOptions)
            }
        }
    }




}