package org.o7planning.myapplication.admin

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.activityViewModels
import org.o7planning.myapplication.databinding.FragmentRvStepOneBinding

class RvStepOneFragment : Fragment() {

    private var _binding: FragmentRvStepOneBinding? = null
    private val binding get() = _binding!!

    private val registrationViewModel: RegistrationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRvStepOneBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupInputListeners()
    }

    private fun setupInputListeners() {
        binding.inputNameClb.doAfterTextChanged { text ->
            registrationViewModel.setStoreName(text.toString().trim())
        }
        binding.inputPhoneNumberClb.doAfterTextChanged { text ->
            registrationViewModel.setPhoneNumber(text.toString().trim())
        }
        binding.inputLocationClb.doAfterTextChanged { text ->
            registrationViewModel.setAddress(text.toString().trim())
        }
        binding.inputEmailClb.doAfterTextChanged { text ->
            registrationViewModel.setEmail(text.toString().trim())
        }
        binding.inputDesClb.doAfterTextChanged { text ->
            registrationViewModel.setDescription(text.toString().trim())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}