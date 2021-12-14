#!/bin/bash

# sm --start ASSETS_FRONTEND -r 3.12.0

# sm --start DATASTREAM -r 6.64.0

sm --start DATASTREAM AUTH AUTH_LOGIN_API AUTH_LOGIN_STUB TIME_BASED_ONE_TIME_PASSWORD STRIDE_AUTH_FRONTEND STRIDE_AUTH STRIDE_IDP_STUB USER_DETAILS 

# sm --start API_PUBLISHER API_GATEWAY_STUB THIRD_PARTY_APPLICATION THIRD_PARTY_DEVELOPER API_DEFINITION API_SCOPE API_SUBSCRIPTION_FIELDS API_PLATFORM_EVENTS API_PLATFORM_MICROSERVICE THIRD_PARTY_DELEGATED_AUTHORITY

./run_local.sh