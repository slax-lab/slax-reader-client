package com.slax.reader.di

import com.slax.reader.repository.dataModule
import org.koin.dsl.module

val appModule = module {
    includes(dataModule)
}