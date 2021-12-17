
# API Gatekeeper Approvals Frontend

This service provides a frontend for HMRC's internal users to approve production credentials for applications.

## Requirements 

This service is written in [Scala](http://www.scala-lang.org/) and [Play](http://playframework.com/), so needs at least a [JRE](https://www.java.com/en/download/manual.jsp) to run.

## Run the application

To run the application use the 'run_local_with_dependencies.sh' script to start the service along with all of
the back end dependencies that it needs (which are started using [Service Manager](https://github.com/hmrc/service-manager)). You will need to have added
a suitable user in the Auth database in your local MongoDB. 

Once everything is up and running you can access the application at:

```
http://localhost:9683/api-gatekeeper-approvals
```


## Unit tests
```
sbt test
```

## Run all tests
```
To clean compile and run all tests, use the script 'run_all_tests.sh'
```

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").