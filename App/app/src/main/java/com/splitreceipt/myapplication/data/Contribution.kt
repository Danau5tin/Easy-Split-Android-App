package com.splitreceipt.myapplication.data

class Contribution {

    var contributor: String? = null
    var contributee: String? = null
    var contribValue: Float? = null

    companion object {
        fun createContributionsFromString(entireContribString: String) : ArrayList<Contribution>{
            val contribArray: ArrayList<Contribution> = ArrayList()
            val splitContributions = entireContribString.split("/")
            for (contribString in splitContributions) {
                val newContribution = Contribution()
                val contribDetails = contribString.split(",")
                newContribution.contributor = contribDetails[0]
                newContribution.contribValue = contribDetails[1].toFloat()
                newContribution.contributee = contribDetails[2]
                contribArray.add(newContribution)
            }
            return contribArray
        }
    }
}