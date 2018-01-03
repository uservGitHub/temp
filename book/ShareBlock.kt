package whtong

import android.util.Log

/**
 * Created by work on 2018/1/3.
 */

class ShareBlock private constructor(queryLength: Int) {
    var length: Int = queryLength

    init {
        Log.i("_shareblock", "$queryLength")
        Log.i("_shareblock", "$length")
    }

    companion object {
        @Volatile
        var instance: ShareBlock? = null

        fun getInstance(length: Int): ShareBlock {
            if (instance == null) {
                synchronized(ShareBlock::class) {
                    if (instance == null) {
                        instance = ShareBlock(length)
                    }
                }
            }
            return instance!!
        }
    }
}