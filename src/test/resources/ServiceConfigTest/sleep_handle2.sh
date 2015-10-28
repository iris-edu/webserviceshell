

# testing timeouts, interupts, some data handling

# do logging without disrupting stdout, stderr
logfile=/tmp/sleep_handle2_log.txt
 
args="$@"
echo "sleep handle2 stdout args: ${args}"
echo "sleep handle2 stdout env: " `env`
echo "sleep handle2 args: ${args}" >> ${logfile}
echo "sleep handle2 env: " `env` >> ${logfile}
 
ignore_sighup () { echo "sleep handle2 ignoring SIGHUP, date: " `date` >> ${logfile};}
ignore_sigint () { echo "sleep handle2 received SIGINT, date: " `date` >> ${logfile}; exit $1; }
ignore_sigterm () { echo "sleep handle2 ignoring SIGTERM, date: " `date` >> ${logfile}; }
ignore_sigquit () { echo "sleep handle2 ignoring SIGQUIT, date: " `date` >> ${logfile}; }
trap "ignore_sighup" SIGHUP
trap "ignore_sigint" SIGINT
trap "ignore_sigterm" SIGTERM
trap "ignore_sigquit" SIGQUIT

main () {
  echo "sleep handle2 starting, date: " `date` >> ${logfile}

  # write some data to stdout to get WSS into a later processing phase
  ##teststr="somedata 1 for stdout `date`"
  ##echo "sleep handle2 writing this data to stdout, teststr: ${teststr}" >> ${logfile}
  ##echo "${teststr}"

  # use to send specific data 
  sendThisData="false"
  if [[ ${sendThisData} == "true" ]]; then
    cmd="ls -l ./"
    teststr="ls results:  $(${cmd})"
    echo "sleep handle2 writing this data to stdout, teststr: ${teststr}" >> ${logfile}
    //echo "${teststr}"

    # this chunk test exit code after closing stdout
    echo "sleep handle2 closing stdout, teststr: ${teststr}" >> ${logfile}
    # this is bash for close stdout
    exec 1>&-
    exit 33
    ##sleep 45
  fi

  testTimeOut="false"
  if [[ ${testTimeOut} == "true" ]]; then
    # set handle2 time to sleep, WSS default is 300 seconds
    ##sleep_time=320
    sleep_time=130
  else
    sleep_time=1
  fi

  echo "sleep handle2 starting sleep loop, sleep_time: ${sleep_time}  date: " `date` >> ${logfile}
  while :
  do
    if [[ ${args} == *"--STDIN"* ]]; then
      # for POST, read stdin
      read stdin_content
      if [ ${#stdin_content} -ne 0 ]; then 
        echo "sleep handle2 stdin_content: ${stdin_content}" >> ${logfile}
      fi
    fi

    sleep_time=$(expr ${sleep_time} - 1)
    if [ ${sleep_time} -le 0 ]; then break; fi
    if [ $(( ${sleep_time} % 20 )) -eq 0 ]; then
      echo "sleep handle2 time remaining: ${sleep_time}  date: " `date` >> ${logfile};
    fi

    sleep 1
  done

  echo "sleep handle2 ending, date: " `date` >> ${logfile}
}

main

