#!/bin/bash
export GC_LOG_PATH="/usr/local/logs/gc"
export JAVA_OPTS="-Dspring.profiles.active=${PROFILE} \
                  -Dspring.datasource.platform=${PROFILE} \
                  -XX:+UseG1GC \
                  -XX:G1HeapRegionSize=8m \
                  -XX:+ParallelRefProcEnabled \
                  -XX:-ResizePLAB \
                  -XX:+UseThreadPriorities \
                  -XX:ThreadPriorityPolicy=0 \
                  -XX:-UsePerfData \
                  -XX:+AlwaysPreTouch \
                  -XX:ParallelGCThreads=4 \
                  -XX:MaxGCPauseMillis=500 \
                  -XX:MetaspaceSize=128m"

[[ ${PROFILE} == "live" ]] && export JAVA_OPTS="${JAVA_OPTS} \
                -Dpinpoint.agentId=${DOCKER_ID} \
                -Dpinpoint.applicationName=${JOB_NAME}"

[[ ${PROFILE} == "staging" ]] && export JAVA_OPTS="${JAVA_OPTS} \
                -Dpinpoint.agentId=stage-${DOCKER_ID} \
                -Dpinpoint.applicationName=${JOB_NAME}"

exec java \
  -DDOCKER_ID=${DOCKER_ID} \
  ${JAVA_OPTS} \
  ${JVM_OPT} \
  -jar /usr/local/tomcat/webapps/ROOT.jar