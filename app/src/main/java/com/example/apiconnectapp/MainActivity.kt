package com.example.apiconnectapp

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.OvershootInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var searchEditText: EditText
    private lateinit var searchButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var resultTextView: TextView
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var resultsCard: LinearLayout
    private lateinit var logoImage: ImageView
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var resultsHeaderText: TextView

    // Get your free API key from https://newsapi.org/
    private val API_KEY = "eb6b5bc876264054ab20156fbf4845fa"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        initializeViews()
        setupClickListeners()
        playEntranceAnimations()
    }

    private fun initializeViews() {
        searchEditText = findViewById(R.id.searchEditText)
        searchButton = findViewById(R.id.searchButton)
        progressBar = findViewById(R.id.progressBar)
        resultTextView = findViewById(R.id.resultTextView)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        resultsCard = findViewById(R.id.resultsCard)
        logoImage = findViewById(R.id.logoImage)
        titleText = findViewById(R.id.titleText)
        subtitleText = findViewById(R.id.subtitleText)
        resultsHeaderText = findViewById(R.id.resultsHeaderText)
    }

    private fun setupClickListeners() {
        searchButton.setOnClickListener {
            animateButtonPress(it)
            performSearch()
        }

        // Handle keyboard search action
        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = searchEditText.text.toString().trim()

        if (query.isEmpty()) {
            shakeView(searchEditText)
            Toast.makeText(this, "Please enter a search query", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show()
            return
        }

        hideKeyboard()
        fetchNews(query)
    }

    private fun playEntranceAnimations() {
        // Logo bounce animation
        logoImage.alpha = 0f
        logoImage.scaleX = 0.5f
        logoImage.scaleY = 0.5f
        logoImage.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(600)
            .setInterpolator(OvershootInterpolator(2f))
            .start()

        // Title slide in
        titleText.alpha = 0f
        titleText.translationY = -30f
        titleText.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setStartDelay(200)
            .start()

        // Subtitle fade in
        subtitleText.alpha = 0f
        subtitleText.animate()
            .alpha(1f)
            .setDuration(500)
            .setStartDelay(400)
            .start()

        // Results card slide up
        resultsCard.alpha = 0f
        resultsCard.translationY = 100f
        resultsCard.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setStartDelay(500)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun animateButtonPress(view: View) {
        val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.95f)
        val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f)
        val scaleUpX = ObjectAnimator.ofFloat(view, "scaleX", 1f)
        val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 1f)

        scaleDownX.duration = 100
        scaleDownY.duration = 100
        scaleUpX.duration = 100
        scaleUpY.duration = 100

        val scaleDown = AnimatorSet()
        scaleDown.playTogether(scaleDownX, scaleDownY)

        val scaleUp = AnimatorSet()
        scaleUp.playTogether(scaleUpX, scaleUpY)
        scaleUp.startDelay = 100

        val bounce = AnimatorSet()
        bounce.playSequentially(scaleDown, scaleUp)
        bounce.start()
    }

    private fun shakeView(view: View) {
        val shake = ObjectAnimator.ofFloat(view, "translationX", 0f, 15f, -15f, 10f, -10f, 5f, -5f, 0f)
        shake.duration = 400
        shake.start()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchEditText.windowToken, 0)
    }

    private fun fetchNews(query: String) {
        showLoading(true)

        val call = RetrofitClient.newsApiService.searchNews(query, API_KEY)

        call.enqueue(object : Callback<NewsResponse> {
            override fun onResponse(call: Call<NewsResponse>, response: Response<NewsResponse>) {
                showLoading(false)

                if (response.isSuccessful) {
                    val newsResponse = response.body()

                    if (newsResponse != null && newsResponse.articles.isNotEmpty()) {
                        displayResults(newsResponse)
                    } else {
                        resultTextView.text = "No articles found for '$query'\n\nTry a different search term!"
                        resultTextView.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_secondary))
                    }
                } else {
                    handleError("Error: ${response.code()} - ${response.message()}")
                }
            }

            override fun onFailure(call: Call<NewsResponse>, t: Throwable) {
                showLoading(false)
                handleError("Network error: ${t.message}")
            }
        })
    }

    private fun displayResults(newsResponse: NewsResponse) {
        resultsHeaderText.text = "Results (${newsResponse.totalResults})"
        
        val sb = StringBuilder()

        newsResponse.articles.forEachIndexed { index, article ->
            sb.append("📰 ${article.title}\n")
            sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
            sb.append("📍 Source: ${article.source.name}\n")

            if (!article.author.isNullOrEmpty()) {
                sb.append("✍️ Author: ${article.author}\n")
            }

            if (!article.description.isNullOrEmpty()) {
                sb.append("\n${article.description}\n")
            }

            sb.append("\n📅 ${formatDate(article.publishedAt)}\n")
            sb.append("🔗 ${article.url}\n")
            
            if (index < newsResponse.articles.size - 1) {
                sb.append("\n\n")
            }
        }

        resultTextView.text = sb.toString()
        resultTextView.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        
        // Animate results appearing
        resultTextView.alpha = 0f
        resultTextView.animate()
            .alpha(1f)
            .setDuration(400)
            .start()
    }

    private fun formatDate(dateString: String): String {
        return try {
            // Simple date formatting - just show the date part
            dateString.substringBefore("T").replace("-", "/")
        } catch (e: Exception) {
            dateString
        }
    }

    private fun handleError(message: String) {
        resultTextView.text = "❌ $message\n\nPlease try again."
        resultTextView.setTextColor(ContextCompat.getColor(this, R.color.error))
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            loadingOverlay.visibility = View.VISIBLE
            loadingOverlay.alpha = 0f
            loadingOverlay.animate()
                .alpha(1f)
                .setDuration(200)
                .start()
        } else {
            loadingOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    loadingOverlay.visibility = View.GONE
                }
                .start()
        }
        
        searchButton.isEnabled = !isLoading
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
