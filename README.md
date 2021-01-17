
# Job status manager

## How to run 

How to run directly using sbt:
```shel
sbt run
```
*** Application was run and compiled using: openjdk 11.0.8 2020-07-14

How to build docker image:
```shell
sbt docker:publishLocal
```

And then:
```shell script
docker run -p 8080:8080 --env NUMBER_OF_NODES=2 --env MAX_NUMBERF_RETAINED_FINISHED_JOBS=5 dotdata/jobs:
```



This command will build docker image with name: dotdata/jobs

## Customization

Currently there are two environment variables 

```NUMBER_OF_NODES``` with default value: 10

```MAX_NUMBER_OF_RETAINED_FINISHED_JOBS```: 120



## How actors works 

1) So every time we submit job JobFactory create JobActor and send it to JobQueue

2) Workers asks JobQueue for a job:
    - if there is a job be done worker receives it
    - otherwise worker is being added to waitingWorkers
    
3) Job is capable to handle finish message and then:
   - Ask worker to release (and to WaitingWorkers)
   - Send message to FinishedJobQueue that job has been finished

4) Job is capable to destroy when FinishedJobQueue wants ask

## TBD

- [ ] - check if job is running when finish
- [ ] - write more tests retention
- [ ] - write more test for running count
- [ ] - improve job name encoding
- [ ] - error messages