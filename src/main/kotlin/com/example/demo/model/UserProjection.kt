package com.example.demo.model

interface UserProjection {
    var id: String
    var givenName: String?
    var familyName: String?
    var belongsTo: Organization
    var supervisedBy: UserJustIDProjection?
}