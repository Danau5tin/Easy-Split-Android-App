package com.splitreceipt.myapplication.helper_classes

import android.content.Context
import android.util.Log
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.splitreceipt.myapplication.WelcomeJoinActivity
import com.splitreceipt.myapplication.data.FirebaseAccountInfoData
import com.splitreceipt.myapplication.data.ParticipantBalanceData
import de.hdodenhof.circleimageview.CircleImageView

object FirebaseUpdateHelper {

    fun checkGroup(sqlGroupId: String, context: Context, sqlHelper: SqlDbHelper,
                   firebaseDbHelper: FirebaseDbHelper, textView: TextView, circleImageView: CircleImageView, participantList: ArrayList<String>?=null) {
        val accountInfoDbRef = firebaseDbHelper.getAccountInfoListeningRef()
        accountInfoDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            //Listens for changes to the account information
            override fun onCancelled(p0: DatabaseError) {
                Toast.makeText(context, "Failed to sync changes", Toast.LENGTH_SHORT).show()
            }
            override fun onDataChange(data: DataSnapshot) {
                val firebaseGroupData = data.getValue(FirebaseAccountInfoData::class.java)!!
                val sqlGroupData = sqlHelper.retrieveSqlAccountInfoData(sqlGroupId)
                var infoChanged = false
                var imageChanged = false
                var participantsChanged = false
                if (firebaseGroupData.accName != sqlGroupData.accName) {
                    textView.text = firebaseGroupData.accName
                    infoChanged = true
                }
                if (firebaseGroupData.accParticipantLastEdit != sqlGroupData.accParticipantLastEdit){
                    infoChanged = true
                    participantsChanged = true
                }
                if (firebaseGroupData.accLastImage != sqlGroupData.accLastImage) {
                    imageChanged = true
                    infoChanged = true
                }
                if (infoChanged) {
                    sqlHelper.updateGroupInfo(firebaseGroupData, sqlGroupId)
                }
                if (imageChanged) {
                    firebaseDbHelper.downloadGroupProfileImage(context, circleImageView)
                }
                if (participantsChanged) {
                    // Save the new users into SQL
                    checkParticipants(
                        sqlGroupId, sqlHelper,
                        firebaseDbHelper, firebaseGroupData.accParticipantLastEdit, participantList
                    )
                }
            }
        })
    }

    fun checkParticipants(sqlGroupId: String, sqlDbHelper: SqlDbHelper,
                          firebaseDbHelper: FirebaseDbHelper, groupParticipantLastEdit: String,
                          participantList: ArrayList<String>?=null, context: Context?=null, radioGroup: RadioGroup?=null) {
        /*
        Checks the participants in the fireBase database against the participants in the sqlDB
        If fBase user exists in sql ensure uName is the same, if not then save user.
         */
        val usersDbRef = firebaseDbHelper.getUsersListeningRef()
        usersDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(error: DatabaseError) {}
            override fun onDataChange(snapshot: DataSnapshot) {
                val allSqlUsers: ArrayList<ParticipantBalanceData> = ArrayList()
                sqlDbHelper.retrieveParticipants(allSqlUsers, sqlGroupId)
                for (fbUser in snapshot.children) {
                    var userExists = false
                    val fBaseKey = fbUser.key.toString()
                    val downloadedUser = fbUser.getValue(ParticipantBalanceData::class.java)
                    downloadedUser!!.userKey = fBaseKey
                    for (sqlUser in allSqlUsers) {
                        if (sqlUser.userKey == fBaseKey) {
                            // User exists in SQL Database
                            userExists = true
                            if (sqlUser.userName != downloadedUser.userName) {
                                //Users name has been changed. Update SQL.
                                sqlDbHelper.updateParticipantsName(sqlUser,
                                    downloadedUser.userName, groupParticipantLastEdit, sqlGroupId)
                                Log.i("Participants", "Participant: ${sqlUser.userName} exists in DB. Name has been changed to ${downloadedUser.userName}")
                            } else {
                                Log.i("Participants", "Participant: ${sqlUser.userName} exists in DB. Name unchanged.")
                            }
                            break
                        }
                    }
                    if (!userExists) {
                        // User has been added to the group since last update
                        sqlDbHelper.setGroupParticipants(downloadedUser, sqlGroupId, groupParticipantLastEdit)
                        Log.i("Participants", "Participant: ${downloadedUser
                            .userName} of key: ${downloadedUser.userKey} is new and has been added to SQL")
                    }
                    participantList?.add(downloadedUser.userName)
                }
                if (participantList != null) {
                    WelcomeJoinActivity.populateRadioButtons(
                        context!!,
                        participantList,
                        radioGroup!!
                    )
                }
            }
        })
    }
}