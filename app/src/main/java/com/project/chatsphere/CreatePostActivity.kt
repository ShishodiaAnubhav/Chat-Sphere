package com.project.chatsphere

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.project.chatsphere.daos.PostDao

class CreatePostActivity : AppCompatActivity() {

    private lateinit var postDao: PostDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_post)

        postDao = PostDao()

        val mPostButton = findViewById<Button>(R.id.postButton)
        mPostButton.setOnClickListener {
            val input = findViewById<EditText>(R.id.postInput).text.toString().trim()
            if(input.isNotEmpty()){
                postDao.addPost(input)
                finish()
                val mainActivityIntent = Intent(this, MainActivity::class.java)
                startActivity(mainActivityIntent)
                finish()
            }
        }
    }
}