#!/bin/bash

# sm --start ASSETS_FRONTEND -r 3.12.0

sm2 -start EMAIL HMRC_EMAIL_RENDERER

sm2 -start DATASTREAM AUTH AUTH_LOGIN_API AUTH_LOGIN_STUB TIME_BASED_ONE_TIME_PASSWORD STRIDE_AUTH_FRONTEND STRIDE_AUTH STRIDE_IDP_STUB USER_DETAILS THIRD_PARTY_APPLICATION API_PLATFORM_MICROSERVICE API_PLATFORM_EVENTS

# sm2 -start API_PUBLISHER API_GATEWAY_STUB THIRD_PARTY_DEVELOPER API_DEFINITION API_SCOPE API_SUBSCRIPTION_FIELDS THIRD_PARTY_DELEGATED_AUTHORITY

./run_local.sh
