 package com.slobodianiuk.topapps.presentation

import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.PersistableBundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.slobodianiuk.topapps.R
import com.slobodianiuk.topapps.model.parse.FeedEntry
import com.slobodianiuk.topapps.model.parse.ParseItem
import com.slobodianiuk.topapps.model.itemRecycler.FeedAdapter
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.MalformedURLException
import java.net.URL

class MainActivity : AppCompatActivity() {

    object MainConstants {
        const val STATE_URL = "feedUrl"
        const val STATE_LIMIT = "feedLimit"
        const val TAG = "LOG"
    }
    private var feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
    private var feedLimit = 10
    private var feedCachedUrl = ""
    private var rssFeed = ""
    private val appList : ArrayList<FeedEntry> = arrayListOf()
    private var handler: Handler? = null
    private val thread = HandlerThread("Thread1")

    private var items: RecyclerView? = null
    private var itemAdapter: FeedAdapter<FeedEntry>? = null
    private var itemLayoutManager : RecyclerView.LayoutManager? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Log.d(MainConstants.TAG, "onCreate: start")
        if (savedInstanceState != null) {
            feedUrl = savedInstanceState.getString(MainConstants.STATE_URL).toString()
            feedLimit = savedInstanceState.getInt(MainConstants.STATE_LIMIT)
        }

        items = findViewById(R.id.xmlRecyclerView)
        items.let {
            if (it != null) {
                attachAdapter(it)
            }
        }


        displayData()
        Log.d(MainConstants.TAG, "onCreate: done")
    }



    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.feeds_menu, menu)
        if (feedLimit == 10) {
            menu?.findItem(R.id.mnu10)?.isChecked = true
        } else {
            menu?.findItem(R.id.mnu25)?.isChecked = true
        }
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.mnuFree -> feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topfreeapplications/limit=%d/xml"
            R.id.mnuPaid -> feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topalbums/limit=%d/xml"
            R.id.mnuSongs -> feedUrl = "http://ax.itunes.apple.com/WebObjects/MZStoreServices.woa/ws/RSS/topsongs/limit=%d/xml"
            R.id.mnu10 -> if (!item.isChecked) {
                item.isChecked = true
                feedLimit = 10
            }
            R.id.mnu25 -> if (!item.isChecked) {
                item.isChecked = true
                feedLimit = 35 - feedLimit
            }
            R.id.mnuRefresh -> feedCachedUrl = "REFRESH"
            else -> return super.onOptionsItemSelected(item)
        }
        downloadUrl(handler, String.format(feedUrl, feedLimit))
        return true
    }
    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        outState.putString(MainConstants.STATE_URL, feedUrl)
        outState.putInt(MainConstants.STATE_LIMIT, feedLimit)
        super.onSaveInstanceState(outState, outPersistentState)
    }

    private fun attachAdapter(rView : RecyclerView) {
        itemAdapter = FeedAdapter(appList,rView)
        itemLayoutManager = LinearLayoutManager(this)
        rView.adapter = itemAdapter
        rView.layoutManager = itemLayoutManager
    }
    private fun displayData() {
        thread.start()
        val looper = thread.looper
        handler = Handler(looper)
        downloadUrl(handler, String.format(feedUrl, feedLimit))
    }
    private fun downloadUrl (h: Handler?, Url: String) {
        if (feedUrl != feedCachedUrl) {
            h?.post (object : Runnable {
                override fun run() {
                    rssFeed = downloadXml(Url)
                    val app = ParseItem()
                    val success = app.parse(rssFeed)
                    if (success) {
                        val feedAdapter = FeedAdapter(app.application,items!!)
                        runOnUiThread(object : Runnable {
                            override fun run() {
                                items?.adapter = feedAdapter
                            }
                        })
                    }
                }
            })
        }
    }
    private fun downloadXml(urlPath: String) : String {
        val xmlResult = StringBuilder("")
        try {
            val url = URL(urlPath)
            val connection = url.openConnection()
            val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
            var charsRead: Int
            val inputBuffer = CharArray(500)
            while(true) {
                charsRead = reader.read(inputBuffer)
                if (charsRead < 0) {
                    break
                }
                if (charsRead > 0) {
                    xmlResult.append(inputBuffer, 0, charsRead)
                }
            }
            reader.close()
            return xmlResult.toString()
        } catch (e: MalformedURLException) {
            Log.e(MainConstants.TAG, "downloadXml: Invalid URL ${e.message}" )
        } catch (e: IOException) {
            Log.e(MainConstants.TAG, "downloadXml: IO exception ${e.message}" )
        } catch (e: SecurityException) {
            Log.e(MainConstants.TAG, "downloadXml: Security exception ${e.message}" )
        }
        return ""
    }

   /* private fun recyclerViewListener(context: Context, rv: RecyclerView?) {
        rv?.addOnItemTouchListener(RecyclerItemClickListener(context, rv, object : RecyclerViewItemOnClick {
            override fun onItemClick(v: View?, position: Int) {
                Toast.makeText(context, "TEST CLICK", Toast.LENGTH_SHORT).show()
            }

            override fun onLongItemClick(v: View?, position: Int) {

            }
        }))
    }*/
    /*private fun recyclerListener(context: Context, rv: RecyclerView?) {
       val itemListener = RecyclerItemClickListener(context, rv!!, object : ItemOnClick {

           override fun onDoubleItemClick(v: View?, position: Int) {
               val viewType = rv.findViewHolderForAdapterPosition(position)?.itemViewType
               
               Toast.makeText(context, "TEST DOUBLE CLICK ${position +1} item with view type $viewType", Toast.LENGTH_SHORT).show()
           }

           override fun onLongItemClick(v: View?, position: Int) {
               Toast.makeText(context, "TEST LONG CLICK ${position +1} item", Toast.LENGTH_SHORT).show()
           }
       })

       rv.setOnTouchListener(View.OnTouchListener { _, event ->
           itemListener.gestureDetector!!.onTouchEvent(event)
           false
       })
    }*/
}