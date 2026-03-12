package com.pdv.data

object StoreConfig {
    fun getName(): String = Config.get("store.name", "Minha Loja")
    fun getAddress(): String = Config.get("store.address", "")
    fun getDocument(): String = Config.get("store.document", "")

    fun setName(v: String) = Config.set("store.name", v)
    fun setAddress(v: String) = Config.set("store.address", v)
    fun setDocument(v: String) = Config.set("store.document", v)
}

