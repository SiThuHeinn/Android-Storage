package com.sithuhein.androidstorage

import android.os.Build


fun <T>sdk29AndUp( onSdk29AndUp : () -> T ) : T?{
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
        onSdk29AndUp()
    } else null
}