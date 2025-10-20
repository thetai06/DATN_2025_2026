package org.o7planning.myapplication.admin

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class RegistrationViewModel : ViewModel() {

    // --- Bước 1 ---
    private val _storeName = MutableLiveData<String>()
    val storeName: LiveData<String> = _storeName
    fun setStoreName(name: String) { _storeName.value = name }

    private val _phoneNumber = MutableLiveData<String>()
    val phoneNumber: LiveData<String> = _phoneNumber
    fun setPhoneNumber(phone: String) { _phoneNumber.value = phone }

    private val _address = MutableLiveData<String>()
    val address: LiveData<String> = _address
    fun setAddress(addr: String) { _address.value = addr }

    private val _email = MutableLiveData<String>()
    val email: LiveData<String> = _email
    fun setEmail(mail: String) { _email.value = mail }

    private val _description = MutableLiveData<String>()
    val description: LiveData<String> = _description
    fun setDescription(desc: String) { _description.value = desc }

    // --- Bước 2: Ảnh ---
    private val _businessLicenseUri = MutableLiveData<Uri?>()
    val businessLicenseUri: LiveData<Uri?> = _businessLicenseUri
    fun setBusinessLicenseUri(uri: Uri) { _businessLicenseUri.value = uri }

    private val _nationalIdFrontUri = MutableLiveData<Uri?>()
    val nationalIdFrontUri: LiveData<Uri?> = _nationalIdFrontUri
    fun setNationalIdFrontUri(uri: Uri) { _nationalIdFrontUri.value = uri }

    private val _nationalIdBackUri = MutableLiveData<Uri?>()
    val nationalIdBackUri: LiveData<Uri?> = _nationalIdBackUri
    fun setNationalIdBackUri(uri: Uri) { _nationalIdBackUri.value = uri }

    // --- Bước 2: Ngân hàng ---
    private val _bankAccountNumber = MutableLiveData<String>()
    val bankAccountNumber: LiveData<String> = _bankAccountNumber
    fun setBankAccountNumber(num: String) { _bankAccountNumber.value = num }

    private val _edtNameBank = MutableLiveData<String>()
    val edtNameBank: LiveData<String> = _edtNameBank
    fun setBankName(name: String) { _edtNameBank.value = name } // Đổi tên hàm

    private val _edtDateBank = MutableLiveData<String>()
    val edtDateBank: LiveData<String> = _edtDateBank
    fun setBankHolderName(name: String) { _edtDateBank.value = name } // Đổi tên hàm

    // --- Bước 3 ---
    private val _checkboxWallet = MutableLiveData<Boolean>(false)
    val checkboxWallet: LiveData<Boolean> = _checkboxWallet
    fun setCheckboxWallet(isChecked: Boolean) { _checkboxWallet.value = isChecked }

    private val _checkboxBank = MutableLiveData<Boolean>(false)
    val checkboxBank: LiveData<Boolean> = _checkboxBank
    fun setCheckboxBank(isChecked: Boolean) { _checkboxBank.value = isChecked }


    // --- Các hàm Validation ---
    fun isStepOneValid(): Boolean {
        return !storeName.value.isNullOrBlank() &&
                !phoneNumber.value.isNullOrBlank() &&
                !address.value.isNullOrBlank() &&
                !email.value.isNullOrBlank()
    }

    fun isStepTwoValid(): Boolean {
        return businessLicenseUri.value != null &&
                nationalIdFrontUri.value != null &&
                nationalIdBackUri.value != null &&
                !bankAccountNumber.value.isNullOrBlank() &&
                !edtNameBank.value.isNullOrBlank() &&
                !edtDateBank.value.isNullOrBlank()
    }

    fun isStepThreeValid(): Boolean {
        val isPaymentMethodSelected = checkboxBank.value == true || checkboxWallet.value == true
        return isPaymentMethodSelected
    }
}