package com.poc.atmdepositbalancereader

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_tetected_details.*

class DetectedDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tetected_details)

        val detectedDetail = intent.getSerializableExtra("Data") as ArrayList<String>
        rc_DetectedTextListHolder?.layoutManager = LinearLayoutManager(baseContext)
        rc_DetectedTextListHolder?.adapter = DetectTextListAdapter(baseContext, detectedDetail)

    }
}