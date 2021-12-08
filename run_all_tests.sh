#!/usr/bin/env bash
sbt clean coverage compile test it:test coverageReport
# python dependencyReport.py api-gatekeeper-frontend
