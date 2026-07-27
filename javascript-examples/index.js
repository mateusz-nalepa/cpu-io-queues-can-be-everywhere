import {bigProcessing} from "./processingDefault/processingDefault.js";
import {internalBigProcessingChunked, limitedBigProcessingChunked} from "./processingSEDA/processingSEDA.js";
import {setTimeout as sleep} from 'node:timers/promises';
import {logMessage} from "./common/logMessage.js";

function eventLoopTask() {
    return sleep(1500).then(() => {
        logMessage("### EventLoopTask")
    });
}

// this will block event loop for a long time
// tasks are being executed synchronously
async function step1_execution_DEFAULT() {
    console.log();
    console.log("############### Default");

    await Promise.all([
        bigProcessing(1),
        bigProcessing(2),
        eventLoopTask()
    ]);
}


// this won't block event loop
// tasks are being executed asynchronously
// but steps for different tasks are executed at the same time :((
async function step2_execution_SMALL_EVENT_LOOP() {
    console.log();
    console.log("############### Small Event Loop");

    await Promise.all([
        internalBigProcessingChunked(1),
        internalBigProcessingChunked(2),
        eventLoopTask()
    ])
}

// this won't block event loop
// tasks are being executed asynchronously
// but thanks to mutex, from logical point of view,
// task are being executed one after another
async function step3_execution_SMALL_EVENT_LOOP_WITH_MUTEX() {
    console.log();
    console.log("############### SEDA");

    await Promise.all([
        limitedBigProcessingChunked(1),
        limitedBigProcessingChunked(2),
        eventLoopTask()
    ]);
}


await step1_execution_DEFAULT();
// ############### Default
// 21:18:28.900   ### Start big function for task number: 1
// 21:18:28.901   ### Task number: 1 Processing step: step 1
// 21:18:29.901   ### Task number: 1 Processing step: step 2
// 21:18:30.901   ### Task number: 1 Processing step: step 3
// 21:18:31.901   ### Task number: 1 Processing step: step 4
// 21:18:32.901   ### Task number: 1 Processing step: step 5
// 21:18:33.901   ### Ended big function for task number: 1 after: 5000 ms <- this is good!
// 21:18:33.901   ### Start big function for task number: 2
// 21:18:33.901   ### Task number: 2 Processing step: step 1
// 21:18:34.901   ### Task number: 2 Processing step: step 2
// 21:18:35.901   ### Task number: 2 Processing step: step 3
// 21:18:36.901   ### Task number: 2 Processing step: step 4
// 21:18:37.901   ### Task number: 2 Processing step: step 5
// 21:18:38.901   ### Ended big function for task number: 2 after: 5000 ms <- this is good!
// 21:18:40.402   ### EventLoopTask                             <- this is bad! :<


await step2_execution_SMALL_EVENT_LOOP();
// ############### Small Event Loop
// 21:18:40.403   ### Start big function for task number: 1
// 21:18:40.403   ### Task number: 1 Processing step: step 1
// 21:18:40.403   ### Start big function for task number: 2
// 21:18:40.403   ### Task number: 2 Processing step: step 1
// 21:18:42.403   ### Task number: 1 Processing step: step 2
// 21:18:43.403   ### EventLoopTask                            <- this is good!
// 21:18:43.404   ### Task number: 2 Processing step: step 2
// 21:18:44.404   ### Task number: 1 Processing step: step 3
// 21:18:45.404   ### Task number: 2 Processing step: step 3
// 21:18:46.404   ### Task number: 1 Processing step: step 4
// 21:18:47.404   ### Task number: 2 Processing step: step 4
// 21:18:48.404   ### Task number: 1 Processing step: step 5
// 21:18:49.404   ### Task number: 2 Processing step: step 5
// 21:18:50.404   ### Ended big function for task number: 1 after: 10001 ms <- this is bad! :<
// 21:18:50.405   ### Ended big function for task number: 2 after: 10002 ms <- this is bad! :<

await step3_execution_SMALL_EVENT_LOOP_WITH_MUTEX();
// ############### SEDA
// 21:18:50.405   ### Task number: 1 waited 0 ms in the queue.
// 21:18:50.405   ### Start big function for task number: 1
// 21:18:50.405   ### Task number: 1 Processing step: step 1
// 21:18:51.406   ### Task number: 1 Processing step: step 2
// 21:18:52.406   ### EventLoopTask                                    <- this is good!
// 21:18:52.407   ### Task number: 1 Processing step: step 3
// 21:18:53.408   ### Task number: 1 Processing step: step 4
// 21:18:54.409   ### Task number: 1 Processing step: step 5
// 21:18:55.410   ### Ended big function for task number: 1 after: 5005 ms <- this is good!
// 21:18:55.410   ### Task number: 2 waited 5005 ms in the queue.          <- this is good!
// 21:18:55.410   ### Start big function for task number: 2
// 21:18:55.410   ### Task number: 2 Processing step: step 1
// 21:18:56.411   ### Task number: 2 Processing step: step 2
// 21:18:57.412   ### Task number: 2 Processing step: step 3
// 21:18:58.413   ### Task number: 2 Processing step: step 4
// 21:18:59.414   ### Task number: 2 Processing step: step 5
// 21:19:0.415   ### Ended big function for task number: 2 after: 5005 ms  <- this is good!