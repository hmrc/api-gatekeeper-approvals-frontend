#!/usr/bin/env bash
sbt clean compile coverage test it:test acceptance:test sandbox:test coverageReport
# python dependencyReport.py api-gatekeeper-frontend
