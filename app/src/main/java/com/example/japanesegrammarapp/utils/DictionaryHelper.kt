package com.example.japanesegrammarapp.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.japanesegrammarapp.R

enum class DictionaryApp(val nameResId: Int) {
    EUDIC(R.string.dict_eudic),
    DICTANGO(R.string.dict_dictango),
    MOJI(R.string.dict_moji);

    fun search(context: Context, word: String) {
        when (this) {
            EUDIC -> DictionaryHelper.searchInEudic(context, word)
            DICTANGO -> DictionaryHelper.searchInDictango(context, word)
            MOJI -> DictionaryHelper.searchInMoji(context, word)
        }
    }
}

object DictionaryHelper {

    fun searchInEudic(context: Context, word: String) {
        try {
            val intent = Intent("eudic.intent.action.SEARCH_SHORT")
            intent.putExtra("eudic.intent.extra.word", word)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showNotInstalledMessage(context, "欧路词典 (Eudic)")
        }
    }

    fun searchInDictango(context: Context, word: String) {
        try {
            val intent = Intent(Intent.ACTION_PROCESS_TEXT)
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, word)
            intent.type = "text/plain"
            intent.setPackage("cn.jimex.dict")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            showNotInstalledMessage(context, "Dictango")
        }
    }

    fun searchInMoji(context: Context, word: String) {
        try {
            val intent = Intent(Intent.ACTION_PROCESS_TEXT)
            intent.putExtra(Intent.EXTRA_PROCESS_TEXT, word)
            intent.type = "text/plain"
            intent.setPackage("com.mojitec.mojidict")
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Fallback to SEND intent if PROCESS_TEXT fails for some reason
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND)
                fallbackIntent.putExtra(Intent.EXTRA_TEXT, word)
                fallbackIntent.type = "text/plain"
                fallbackIntent.setPackage("com.mojitec.mojidict")
                fallbackIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(fallbackIntent)
            } catch (e2: ActivityNotFoundException) {
                showNotInstalledMessage(context, "MOJi辞书")
            }
        }
    }

    private fun showNotInstalledMessage(context: Context, appName: String) {
        val message = context.getString(R.string.dict_not_installed, appName)
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
