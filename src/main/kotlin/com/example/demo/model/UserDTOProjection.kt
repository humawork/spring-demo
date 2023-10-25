package com.example.demo.model

data class UserDTOProjection(
    var id: String,
    var givenName: String?,
    var familyName: String?,
    var belongsTo: Organization,
    var supervisedBy: UserJustIDDTOProjection?,
)