package org.o7planning.myapplication.Admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import org.o7planning.myapplication.Admin.PagerAdapterUpgrade
import org.o7planning.myapplication.R
import org.o7planning.myapplication.databinding.FragmentUpgradeBinding

class FragmentUpgrade : Fragment(R.layout.fragment_upgrade) {
    private lateinit var viewPager: ViewPager2
    private lateinit var adapter: PagerAdapterUpgrade
    private lateinit var dbRefUser: DatabaseReference

    private lateinit var binding: FragmentUpgradeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentUpgradeBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewPager = binding.viewPager
        adapter = PagerAdapterUpgrade(requireActivity())
        viewPager.adapter = adapter

        dbRefUser = FirebaseDatabase.getInstance().getReference("dataUser")

        var currenStep = 0
        updateStepBackground(currenStep)

        binding.btnNext.setOnClickListener {
            if (binding.btnNext.text == "Hoàn tất nâng cấp"){
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null){
                    upgradeToOwner(currentUserId)
                }else{
                    Toast.makeText(requireContext(), "Lỗi: Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.fragment_login)
                }
            }
            val nextItem = viewPager.currentItem + 1
            if (nextItem < adapter.itemCount){
                viewPager.currentItem = nextItem
                currenStep = nextItem
                updateStepBackground(currenStep)
                if (nextItem == 2){
                    binding.btnNext.setText("Hoàn tất nâng cấp")
                }else{
                    binding.btnNext.setText("Tiếp theo")
                }
            }
        }
        binding.btnBack.setOnClickListener {
            val backItem = viewPager.currentItem - 1
            if (backItem == -1){
                findNavController().navigate(R.id.fragment_profile)
                (activity as MainActivity).setupBottomNavigation(false)
            }
            if (backItem >= 0){
                viewPager.currentItem = backItem
                currenStep = backItem
                updateStepBackground(currenStep)
                if (backItem == 2){
                    binding.btnNext.setText("Hoàn tất nâng cấp")
                }else{
                    binding.btnNext.setText("Tiếp theo")
                }
            }
        }
    }
    private fun updateStepBackground(step: Int){
        val step1Background = if(step == 0) R.drawable.bg_upgrade_orgrien else R.drawable.bg_upgrade_white
        val step2Background = if(step == 1) R.drawable.bg_upgrade_orgrien else R.drawable.bg_upgrade_white
        val step3Background = if (step == 2) R.drawable.bg_upgrade_orgrien else R.drawable.bg_upgrade_white

        binding.ivStep1.setBackgroundResource(step1Background)
        binding.ivStep2.setBackgroundResource(step2Background)
        binding.ivStep3.setBackgroundResource(step3Background)
    }

    private fun upgradeToOwner(userId: String){
        val db = FirebaseFirestore.getInstance()
        val update = hashMapOf<String, Any>(
            "role" to "owner"
        )

        db.collection("users").document(userId)
            .update(update)
            .addOnSuccessListener {
                Log.d("Firestore", "Nâng cấp vai trò Firestore thành OWNER thành công.")

                // CHỈ CHUYỂN SANG BƯỚC 2 (RTDB) KHI FIRESTORE THÀNH CÔNG
                updateRtdbRole(userId, update)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Lỗi khi nâng cấp vai trò Firestore", e)
                Toast.makeText(requireContext(), "Lỗi: Không thể nâng cấp tài khoản. Vui lòng thử lại.", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateRtdbRole(userId: String, update: HashMap<String, Any>) {
        dbRefUser.child(userId).updateChildren(update)
            .addOnSuccessListener {
                Log.d("Firestore", "Nâng cấp vai trò thành OWNER thành công.")
                Toast.makeText(requireContext(), "Tài khoản đã được nâng cấp lên Chủ quán!", Toast.LENGTH_LONG).show()
                completeUpgradeUI()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Lỗi khi nâng cấp vai trò", e)
                Toast.makeText(requireContext(), "Lỗi: Không thể nâng cấp tài khoản. Vui lòng thử lại.", Toast.LENGTH_LONG).show()
            }
    }

    private fun completeUpgradeUI() {
        findNavController().navigate(R.id.fragment_overview)
        (activity as MainActivity).setupBottomNavigation(true)
    }

}