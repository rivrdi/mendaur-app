package com.dicoding.abednego.mendaurid.ui.postartikel

import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.dicoding.abednego.mendaurid.R
import com.dicoding.abednego.mendaurid.databinding.ActivityPostArtikelBinding
import com.dicoding.abednego.mendaurid.ui.daftarartikel.DaftarArtikelActivity
import com.dicoding.abednego.mendaurid.utils.Result
import com.dicoding.abednego.mendaurid.utils.reduceFileImage
import com.dicoding.abednego.mendaurid.utils.uriToFile
import com.dicoding.abednego.mendaurid.viewmodel.ViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File

class PostArtikelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPostArtikelBinding
    private lateinit var auth: FirebaseAuth
    private var getFile: File? = null
    private val postArtikelViewModel: PostArtikelViewModel by viewModels {
        ViewModelFactory()
    }
    private lateinit var progressDialog: ProgressDialog


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPostArtikelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = getString(R.string.title_post_artikel)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle(getString(R.string.memuat))
        progressDialog.setMessage(getString(R.string.mohon_menunggu))
        progressDialog.setCancelable(false)

        auth = FirebaseAuth.getInstance()

        var uid: String? = null

        val sharedPreferences = getSharedPreferences(MY_PREF, Context.MODE_PRIVATE)
        val accountId = sharedPreferences.getString(ACCOUNT_ID, null)
        if (accountId != null) {
            uid = accountId
        }

        val currentUser = auth.currentUser
        val photoUrl = currentUser?.photoUrl

        Glide.with(this)
            .load(photoUrl)
            .into(binding.ivProfile)

        binding.ivArtikel.setOnClickListener {
            startGallery()
        }

        binding.btnSend.setOnClickListener{

            val id = uid!!.toRequestBody("text/plain".toMediaTypeOrNull())
            val title = binding.etJudulArtikel.text.toString()
            if(title.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_title), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val content = binding.etIsiArtikel.text.toString()
            if(content.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_content), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val file = getFile?.takeIf { it.exists() } ?: run {
                Toast.makeText(this, getString(R.string.empty_file), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            progressDialog.show()

            val reducedFile = reduceFileImage(file)
            val requestImageFile = reducedFile.asRequestBody(getString(R.string.media_type).toMediaTypeOrNull())
            val imageMultipart: MultipartBody.Part = MultipartBody.Part.createFormData(
                FILE,
                file.name,
                requestImageFile
            )

            postArtikelViewModel.postArticle(
                id,
                title.toRequestBody("text/plain".toMediaTypeOrNull()),
                content.toRequestBody("text/plain".toMediaTypeOrNull()),
                imageMultipart
            ).observe(this) { result ->
                when (result) {
                    is Result.Success -> {
                        progressDialog.dismiss()
                        val data = result.data
                        val message = data.message
                        Toast.makeText(this,
                            message,
                            Toast.LENGTH_LONG).show()
                        val intent = Intent(this, DaftarArtikelActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    is Result.Error -> {
                        progressDialog.dismiss()
                        Toast.makeText(
                            this,
                            getString(R.string.post_tidak_berhasil),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun startGallery() {
        val intent = Intent()
        intent.action = Intent.ACTION_GET_CONTENT
        intent.type = getString(R.string.intent_type)
        val chooser = Intent.createChooser(intent, getString(R.string.choose_image))
        launcherIntentGallery.launch(chooser)
    }

    private val launcherIntentGallery = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val selectedImg: Uri = result.data?.data as Uri
            val myFile = uriToFile(selectedImg, this@PostArtikelActivity)
            getFile = myFile
            binding.ivArtikel.setImageURI(selectedImg)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val MY_PREF = "MyPrefs"
        const val ACCOUNT_ID = "account_id"
        const val FILE = "file"
    }
}