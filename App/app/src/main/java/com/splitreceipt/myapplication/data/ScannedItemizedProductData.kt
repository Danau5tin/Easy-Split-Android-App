package com.splitreceipt.myapplication.data

data class ScannedItemizedProductData (var itemName: String, var itemValue: String,
                                       var potentialError: Boolean, var ownership: String = "Equal",
                                        var sqlRowId: String)